package com.alin.lin.service.impl;

import com.alin.lin.config.PosChangeProperties;
import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.AddressChangeDto;
import com.alin.lin.dto.AddressChangeRequest;
import com.alin.lin.dto.CreateChangeCaseDto;
import com.alin.lin.dto.CreateChangeCaseRequest;
import com.alin.lin.dto.MainAmountChangeDto;
import com.alin.lin.dto.MainAmountChangeRequest;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.dto.PolicyDetailDto;
import com.alin.lin.dto.PostalCodeAreaDto;
import com.alin.lin.dto.RideAmountChangeRequest;
import com.alin.lin.dto.RiderAmountChangeListRequest;
import com.alin.lin.dto.UpdateChangeCaseStatusDto;
import com.alin.lin.dto.UpdateChangeCaseStatusRequest;
import com.alin.lin.entity.CodeDescription;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.entity.PolicyChangeAcceptance;
import com.alin.lin.entity.PolicyChangeItem;
import com.alin.lin.enums.PolicyChangeFieldName;
import com.alin.lin.enums.PolicyRideKey;
import com.alin.lin.enums.PostalCodeRule;
import com.alin.lin.enums.RideChangeField;
import com.alin.lin.service.ChangeCaseApplyService;
import com.alin.lin.service.CodeDescriptionService;
import com.alin.lin.service.PolicyChangeService;
import com.alin.lin.util.PolicyChangeFieldUtil.FieldChange;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.alin.lin.util.PolicyChangeFieldUtil.addAmountChangeIfDifferent;
import static com.alin.lin.util.PolicyChangeFieldUtil.amountEquals;
import static com.alin.lin.util.PolicyChangeFieldUtil.amountToString;
import static com.alin.lin.util.PolicyChangeFieldUtil.collectAddressFieldChanges;
import static com.alin.lin.util.PolicyChangeFieldUtil.normalizeBlank;
import static com.alin.lin.util.PolicyChangeFieldUtil.requireNotEmpty;
import static com.alin.lin.util.PolicyChangeFieldUtil.requireNotNull;
import static com.alin.lin.util.PolicyChangeFieldUtil.requireText;
import static com.alin.lin.util.PolicyChangeFieldUtil.validateAddressPostalCodeFormat;

@Service
public class PolicyChangeServiceImpl implements PolicyChangeService {
    private static final int SERIAL_MAX = 999;
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MMdd");

    private final PolicyChangeDao policyChangeDao;
    private final ObjectMapper objectMapper;
    private final CodeDescriptionService codeDescriptionService;
    private final ChangeCaseApplyService changeCaseApplyService;
    private final ZoneId changeCaseZoneId;
    private String currentCaseNoPrefix;
    private int currentCaseNoSerial;

    public PolicyChangeServiceImpl(
            PolicyChangeDao policyChangeDao,
            ObjectMapper objectMapper,
            PosChangeProperties posChangeProperties,
            CodeDescriptionService codeDescriptionService,
            ChangeCaseApplyService changeCaseApplyService
    ) {
        this.policyChangeDao = policyChangeDao;
        this.objectMapper = objectMapper;
        this.codeDescriptionService = codeDescriptionService;
        this.changeCaseApplyService = changeCaseApplyService;
        this.changeCaseZoneId = ZoneId.of(posChangeProperties.getZoneId());
    }

    @Override
    public PolicyDetailDto findPolicyDetail(String policyNo, Integer policySeq) {
        MainPolicyMaster master = requirePolicy(policyNo, policySeq);
        List<MainPolicyAddress> addressList = policyChangeDao.findAddresses(policyNo, policySeq);
        List<MainPolicyRide> rideList = policyChangeDao.findRides(policyNo, policySeq);
        String communicationAddressCode = codeDescriptionService.communicationAddressCode();
        MainPolicyAddress communicationAddress = addressList.stream()
                .filter(address -> communicationAddressCode.equals(address.getAddressType()))
                .findFirst()
                .orElse(null);
        return PolicyDetailDto.builder()
                .master(master)
                .communicationAddress(communicationAddress)
                .addressList(addressList)
                .rideList(rideList)
                .addressTypes(codeDescriptionService.findAddressTypes())
                .acceptanceStatuses(codeDescriptionService.findAcceptanceStatuses())
                .changeItems(codeDescriptionService.findChangeItems())
                .build();
    }

