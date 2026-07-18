package com.alin.lin.service.impl;

import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.entity.PolicyChangeField;
import com.alin.lin.entity.PolicyChangeFile;
import com.alin.lin.enums.PolicyRideKey;
import com.alin.lin.enums.RideChangeField;
import com.alin.lin.exception.ChangeCaseConflictException;
import com.alin.lin.service.ChangeCaseApplyService;
import com.alin.lin.service.CodeDescriptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alin.lin.util.PolicyChangeFieldUtil.amountEquals;
import static com.alin.lin.util.PolicyChangeFieldUtil.collectAddressFieldChanges;
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
                    refreshMasterTotalPremium(policyNo, policySeq);
                }
                continue;
            }
            if (riderAmountChangeItem.equals(changeItem)) {
                premiumTotalShouldRefresh = applyRiderAmountChanges(policyNo, policySeq, changeCaseNo, changeItem);
                appliedItemCount++;
                if (premiumTotalShouldRefresh) {
                    refreshMasterTotalPremium(policyNo, policySeq);
                }
                continue;
            }
            throw new IllegalArgumentException("不支援的保全變更項目: " + changeItem);
        }
        return appliedItemCount;
    }

    private void applyAddressChanges(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        List<PolicyChangeFile> changeFiles = policyChangeDao.findChangeFilesByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (changeFiles.isEmpty()) {
            throw new IllegalStateException("找不到地址變更檔案: " + changeCaseNo);
        }
        for (PolicyChangeFile changeFile : changeFiles) {
            MainPolicyAddress beforeAddress = readAddress(changeFile.getContentBefore());
            MainPolicyAddress address = readAddress(changeFile.getContentAfter());
            MainPolicyAddress currentAddress = policyChangeDao.findAddressForUpdate(
                    policyNo, policySeq, address.getAddressType()
            );
            if (currentAddress == null || !collectAddressFieldChanges(currentAddress, beforeAddress).isEmpty()) {
                throw new ChangeCaseConflictException("地址資料已被其他案件修改，請重新建立變更: " + address.getAddressType());
            }
            requireSingleRowUpdate(policyChangeDao.updateAddress(address), "地址回寫失敗: " + address.getAddressType());
        }
    }

    private boolean applyMainAmountChanges(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        List<PolicyChangeField> changeFields = policyChangeDao.findChangeFieldsByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (changeFields.isEmpty()) {
            throw new IllegalStateException("找不到主保額變更欄位: " + changeCaseNo);
        }
        lockMaster(policyNo, policySeq);
        Map<String, MainPolicyRide> rides = lockedRideMap(policyNo, policySeq);
        for (PolicyChangeField changeField : changeFields) {
            assertRideBeforeValue(rides, changeField);
        }
        boolean premiumChanged = false;
        for (PolicyChangeField changeField : changeFields) {
            if (isRideInsuredAmountField(changeField.getChangeField())) {
                String rideOrder = resolveRideOrder(changeField);
                if (!PolicyRideKey.MAIN.getRideOrder().equals(rideOrder)) {
                    throw new IllegalArgumentException("002 主約保額變更不可回寫附約: " + rideOrder);
                }
                requireSingleRowUpdate(
                        policyChangeDao.updateRideAmount(policyNo, policySeq, rideOrder, changeField.getContentAfter()),
                        "主約保額回寫失敗: " + rideOrder
                );
                continue;
            }
            if (isRidePremiumField(changeField.getChangeField())) {
                String rideOrder = resolveRideOrder(changeField, RideChangeField.PREMIUM);
                if (!PolicyRideKey.MAIN.getRideOrder().equals(rideOrder)) {
                    throw new IllegalArgumentException("002 主約變更不可回寫附約保費: " + rideOrder);
                }
                requireSingleRowUpdate(
                        policyChangeDao.updateRidePremium(policyNo, policySeq, rideOrder, changeField.getContentAfter()),
                        "主約保費回寫失敗: " + rideOrder
                );
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
        lockMaster(policyNo, policySeq);
        Map<String, MainPolicyRide> rides = lockedRideMap(policyNo, policySeq);
        for (PolicyChangeField changeField : changeFields) {
            assertRideBeforeValue(rides, changeField);
        }
        boolean premiumChanged = false;
        for (PolicyChangeField changeField : changeFields) {
            if (isRideInsuredAmountField(changeField.getChangeField())) {
                String rideOrder = resolveRideOrder(changeField);
                if (PolicyRideKey.MAIN.getRideOrder().equals(rideOrder)) {
                    throw new IllegalArgumentException("003 附約保額變更不可回寫主約");
                }
                requireSingleRowUpdate(
                        policyChangeDao.updateRideAmount(policyNo, policySeq, rideOrder, changeField.getContentAfter()),
                        "附約保額回寫失敗: " + rideOrder
                );
                continue;
            }
            if (isRidePremiumField(changeField.getChangeField())) {
                String rideOrder = resolveRideOrder(changeField, RideChangeField.PREMIUM);
                if (PolicyRideKey.MAIN.getRideOrder().equals(rideOrder)) {
                    throw new IllegalArgumentException("003 附約變更不可回寫主約保費");
                }
                requireSingleRowUpdate(
                        policyChangeDao.updateRidePremium(policyNo, policySeq, rideOrder, changeField.getContentAfter()),
                        "附約保費回寫失敗: " + rideOrder
                );
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

    private MainPolicyMaster lockMaster(String policyNo, Integer policySeq) {
        MainPolicyMaster master = policyChangeDao.findMasterForUpdate(policyNo, policySeq);
        if (master == null) {
            throw new ChangeCaseConflictException("保單主檔已不存在，請重新查詢");
        }
        return master;
    }

    private Map<String, MainPolicyRide> lockedRideMap(String policyNo, Integer policySeq) {
        return policyChangeDao.findRidesForUpdate(policyNo, policySeq).stream()
                .collect(Collectors.toMap(MainPolicyRide::getRideOrder, Function.identity()));
    }

    private void refreshMasterTotalPremium(String policyNo, Integer policySeq) {
        requireSingleRowUpdate(
                policyChangeDao.updateMasterTotalPremiumFromRides(policyNo, policySeq),
                "保單總保費回寫失敗"
        );
    }

    private void requireSingleRowUpdate(int updatedRows, String errorMessage) {
        if (updatedRows != 1) {
            throw new ChangeCaseConflictException(errorMessage);
        }
    }

    private void assertRideBeforeValue(Map<String, MainPolicyRide> rides, PolicyChangeField changeField) {
        String rideOrder;
        if (isRideInsuredAmountField(changeField.getChangeField())) {
            rideOrder = resolveRideOrder(changeField);
            MainPolicyRide ride = requireRide(rides, rideOrder);
            assertAmountUnchanged(ride.getInsuredAmount(), changeField, "主附約保額 " + rideOrder);
            return;
        }
        if (isRidePremiumField(changeField.getChangeField())) {
            rideOrder = resolveRideOrder(changeField, RideChangeField.PREMIUM);
            MainPolicyRide ride = requireRide(rides, rideOrder);
            assertAmountUnchanged(ride.getPremium(), changeField, "主附約保費 " + rideOrder);
            return;
        }
        throw new IllegalArgumentException("不支援的主附約異動欄位: " + changeField.getChangeField());
    }

    private MainPolicyRide requireRide(Map<String, MainPolicyRide> rides, String rideOrder) {
        MainPolicyRide ride = rides.get(rideOrder);
        if (ride == null) {
            throw new ChangeCaseConflictException("主附約資料已不存在: " + rideOrder);
        }
        return ride;
    }

    private void assertAmountUnchanged(
            java.math.BigDecimal currentValue,
            PolicyChangeField changeField,
            String displayName
    ) {
        java.math.BigDecimal beforeValue = changeField.getContentBefore() == null
                ? null
                : new java.math.BigDecimal(changeField.getContentBefore());
        if (!amountEquals(currentValue, beforeValue)) {
            throw new ChangeCaseConflictException(displayName + " 已被其他案件修改，請重新建立變更");
        }
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
