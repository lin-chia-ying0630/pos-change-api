package com.alin.lin.service.impl;

import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.MainAmountChangeDto;
import com.alin.lin.dto.MainAmountChangeRequest;
import com.alin.lin.dto.RideAmountChangeRequest;
import com.alin.lin.dto.RiderAmountChangeListRequest;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.enums.PolicyRideKey;
import com.alin.lin.enums.RideChangeField;
import com.alin.lin.service.AmountChangeSaveService;
import com.alin.lin.service.CodeDescriptionService;
import com.alin.lin.service.PolicyChangeSupportService;
import com.alin.lin.util.PolicyChangeFieldUtil.FieldChange;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.alin.lin.util.PolicyChangeFieldUtil.addAmountChangeIfDifferent;
import static com.alin.lin.util.PolicyChangeFieldUtil.amountEquals;
import static com.alin.lin.util.PolicyChangeFieldUtil.amountToString;
import static com.alin.lin.util.PolicyChangeFieldUtil.requireNotEmpty;
import static com.alin.lin.util.PolicyChangeFieldUtil.requireNotNull;
import static com.alin.lin.util.PolicyChangeFieldUtil.requireText;

@Service
public class AmountChangeSaveServiceImpl implements AmountChangeSaveService {
    private static final String RIDE_CHANGE_FILE = "main_policy_ride";

    private final PolicyChangeDao policyChangeDao;
    private final PolicyChangeSupportService policyChangeSupportService;
    private final CodeDescriptionService codeDescriptionService;
    private final ObjectMapper objectMapper;

    public AmountChangeSaveServiceImpl(
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
    public MainAmountChangeDto saveMainAmountChange(String changeCaseNo, MainAmountChangeRequest request) {
        policyChangeSupportService.requirePolicy(request.getPolicyNo(), request.getPolicySeq());
        requireText(changeCaseNo, "changeCaseNo");
        requireNotNull(request.getInsuredAmount(), "insuredAmount");

        String changeItem = codeDescriptionService.mainAmountChangeItemCode();
        policyChangeSupportService.validateChangeCaseAccess(
                request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, changeItem
        );
        policyChangeDao.deleteChangeFieldsByItem(request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, changeItem);
        policyChangeDao.deleteChangeFileByItemAndKey(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                changeItem,
                RIDE_CHANGE_FILE,
                PolicyRideKey.MAIN.getRideOrder()
        );
        MainPolicyRide mainRide = policyChangeSupportService.requireMainRide(request.getPolicyNo(), request.getPolicySeq());
        if (amountEquals(mainRide.getInsuredAmount(), request.getInsuredAmount())) {
            policyChangeSupportService.removeEmptyChangeItemAndAcceptance(
                    request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, changeItem
            );
            return result(changeCaseNo, changeItem, 0);
        }

        FieldChange fieldChange = new FieldChange(
                RideChangeField.INSURED_AMOUNT.fieldName(PolicyRideKey.MAIN.getRideOrder()),
                PolicyRideKey.MAIN.getRideOrder(),
                amountToString(mainRide.getInsuredAmount()),
                amountToString(request.getInsuredAmount())
        );

        policyChangeSupportService.ensureChangeCaseSaved(
                request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, changeItem
        );
        policyChangeSupportService.upsertFieldChange(
                request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, changeItem, fieldChange
        );
        policyChangeDao.upsertChangeFile(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                changeItem,
                RIDE_CHANGE_FILE,
                PolicyRideKey.MAIN.getRideOrder(),
                toJson(rideSnapshot(mainRide, mainRide.getInsuredAmount())),
                toJson(rideSnapshot(mainRide, request.getInsuredAmount()))
        );
        // 002 只異動主附約檔的主約列，對使用者回傳一筆業務異動。
        return result(changeCaseNo, changeItem, 1);
    }

    @Override
    @Transactional
    public MainAmountChangeDto saveRiderAmountChange(
            String changeCaseNo,
            String policyNo,
            Integer policySeq,
            RiderAmountChangeListRequest request
    ) {
        policyChangeSupportService.requirePolicy(policyNo, policySeq);
        requireText(changeCaseNo, "changeCaseNo");
        requireNotEmpty(request.getRides(), "rides");

        List<MainPolicyRide> rides = policyChangeDao.findRides(policyNo, policySeq);
        Map<String, MainPolicyRide> rideMap = new LinkedHashMap<>();
        rides.forEach(ride -> rideMap.put(ride.getRideOrder(), ride));

        List<FieldChange> fieldChanges = new ArrayList<>();
        Set<String> requestedRideOrders = new HashSet<>();
        for (RideAmountChangeRequest changedRide : request.getRides()) {
            requireText(changedRide.getRideOrder(), "rideOrder");
            requireNotNull(changedRide.getInsuredAmount(), "ride insuredAmount");
            if (!requestedRideOrders.add(changedRide.getRideOrder())) {
                throw new IllegalArgumentException("附約序號不可重複: " + changedRide.getRideOrder());
            }
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

        String changeItem = codeDescriptionService.riderAmountChangeItemCode();
        policyChangeSupportService.validateChangeCaseAccess(policyNo, policySeq, changeCaseNo, changeItem);
        policyChangeDao.deleteChangeFieldsByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (fieldChanges.isEmpty()) {
            policyChangeSupportService.removeEmptyChangeItemAndAcceptance(policyNo, policySeq, changeCaseNo, changeItem);
            return result(changeCaseNo, changeItem, 0);
        }

        policyChangeSupportService.ensureChangeCaseSaved(policyNo, policySeq, changeCaseNo, changeItem);
        fieldChanges.forEach(fieldChange -> policyChangeSupportService.upsertFieldChange(
                policyNo, policySeq, changeCaseNo, changeItem, fieldChange
        ));
        return result(changeCaseNo, changeItem, fieldChanges.size());
    }

    private MainAmountChangeDto result(String changeCaseNo, String changeItem, int changedFieldCount) {
        return MainAmountChangeDto.builder()
                .changeCaseNo(changeCaseNo)
                .changeItem(changeItem)
                .changedFieldCount(changedFieldCount)
                .build();
    }

    private Map<String, Object> rideSnapshot(MainPolicyRide ride, java.math.BigDecimal insuredAmount) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("policyNo", ride.getPolicyNo());
        snapshot.put("policySeq", ride.getPolicySeq());
        snapshot.put("rideType", ride.getRideType());
        snapshot.put("rideOrder", ride.getRideOrder());
        snapshot.put("productCode", ride.getProductCode());
        snapshot.put("policyYears", ride.getPolicyYears());
        snapshot.put("insuredAmount", insuredAmount);
        snapshot.put("premium", ride.getPremium());
        return snapshot;
    }

    private String toJson(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("主約資料轉換失敗", exception);
        }
    }
}