    @Override
    public PostalCodeAreaDto findPostalCodeArea(String postalCode) {
        requireText(postalCode, "postalCode");
        String normalizedPostalCode = postalCode.trim();
        if (!PostalCodeRule.ZIP_CODE_3_OR_6.matches(normalizedPostalCode)) {
            throw new IllegalArgumentException("郵遞區號前三碼必填，後三碼可空白；若填寫需為 3 碼");
        }

        String zipCode3 = normalizedPostalCode.substring(0, 3);
        CodeDescription code = codeDescriptionService.findPostalCodeZipCode3(zipCode3);
        if (code == null) {
            throw new NoSuchElementException("找不到郵遞區號前三碼: " + zipCode3);
        }

        String[] cityAndDistrict = parseCityAndDistrict(code);
        return PostalCodeAreaDto.builder()
                .postalCode(normalizedPostalCode)
                .zipCode3(zipCode3)
                .city(cityAndDistrict[0])
                .district(cityAndDistrict[1])
                .addressPrefix(String.join("", cityAndDistrict))
                .halfWidthAddressPrefix(code.getCodeDescription())
                .build();
    }

    @Override
    public CreateChangeCaseDto createChangeCase(CreateChangeCaseRequest request) {
        requirePolicy(request.getPolicyNo(), request.getPolicySeq());
        requireText(request.getChangeItem(), "changeItem");

        String changeCaseNo = generateChangeCaseNo();

        return CreateChangeCaseDto.builder()
                .policyNo(request.getPolicyNo())
                .policySeq(request.getPolicySeq())
                .changeCaseNo(changeCaseNo)
                .acceptanceStatus(codeDescriptionService.pendingStatusCode())
                .changeItem(request.getChangeItem())
                .build();
    }

    @Override
    @Transactional
    public AddressChangeDto saveAddressChange(String changeCaseNo, AddressChangeRequest request) {
        String addressType = request.getAddressType() == null || request.getAddressType().isBlank()
                ? codeDescriptionService.communicationAddressCode()
                : request.getAddressType();

        requirePolicy(request.getPolicyNo(), request.getPolicySeq());
        requireText(changeCaseNo, "changeCaseNo");

        MainPolicyAddress beforeAddress = policyChangeDao.findAddress(request.getPolicyNo(), request.getPolicySeq(), addressType);
        if (beforeAddress == null) {
            throw new NoSuchElementException("找不到地址資料: " + request.getPolicyNo() + "-" + request.getPolicySeq() + "-" + addressType);
        }

        String zipCode3 = normalizeBlank(request.getZipCode3());
        String zipCode2 = normalizeBlank(request.getZipCode2());
        validateAddressRequest(addressType, zipCode3, zipCode2, request.getFullWidthAddress(), request.getHalfWidthAddress());

        boolean physicalAddressType = isPhysicalAddressType(addressType);
        String contactValue = normalizeContactValue(beforeAddress, request.getHalfWidthAddress());
        MainPolicyAddress afterAddress = MainPolicyAddress.builder()
                .policyNo(request.getPolicyNo())
                .policySeq(request.getPolicySeq())
                .addressType(addressType)
                .zipCode3(physicalAddressType ? zipCode3 : beforeAddress.getZipCode3())
                .zipCode2(physicalAddressType ? zipCode2 : beforeAddress.getZipCode2())
                .fullWidthAddress(physicalAddressType ? normalizeBlank(request.getFullWidthAddress()) : beforeAddress.getFullWidthAddress())
                .halfWidthAddress(physicalAddressType ? beforeAddress.getHalfWidthAddress() : contactValue)
                .build();

        String addressChangeItem = codeDescriptionService.addressChangeItemCode();
        List<FieldChange> fieldChanges = collectAddressFieldChanges(beforeAddress, afterAddress);
        if (fieldChanges.isEmpty()) {
            return AddressChangeDto.builder()
                    .changeCaseNo(changeCaseNo)
                    .changeItem(addressChangeItem)
                    .changedFieldCount(0)
                    .build();
        }

        ensureChangeCaseSaved(request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, addressChangeItem);
        fieldChanges.forEach(fieldChange -> insertFieldChange(request, changeCaseNo, addressChangeItem, fieldChange));

        policyChangeDao.insertChangeFile(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                addressChangeItem,
                "main_policy_address",
                toJson(addressSnapshot(beforeAddress)),
                toJson(addressSnapshot(afterAddress))
        );

        return AddressChangeDto.builder()
                .changeCaseNo(changeCaseNo)
                .changeItem(addressChangeItem)
                .changedFieldCount(fieldChanges.size())
                .build();
    }

