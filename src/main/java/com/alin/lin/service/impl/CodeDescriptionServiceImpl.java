package com.alin.lin.service.impl;

import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.entity.CodeDescription;
import com.alin.lin.enums.CodeDescriptionMeaning;
import com.alin.lin.enums.CodeTable;
import com.alin.lin.service.CodeDescriptionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class CodeDescriptionServiceImpl implements CodeDescriptionService {
    private final PolicyChangeDao policyChangeDao;

    public CodeDescriptionServiceImpl(PolicyChangeDao policyChangeDao) {
        this.policyChangeDao = policyChangeDao;
    }

    @Override
    public List<CodeDescription> findAddressTypes() {
        return findCodes(CodeTable.ADDRESS_TYPE);
    }

    @Override
    public List<CodeDescription> findAcceptanceStatuses() {
        return findCodes(CodeTable.ACCEPTANCE_STATUS);
    }

    @Override
    public List<CodeDescription> findChangeItems() {
        return findCodes(CodeTable.CHANGE_ITEM);
    }

    @Override
    public CodeDescription findPostalCodeZipCode3(String zipCode3) {
        return policyChangeDao.findCode(
                CodeTable.POSTAL_CODE_ZIP_CODE3.getCodeGroup(),
                CodeTable.POSTAL_CODE_ZIP_CODE3.getCodeField(),
                zipCode3
        );
    }

    @Override
    public Map<String, String> findChtFieldNames() {
        Map<String, String> fieldNames = new LinkedHashMap<>();
        for (CodeDescription code : policyChangeDao.findCodesByGroup("CHT-code")) {
            fieldNames.putIfAbsent(code.getCodeField(), code.getCodeBefore());
        }
        return fieldNames;
    }

    @Override
    public String communicationAddressCode() {
        return codeBefore(CodeDescriptionMeaning.COMMUNICATION_ADDRESS);
    }

    @Override
    public String registeredAddressCode() {
        return codeBefore(CodeDescriptionMeaning.REGISTERED_ADDRESS);
    }

    @Override
    public String emailAddressCode() {
        return codeBefore(CodeDescriptionMeaning.EMAIL_ADDRESS);
    }

    @Override
    public String addressChangeItemCode() {
        return changeItemCode(CodeDescriptionMeaning.ADDRESS_CHANGE);
    }

    @Override
    public String mainAmountChangeItemCode() {
        return changeItemCode(CodeDescriptionMeaning.MAIN_AMOUNT_CHANGE);
    }

    @Override
    public String riderAmountChangeItemCode() {
        return changeItemCode(CodeDescriptionMeaning.RIDER_AMOUNT_CHANGE);
    }

    @Override
    public String pendingStatusCode() {
        return acceptanceStatusCode(CodeDescriptionMeaning.PENDING_STATUS);
    }

    @Override
    public String processingStatusCode() {
        return acceptanceStatusCode(CodeDescriptionMeaning.PROCESSING_STATUS);
    }

    @Override
    public String completeStatusCode() {
        return acceptanceStatusCode(CodeDescriptionMeaning.COMPLETE_STATUS);
    }

    @Override
    public String cancelStatusCode() {
        return acceptanceStatusCode(CodeDescriptionMeaning.CANCEL_STATUS);
    }

    @Override
    public String mainRideTypeCode() {
        return codeBefore(CodeDescriptionMeaning.MAIN_RIDE_TYPE);
    }

    private List<CodeDescription> findCodes(CodeTable codeTable) {
        return policyChangeDao.findCodes(codeTable.getCodeGroup(), codeTable.getCodeField());
    }

    private String changeItemCode(CodeDescriptionMeaning meaning) {
        return codeBefore(meaning);
    }

    private String acceptanceStatusCode(CodeDescriptionMeaning meaning) {
        return codeBefore(meaning);
    }

    private String codeBefore(CodeDescriptionMeaning meaning) {
        return findCodes(meaning.getCodeTable()).stream()
                .filter(code -> meaning.getCodeBefore().equals(code.getCodeBefore()))
                .map(CodeDescription::getCodeBefore)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "找不到代碼: " + meaning.getCodeTable().getCodeGroup() + "/" + meaning.getCodeTable().getCodeField() + "/" + meaning.getCodeBefore()
                ));
    }
}
