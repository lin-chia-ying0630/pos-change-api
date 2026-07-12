package com.alin.lin.service;

import com.alin.lin.dto.AddressChangeDto;
import com.alin.lin.dto.AddressChangeRequest;
import com.alin.lin.dto.CreateChangeCaseDto;
import com.alin.lin.dto.CreateChangeCaseRequest;
import com.alin.lin.dto.PolicyChangeCaseDetailDto;
import com.alin.lin.dto.UpdateChangeCaseStatusRequest;
import com.alin.lin.exception.ChangeCaseConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PolicyChangeWorkflowIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("main")
            .withUsername("pos")
            .withPassword("pos-test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/local");
        registry.add("pos.security.enabled", () -> false);
    }

    @Autowired
    private PolicyChangeService policyChangeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetBusinessData() {
        jdbcTemplate.update("DELETE FROM policy_change_field");
        jdbcTemplate.update("DELETE FROM policy_change_file");
        jdbcTemplate.update("DELETE FROM policy_change_item");
        jdbcTemplate.update("DELETE FROM policy_change_acceptance");
        jdbcTemplate.update("DELETE FROM policy_change_case_sequence");
        jdbcTemplate.update("""
                UPDATE main_policy_address
                SET zip_code3 = '100',
                    zip_code2 = '001',
                    full_width_address = '臺北市中正區重慶南路一段１號',
                    half_width_address = 'No.1, Sec.1, Chongqing S. Rd., Zhongzheng Dist., Taipei City'
                WHERE policy_no = 'P000000001'
                  AND policy_seq = 1
                  AND address_type = '01'
                """);
    }

    @Test
    void concurrentCaseNumbersAreUnique() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Callable<String>> tasks = java.util.stream.IntStream.range(0, 12)
                    .mapToObj(index -> (Callable<String>) () -> createAddressCase().getChangeCaseNo())
                    .toList();
            List<Future<String>> futures = executor.invokeAll(tasks);
            Set<String> caseNumbers = new HashSet<>();
            for (Future<String> future : futures) {
                caseNumbers.add(future.get());
            }
            assertEquals(12, caseNumbers.size());
            assertTrue(caseNumbers.stream().allMatch(caseNo -> caseNo.matches("C\\d{7}\\d{3,}")));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void noChangeRemovesPendingDraft() {
        CreateChangeCaseDto changeCase = createAddressCase();
        AddressChangeDto result = saveCommunicationAddress(changeCase.getChangeCaseNo(), "臺北市中正區重慶南路一段１號");

        assertEquals(0, result.getChangedFieldCount());
        assertFalse(policyChangeService.findChangeCases("P000000001").stream()
                .anyMatch(item -> item.getChangeCaseNo().equals(changeCase.getChangeCaseNo())));
    }

    @Test
    void repeatedSaveKeepsOnlyLatestDraftValue() {
        CreateChangeCaseDto changeCase = createAddressCase();
        saveCommunicationAddress(changeCase.getChangeCaseNo(), "臺北市中正區重慶南路一段２號");
        saveCommunicationAddress(changeCase.getChangeCaseNo(), "臺北市中正區重慶南路一段３號");

        PolicyChangeCaseDetailDto detail = policyChangeService.findChangeCaseDetail(
                "P000000001", 1, changeCase.getChangeCaseNo()
        );
        assertEquals(1, detail.getChangeFields().stream()
                .filter(field -> "full_width_address".equals(field.getChangeField()))
                .count());
        assertTrue(detail.getChangeFields().stream()
                .anyMatch(field -> "臺北市中正區重慶南路一段３號".equals(field.getContentAfter())));
    }

    @Test
    void stalePendingCaseCannotOverwriteNewerCompletedCase() {
        CreateChangeCaseDto olderCase = createAddressCase();
        saveCommunicationAddress(olderCase.getChangeCaseNo(), "臺北市中正區重慶南路一段２號");
        CreateChangeCaseDto newerCase = createAddressCase();
        saveCommunicationAddress(newerCase.getChangeCaseNo(), "臺北市中正區重慶南路一段３號");

        complete(newerCase.getChangeCaseNo());
        assertThrows(ChangeCaseConflictException.class, () -> complete(olderCase.getChangeCaseNo()));
        assertEquals(
                "臺北市中正區重慶南路一段３號",
                policyChangeService.findPolicyDetail("P000000001", 1)
                        .getCommunicationAddress()
                        .getFullWidthAddress()
        );
    }

    @Test
    void concurrentReviewsOfSameAddressAllowOnlyOneCaseToComplete() throws Exception {
        CreateChangeCaseDto firstCase = createAddressCase();
        saveCommunicationAddress(firstCase.getChangeCaseNo(), "臺北市中正區重慶南路一段２號");
        CreateChangeCaseDto secondCase = createAddressCase();
        saveCommunicationAddress(secondCase.getChangeCaseNo(), "臺北市中正區重慶南路一段３號");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> firstResult = executor.submit(
                    () -> completeAfterSignal(firstCase.getChangeCaseNo(), ready, start)
            );
            Future<String> secondResult = executor.submit(
                    () -> completeAfterSignal(secondCase.getChangeCaseNo(), ready, start)
            );
            ready.await();
            start.countDown();

            assertEquals(Set.of("COMPLETED", "CONFLICT"), Set.of(firstResult.get(), secondResult.get()));
            String currentAddress = policyChangeService.findPolicyDetail("P000000001", 1)
                    .getCommunicationAddress()
                    .getFullWidthAddress();
            assertTrue(Set.of(
                    "臺北市中正區重慶南路一段２號",
                    "臺北市中正區重慶南路一段３號"
            ).contains(currentAddress));

            List<String> statuses = policyChangeService.findChangeCases("P000000001").stream()
                    .map(changeCase -> changeCase.getAcceptanceStatus())
                    .toList();
            assertEquals(1, statuses.stream().filter("S"::equals).count());
            assertEquals(1, statuses.stream().filter("P"::equals).count());
        } finally {
            executor.shutdownNow();
        }
    }

    private CreateChangeCaseDto createAddressCase() {
        return policyChangeService.createChangeCase(CreateChangeCaseRequest.builder()
                .policyNo("P000000001")
                .policySeq(1)
                .changeItem("001")
                .build());
    }

    private AddressChangeDto saveCommunicationAddress(String changeCaseNo, String address) {
        return policyChangeService.saveAddressChange(changeCaseNo, AddressChangeRequest.builder()
                .policyNo("P000000001")
                .policySeq(1)
                .addressType("01")
                .zipCode3("100")
                .zipCode2("001")
                .fullWidthAddress(address)
                .halfWidthAddress("")
                .build());
    }

    private void complete(String changeCaseNo) {
        policyChangeService.updateChangeCaseStatus(changeCaseNo, UpdateChangeCaseStatusRequest.builder()
                .policyNo("P000000001")
                .policySeq(1)
                .acceptanceStatus("S")
                .build());
    }

    private String completeAfterSignal(
            String changeCaseNo,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            complete(changeCaseNo);
            return "COMPLETED";
        } catch (ChangeCaseConflictException exception) {
            return "CONFLICT";
        }
    }
}