    @Override
    @Transactional
    public MainAmountChangeDto saveMainAmountChange(String changeCaseNo, MainAmountChangeRequest request) {
        MainPolicyMaster master = requirePolicy(request.getPolicyNo(), request.getPolicySeq());
        requireText(changeCaseNo, "changeCaseNo");
        requireNotNull(request.getMasterInsuredAmount(), "masterInsuredAmount");

        String mainAmountChangeItem = codeDescriptionService.mainAmountChangeItemCode();
        if (amountEquals(master.getInsuredAmount(), request.getMasterInsuredAmount())) {
            return MainAmountChangeDto.builder()
                    .changeCaseNo(changeCaseNo)
                    .changeItem(mainAmountChangeItem)
                    .changedFieldCount(0)
                    .build();
        }

        MainPolicyRide mainRide = findMainRide(request.getPolicyNo(), request.getPolicySeq());
        List<FieldChange> fieldChanges = new ArrayList<>();
        fieldChanges.add(new FieldChange(
                PolicyChangeFieldName.MASTER_INSURED_AMOUNT.getFieldName(),
                PolicyChangeFieldName.MASTER_CHANGE_KEY.getFieldName(),
                amountToString(master.getInsuredAmount()),
                amountToString(request.getMasterInsuredAmount())
        ));
        fieldChanges.add(new FieldChange(
                RideChangeField.INSURED_AMOUNT.fieldName(PolicyRideKey.MAIN.getRideOrder()),
                PolicyRideKey.MAIN.getRideOrder(),
                amountToString(mainRide.getInsuredAmount()),
                amountToString(request.getMasterInsuredAmount())
        ));

        ensureChangeCaseSaved(request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, mainAmountChangeItem);
        fieldChanges.forEach(fieldChange -> insertFieldChange(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                mainAmountChangeItem,
                fieldChange
        ));

        return MainAmountChangeDto.builder()
                .changeCaseNo(changeCaseNo)
                .changeItem(mainAmountChangeItem)
                .changedFieldCount(fieldChanges.size())
                .build();
    }

    @Override
    @Transactional
    public MainAmountChangeDto saveRiderAmountChange(String changeCaseNo, String policyNo, Integer policySeq, RiderAmountChangeListRequest request) {
        requirePolicy(policyNo, policySeq);
        requireText(changeCaseNo, "changeCaseNo");
        requireNotEmpty(request.getRides(), "rides");

        List<MainPolicyRide> rides = policyChangeDao.findRides(policyNo, policySeq);
        Map<String, MainPolicyRide> rideMap = new LinkedHashMap<>();
        rides.forEach(ride -> rideMap.put(ride.getRideOrder(), ride));

        List<FieldChange> fieldChanges = new ArrayList<>();
        for (RideAmountChangeRequest changedRide : request.getRides()) {
            requireText(changedRide.getRideOrder(), "rideOrder");
            requireNotNull(changedRide.getInsuredAmount(), "ride insuredAmount");
            MainPolicyRide beforeRide = rideMap.get(changedRide.getRideOrder());
            if (beforeRide == null) {
                throw new NoSuchElementException("找不到附約: " + changedRide.getRideOrder());
            }
            if (codeDescriptionService.mainRideTypeCode().equals(beforeRide.getRideType())) {
                throw new IllegalArgumentException("003 附約保額變更不可修改主約");
            }
            addAmountChangeIfDifferent(
                    fieldChanges,
                    RideChangeField.INSURED_AMOUNT.fieldName(changedRide.getRideOrder()),
                    changedRide.getRideOrder(),
                    beforeRide.getInsuredAmount(),
                    changedRide.getInsuredAmount()
            );
        }

        String riderAmountChangeItem = codeDescriptionService.riderAmountChangeItemCode();
        if (fieldChanges.isEmpty()) {
            return MainAmountChangeDto.builder()
                    .changeCaseNo(changeCaseNo)
                    .changeItem(riderAmountChangeItem)
                    .changedFieldCount(0)
                    .build();
        }

        ensureChangeCaseSaved(policyNo, policySeq, changeCaseNo, riderAmountChangeItem);
        fieldChanges.forEach(fieldChange -> insertFieldChange(
                policyNo,
                policySeq,
                changeCaseNo,
                riderAmountChangeItem,
                fieldChange
        ));

        return MainAmountChangeDto.builder()
                .changeCaseNo(changeCaseNo)
                .changeItem(riderAmountChangeItem)
                .changedFieldCount(fieldChanges.size())
                .build();
    }

