package com.alin.lin.service.impl;

import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.entity.PolicyChangeField;
import com.alin.lin.entity.PolicyChangeFile;
import com.alin.lin.enums.PolicyChangeFieldName;
import com.alin.lin.enums.PolicyRideKey;
import com.alin.lin.enums.RideChangeField;
import com.alin.lin.service.ChangeCaseApplyService;
import com.alin.lin.service.CodeDescriptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.alin.lin.util.PolicyChangeFieldUtil.normalizeBlank;

@Service
public class ChangeCaseApplyServiceImpl implements ChangeCaseApplyService {
    private final PolicyChangeDao policyChangeDao;
    private final CodeDescriptionService codeDescriptionService;
    private final ObjectMapper objectMapper;

    public ChangeCaseApplyServiceImpl(
            PolicyChangeDao policyChangeDao,
            CodeDescriptionService codeDescriptionService,
            ObjectMapper objectMapper
    ) {
        this.policyChangeDao = policyChangeDao;
        this.codeDescriptionService = codeDescriptionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public int applyChangeCase(String policyNo, Integer policySeq, String changeCaseNo) {
        List<String> changeItems = policyChangeDao.findChangeItemsByCaseNo(policyNo, policySeq, changeCaseNo);
        String addressChangeItem = codeDescriptionService.addressChangeItemCode();
        String mainAmountChangeItem = codeDescriptionService.mainAmountChangeItemCode();
        String riderAmountChangeItem = codeDescriptionService.riderAmountChangeItemCode();
        int appliedItemCount = 0;
        for (String changeItem : changeItems) {
            boolean premiumTotalShouldRefresh = false;
            if (addressChangeItem.equals(changeItem)) {
                applyAddressChanges(policyNo, policySeq, changeCaseNo, changeItem);
                appliedItemCount++;
                continue;
            }
            if (mainAmountChangeItem.equals(changeItem)) {
                premiumTotalShouldRefresh = applyMainAmountChanges(policyNo, policySeq, changeCaseNo, changeItem);
                appliedItemCount++;
                if (premiumTotalShouldRefresh) {
                    policyChangeDao.updateMasterTotalPremiumFromRides(policyNo, policySeq);
                }
                continue;
            }
            if (riderAmountChangeItem.equals(changeItem)) {
                premiumTotalShouldRefresh = applyRiderAmountChanges(policyNo, policySeq, changeCaseNo, changeItem);
                appliedItemCount++;
                if (premiumTotalShouldRefresh) {
                    policyChangeDao.updateMasterTotalPremiumFromRides(policyNo, policySeq);
                }
                continue;
            }
            applyMasterChanges(policyNo, policySeq, changeCaseNo, changeItem);
            appliedItemCount++;
        }
        return appliedItemCount;
    }

    private void applyAddressChanges(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        List<PolicyChangeFile> changeFiles = policyChangeDao.findChangeFilesByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (changeFiles.isEmpty()) {
            throw new IllegalStateException("找不到地址變更檔案: " + changeCaseNo);
        }
        for (PolicyChangeFile changeFile : changeFiles) {
            MainPolicyAddress address = readAddress(changeFile.getContentAfter());
            policyChangeDao.updateAddress(address);
        }
    }

    private void applyMasterChanges(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        List<PolicyChangeField> changeFields = policyChangeDao.findChangeFieldsByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (changeFields.isEmpty()) {
            throw new IllegalStateException("找不到主檔變更欄位: " + changeCaseNo);
        }
        for (PolicyChangeField changeField : changeFields) {
            if (!PolicyChangeFieldName.masterChangeFieldNames().contains(changeField.getChangeField())) {
                throw new IllegalArgumentException("不支援回寫主檔欄位: " + changeField.getChangeField());
            }
            policyChangeDao.updateMasterField(policyNo, policySeq, changeField.getChangeField(), changeField.getContentAfter());
        }
    }

    private boolean applyMainAmountChanges(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        List<PolicyChangeField> changeFields = policyChangeDao.findChangeFieldsByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (changeFields.isEmpty()) {
            throw new IllegalStateException("找不到主保額變更欄位: " + changeCaseNo);
        }
        boolean premiumChanged = false;
        for (PolicyChangeField changeField : changeFields) {
            if (PolicyChangeFieldName.MASTER_INSURED_AMOUNT.getFieldName().equals(changeField.getChangeField())) {
                policyChangeDao.updateMasterField(policyNo, policySeq, PolicyChangeFieldName.INSURED_AMOUNT.getFieldName(), changeField.getContentAfter());
                continue;
            }
            if (isRideInsuredAmountField(changeField.getChangeField())) {
                String rideOrder = resolveRideOrder(changeField);
                if (!PolicyRideKey.MAIN.getRideOrder().equals(rideOrder)) {
                    throw new IllegalArgumentException("002 主約保額變更不可回寫附約: " + rideOrder);
                }
                policyChangeDao.updateRideAmount(policyNo, policySeq, rideOrder, changeField.getContentAfter());
                continue;
            }
            if (isRidePremiumField(changeField.getChangeField())) {
                String rideOrder = resolveRideOrder(changeField, RideChangeField.PREMIUM);
                if (!PolicyRideKey.MAIN.getRideOrder().equals(rideOrder)) {
                    throw new IllegalArgumentException("002 主約變更不可回寫附約保費: " + rideOrder);
                }
                policyChangeDao.updateRidePremium(policyNo, policySeq, rideOrder, changeField.getContentAfter());
                premiumChanged = true;
                continue;
            }
            throw new IllegalArgumentException("不支援回寫主約保額欄位: " + changeField.getChangeField());
        }
        return premiumChanged;
    }

    private boolean applyRiderAmountChanges(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        List<PolicyChangeField> changeFields = policyChangeDao.findChangeFieldsByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (changeFields.isEmpty()) {
            throw new IllegalStateException("找不到附約保額變更欄位: " + changeCaseNo);
        }
        boolean premiumChanged = false;
        for (PolicyChangeField changeField : changeFields) {
            if (isRideInsuredAmountField(changeField.getChangeField())) {
                String rideOrder = resolveRideOrder(changeField);
                if (PolicyRideKey.MAIN.getRideOrder().equals(rideOrder)) {
                    throw new IllegalArgumentException("003 附約保額變更不可回寫主約");
                }
                policyChangeDao.updateRideAmount(policyNo, policySeq, rideOrder, changeField.getContentAfter());
                continue;
            }
            if (isRidePremiumField(changeField.getChangeField())) {
                String rideOrder = resolveRideOrder(changeField, RideChangeField.PREMIUM);
                if (PolicyRideKey.MAIN.getRideOrder().equals(rideOrder)) {
                    throw new IllegalArgumentException("003 附約變更不可回寫主約保費");
                }
                policyChangeDao.updateRidePremium(policyNo, policySeq, rideOrder, changeField.getContentAfter());
                premiumChanged = true;
                continue;
            }
            throw new IllegalArgumentException("不支援回寫附約保額欄位: " + changeField.getChangeField());
        }
        return premiumChanged;
    }

    private String resolveRideOrder(PolicyChangeField changeField) {
        return resolveRideOrder(changeField, RideChangeField.INSURED_AMOUNT);
    }

    private String resolveRideOrder(PolicyChangeField changeField, RideChangeField rideChangeField) {
        if (changeField.getChangeKey() != null && !changeField.getChangeKey().isBlank()) {
            return changeField.getChangeKey();
        }
        return rideChangeField.resolveRideOrder(changeField.getChangeField());
    }

    private boolean isRideInsuredAmountField(String fieldName) {
        return RideChangeField.INSURED_AMOUNT.matches(fieldName);
    }

    private boolean isRidePremiumField(String fieldName) {
        return RideChangeField.PREMIUM.matches(fieldName);
    }

    private MainPolicyAddress readAddress(String contentAfter) {
        try {
            MainPolicyAddress address = objectMapper.readValue(contentAfter, MainPolicyAddress.class);
            address.setZipCode3(normalizeBlank(address.getZipCode3()));
            address.setZipCode2(normalizeBlank(address.getZipCode2()));
            address.setFullWidthAddress(normalizeBlank(address.getFullWidthAddress()));
            address.setHalfWidthAddress(normalizeBlank(address.getHalfWidthAddress()));
            return address;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("地址變更內容轉換失敗", e);
        }
    }
}
