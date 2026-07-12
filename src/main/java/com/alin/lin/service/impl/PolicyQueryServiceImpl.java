package com.alin.lin.service.impl;

import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.dto.PolicyChangeCaseDetailDto;
import com.alin.lin.dto.PolicyDetailDto;
import com.alin.lin.dto.PostalCodeAreaDto;
import com.alin.lin.entity.CodeDescription;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.enums.PostalCodeRule;
import com.alin.lin.service.CodeDescriptionService;
import com.alin.lin.service.PolicyChangeSupportService;
import com.alin.lin.service.PolicyQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

import static com.alin.lin.util.PolicyChangeFieldUtil.requireText;

@Service
public class PolicyQueryServiceImpl implements PolicyQueryService {
    private final PolicyChangeDao policyChangeDao;
    private final PolicyChangeSupportService policyChangeSupportService;
    private final CodeDescriptionService codeDescriptionService;

    public PolicyQueryServiceImpl(
            PolicyChangeDao policyChangeDao,
            PolicyChangeSupportService policyChangeSupportService,
            CodeDescriptionService codeDescriptionService
    ) {
        this.policyChangeDao = policyChangeDao;
        this.policyChangeSupportService = policyChangeSupportService;
        this.codeDescriptionService = codeDescriptionService;
    }

    @Override
    public PolicyDetailDto findPolicyDetail(String policyNo, Integer policySeq) {
        MainPolicyMaster master = policyChangeSupportService.requirePolicy(policyNo, policySeq);
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
    public List<PolicyChangeCaseDto> findChangeCases(String policyNo) {
        requireText(policyNo, "policyNo");
        return policyChangeDao.findChangeCases(policyNo);
    }

    @Override
    public PolicyChangeCaseDetailDto findChangeCaseDetail(String policyNo, Integer policySeq, String changeCaseNo) {
        policyChangeSupportService.requirePolicy(policyNo, policySeq);
        requireText(changeCaseNo, "changeCaseNo");
        PolicyChangeCaseDto changeCase = policyChangeDao.findChangeCase(policyNo, policySeq, changeCaseNo);
        if (changeCase == null) {
            throw new NoSuchElementException("找不到保全受理資料: " + changeCaseNo);
        }
        return PolicyChangeCaseDetailDto.builder()
                .changeCase(changeCase)
                .changeFields(policyChangeDao.findChangeFieldsByCaseNo(policyNo, policySeq, changeCaseNo))
                .changeFiles(policyChangeDao.findChangeFilesByCaseNo(policyNo, policySeq, changeCaseNo))
                .build();
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
}