    @Override
    public List<PolicyChangeCaseDto> findChangeCases(String policyNo) {
        requireText(policyNo, "policyNo");
        return policyChangeDao.findChangeCases(policyNo);
    }

    @Override
    @Transactional
    public UpdateChangeCaseStatusDto updateChangeCaseStatus(String changeCaseNo, UpdateChangeCaseStatusRequest request) {
        requireText(changeCaseNo, "changeCaseNo");
        requireText(request.getPolicyNo(), "policyNo");
        requireNotNull(request.getPolicySeq(), "policySeq");

        String acceptanceStatus = normalizeStatus(request.getAcceptanceStatus());
        if (!codeDescriptionService.cancelStatusCode().equals(acceptanceStatus)
                && !codeDescriptionService.completeStatusCode().equals(acceptanceStatus)) {
            throw new IllegalArgumentException("受理狀態只能改為 C 或 S");
        }

        PolicyChangeCaseDto changeCase = policyChangeDao.findChangeCase(request.getPolicyNo(), request.getPolicySeq(), changeCaseNo);
        if (changeCase == null) {
            throw new NoSuchElementException("找不到保全受理資料: " + changeCaseNo);
        }
        if (!codeDescriptionService.pendingStatusCode().equals(normalizeStatus(changeCase.getAcceptanceStatus()))) {
            throw new IllegalStateException("只有 P-受理中可以改為 C 或 S");
        }

        int appliedItemCount = 0;
        if (codeDescriptionService.completeStatusCode().equals(acceptanceStatus)) {
            appliedItemCount = changeCaseApplyService.applyChangeCase(request.getPolicyNo(), request.getPolicySeq(), changeCaseNo);
        }

        policyChangeDao.updateAcceptanceStatus(PolicyChangeAcceptance.builder()
                .policyNo(request.getPolicyNo())
                .policySeq(request.getPolicySeq())
                .changeCaseNo(changeCaseNo)
                .acceptanceStatus(acceptanceStatus)
                .build());
        return UpdateChangeCaseStatusDto.builder()
                .policyNo(request.getPolicyNo())
                .policySeq(request.getPolicySeq())
                .changeCaseNo(changeCaseNo)
                .acceptanceStatus(acceptanceStatus)
                .appliedItemCount(appliedItemCount)
                .build();
    }

    private String normalizeStatus(String acceptanceStatus) {
        requireText(acceptanceStatus, "acceptanceStatus");
        return acceptanceStatus.trim().toUpperCase();
    }

    private void ensureChangeCaseSaved(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        if (policyChangeDao.existsChangeItem(policyNo, policySeq, changeCaseNo, changeItem)) {
            return;
        }
        if (policyChangeDao.existsChangeCaseNo(changeCaseNo)) {
            throw new IllegalArgumentException("變更案號已存在: " + changeCaseNo);
        }
        policyChangeDao.insertAcceptance(PolicyChangeAcceptance.builder()
                .policyNo(policyNo)
                .policySeq(policySeq)
                .changeCaseNo(changeCaseNo)
                .acceptanceStatus(codeDescriptionService.pendingStatusCode())
                .build());
        policyChangeDao.insertChangeItem(PolicyChangeItem.builder()
                .policyNo(policyNo)
                .policySeq(policySeq)
                .changeCaseNo(changeCaseNo)
                .changeItem(changeItem)
                .build());
    }

    private MainPolicyMaster requirePolicy(String policyNo, Integer policySeq) {
        requireText(policyNo, "policyNo");
        requireNotNull(policySeq, "policySeq");
        MainPolicyMaster master = policyChangeDao.findMaster(policyNo, policySeq);
        if (master == null) {
            throw new NoSuchElementException("找不到保單: " + policyNo + "-" + policySeq);
        }
        return master;
    }

