package com.alin.lin.service.impl;

import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.AddressChangeDto;
import com.alin.lin.dto.AddressChangeRequest;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.service.AddressChangeSaveService;
import com.alin.lin.service.CodeDescriptionService;
import com.alin.lin.service.PolicyChangeSupportService;
import com.alin.lin.util.PolicyChangeFieldUtil.FieldChange;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.alin.lin.util.PolicyChangeFieldUtil.collectAddressFieldChanges;
import static com.alin.lin.util.PolicyChangeFieldUtil.normalizeBlank;
import static com.alin.lin.util.PolicyChangeFieldUtil.requireText;
import static com.alin.lin.util.PolicyChangeFieldUtil.validateAddressPostalCodeFormat;

@Service
public class AddressChangeSaveServiceImpl implements AddressChangeSaveService {
    private static final String ADDRESS_CHANGE_FILE = "main_policy_address";

    private final PolicyChangeDao policyChangeDao;
    private final PolicyChangeSupportService policyChangeSupportService;
    private final CodeDescriptionService codeDescriptionService;
    private final ObjectMapper objectMapper;

    public AddressChangeSaveServiceImpl(
            PolicyChangeDao policyChangeDao,
            PolicyChangeSupportService policyChangeSupportService,
            CodeDescriptionService codeDescriptionService,
            ObjectMapper objectMapper
    ) {
        this.policyChangeDao = policyChangeDao;
        this.policyChangeSupportService = policyChangeSupportService;
        this.codeDescriptionService = codeDescriptionService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AddressChangeDto saveAddressChange(String changeCaseNo, AddressChangeRequest request) {
        String addressType = request.getAddressType() == null || request.getAddressType().isBlank()
                ? codeDescriptionService.communicationAddressCode()
                : request.getAddressType();

        policyChangeSupportService.requirePolicy(request.getPolicyNo(), request.getPolicySeq());
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
        policyChangeSupportService.validateChangeCaseAccess(
                request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, addressChangeItem
        );
        List<FieldChange> fieldChanges = collectAddressFieldChanges(beforeAddress, afterAddress);
        policyChangeDao.deleteChangeFieldsByItemAndKey(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                addressChangeItem,
                addressType
        );
        policyChangeDao.deleteChangeFileByItemAndKey(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                addressChangeItem,
                ADDRESS_CHANGE_FILE,
                addressType
        );
        if (fieldChanges.isEmpty()) {
            policyChangeSupportService.removeEmptyChangeItemAndAcceptance(
                    request.getPolicyNo(),
                    request.getPolicySeq(),
                    changeCaseNo,
                    addressChangeItem
            );
            return AddressChangeDto.builder()
                    .changeCaseNo(changeCaseNo)
                    .changeItem(addressChangeItem)
                    .changedFieldCount(0)
                    .build();
        }

        policyChangeSupportService.ensureChangeCaseSaved(request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, addressChangeItem);
        fieldChanges.forEach(fieldChange -> policyChangeSupportService.upsertFieldChange(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                addressChangeItem,
                fieldChange
        ));

        policyChangeDao.upsertChangeFile(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                addressChangeItem,
                ADDRESS_CHANGE_FILE,
                addressType,
                toJson(addressSnapshot(beforeAddress)),
                toJson(addressSnapshot(afterAddress))
        );

        return AddressChangeDto.builder()
                .changeCaseNo(changeCaseNo)
                .changeItem(addressChangeItem)
                .changedFieldCount(fieldChanges.size())
                .build();
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
}
