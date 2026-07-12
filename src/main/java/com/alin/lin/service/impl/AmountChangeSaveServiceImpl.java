package com.alin.lin.service.impl;

import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.MainAmountChangeDto;
import com.alin.lin.dto.MainAmountChangeRequest;
import com.alin.lin.dto.RideAmountChangeRequest;
import com.alin.lin.dto.RiderAmountChangeListRequest;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.enums.PolicyChangeFieldName;
import com.alin.lin.enums.PolicyRideKey;
import com.alin.lin.enums.RideChangeField;
import com.alin.lin.service.AmountChangeSaveService;
import com.alin.lin.service.CodeDescriptionService;
import com.alin.lin.service.PolicyChangeSupportService;
import com.alin.lin.util.PolicyChangeFieldUtil.FieldChange;
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
    private final PolicyChangeDao policyChangeDao;
    private final PolicyChangeSupportService policyChangeSupportService;
    private final CodeDescriptionService codeDescriptionService;

    public AmountChangeSaveServiceImpl(
            PolicyChangeDao policyChangeDao,
            PolicyChangeSupportService policyChangeSupportService,
            CodeDescriptionService codeDescriptionService
    ) {
        this.policyChangeDao = policyChangeDao;
        this.policyChangeSupportService = policyChangeSupportService;
        this.codeDescriptionService = codeDescriptionService;
    }

    @Override
    @Transactional
    public MainAmountChangeDto saveMainAmountChange(String changeCaseNo, MainAmountChangeRequest request) {
        MainPolicyMaster master = policyChangeSupportService.requirePolicy(request.getPolicyNo(), request.getPolicySeq());
        requireText(changeCaseNo, "changeCaseNo");
        requireNotNull(request.getMasterInsuredAmount(), "masterInsuredAmount");

        String changeItem = codeDescriptionService.mainAmountChangeItemCode();
        policyChangeDao.deleteChangeFieldsByItem(request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, changeItem);
        if (amountEquals(master.getInsuredAmount(), request.getMasterInsuredAmount())) {
            policyChangeSupportService.removeEmptyChangeItemAndAcceptance(
                    request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, changeItem
            );
            return result(changeCaseNo, changeItem, 0);
        }

        MainPolicyRide mainRide = policyChangeSupportService.requireMainRide(request.getPolicyNo(), request.getPolicySeq());
        List<FieldChange> fieldChanges = List.of(
                new FieldChange(
                        PolicyChangeFieldName.MASTER_INSURED_AMOUNT.getFieldName(),
                        PolicyChangeFieldName.MASTER_CHANGE_KEY.getFieldName(),
                        amountToString(master.getInsuredAmount()),
                        amountToString(request.getMasterInsuredAmount())
                ),
                new FieldChange(
                        RideChangeField.INSURED_AMOUNT.fieldName(PolicyRideKey.MAIN.getRideOrder()),
                        PolicyRideKey.MAIN.getRideOrder(),
                        amountToString(mainRide.getInsuredAmount()),
                        amountToString(request.getMasterInsuredAmount())
                )
        );

        policyChangeSupportService.ensureChangeCaseSaved(
                request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, changeItem
        );
        fieldChanges.forEach(fieldChange -> policyChangeSupportService.upsertFieldChange(
                request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, changeItem, fieldChange
        ));
        return result(changeCaseNo, changeItem, fieldChanges.size());
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
}