    private MainPolicyRide findMainRide(String policyNo, Integer policySeq) {
        return policyChangeDao.findRides(policyNo, policySeq).stream()
                .filter(ride -> codeDescriptionService.mainRideTypeCode().equals(ride.getRideType())
                        || PolicyRideKey.MAIN.getRideOrder().equals(ride.getRideOrder()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("找不到主約資料: " + policyNo + "-" + policySeq));
    }

    private String[] parseCityAndDistrict(CodeDescription code) {
        String source = code.getCodeAfter() == null || code.getCodeAfter().isBlank()
                ? code.getCodeDescription()
                : code.getCodeAfter();
        String[] parts = source.split("\\|", 2);
        if (parts.length == 2) {
            return parts;
        }
        String description = code.getCodeDescription();
        if (description.length() < 4) {
            return new String[]{description, ""};
        }
        return new String[]{description.substring(0, 3), description.substring(3)};
    }

    private void validateAddressRequest(String addressType, String zipCode3, String zipCode2, String fullWidthAddress, String halfWidthAddress) {
        if (!isPhysicalAddressType(addressType)) {
            requireText(halfWidthAddress, "halfWidthAddress");
            return;
        }
        requireText(fullWidthAddress, "fullWidthAddress");
        validateAddressPostalCodeFormat(zipCode3, zipCode2);
        if (codeDescriptionService.findPostalCodeZipCode3(zipCode3) == null) {
            throw new NoSuchElementException("找不到郵遞區號前三碼: " + zipCode3);
        }
    }

    private boolean isPhysicalAddressType(String addressType) {
        return codeDescriptionService.communicationAddressCode().equals(addressType)
                || codeDescriptionService.registeredAddressCode().equals(addressType);
    }

    private String normalizeContactValue(MainPolicyAddress beforeAddress, String requestContactValue) {
        String normalizedRequestValue = normalizeBlank(requestContactValue);
        String currentDisplayValue = normalizeBlank(beforeAddress.getFullWidthAddress());
        if (currentDisplayValue == null) {
            currentDisplayValue = normalizeBlank(beforeAddress.getHalfWidthAddress());
        }
        if (java.util.Objects.equals(currentDisplayValue, normalizedRequestValue)) {
            return beforeAddress.getHalfWidthAddress();
        }
        return normalizedRequestValue;
    }

    private void insertFieldChange(AddressChangeRequest request, String changeCaseNo, String changeItem, FieldChange fieldChange) {
        insertFieldChange(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                changeItem,
                fieldChange
        );
    }

    private void insertFieldChange(String policyNo, Integer policySeq, String changeCaseNo, String changeItem, FieldChange fieldChange) {
        policyChangeDao.insertChangeField(
                policyNo,
                policySeq,
                changeCaseNo,
                changeItem,
                fieldChange.field(),
                fieldChange.key(),
                fieldChange.beforeValue(),
                fieldChange.afterValue()
        );
    }

    private Map<String, Object> addressSnapshot(MainPolicyAddress address) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("policyNo", address.getPolicyNo());
        snapshot.put("policySeq", address.getPolicySeq());
        snapshot.put("addressType", address.getAddressType());
        snapshot.put("zipCode3", address.getZipCode3());
        snapshot.put("zipCode2", address.getZipCode2());
        snapshot.put("fullWidthAddress", address.getFullWidthAddress());
        snapshot.put("halfWidthAddress", address.getHalfWidthAddress());
        return snapshot;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 轉換失敗", e);
        }
    }

    private synchronized String generateChangeCaseNo() {
        String prefix = buildTodayCaseNoPrefix();
        if (!prefix.equals(currentCaseNoPrefix)) {
            currentCaseNoPrefix = prefix;
            currentCaseNoSerial = findLatestSerial(prefix);
        }

        if (currentCaseNoSerial >= SERIAL_MAX) {
            throw new IllegalStateException("今日變更案號流水號已達上限");
        }

        currentCaseNoSerial++;
        return prefix + String.format("%03d", currentCaseNoSerial);
    }

    private String buildTodayCaseNoPrefix() {
        LocalDate today = LocalDate.now(changeCaseZoneId);
        int rocYear = today.getYear() - 1911;
        return "C" + String.format("%03d", rocYear) + today.format(MONTH_DAY_FORMATTER);
    }

    private int findLatestSerial(String prefix) {
        String maxChangeCaseNo = policyChangeDao.findMaxChangeCaseNoByPrefix(prefix);
        if (maxChangeCaseNo == null || maxChangeCaseNo.length() <= prefix.length()) {
            return 0;
        }
        return Integer.parseInt(maxChangeCaseNo.substring(prefix.length()));
    }

}
