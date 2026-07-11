package com.alin.lin.service.impl;

import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.entity.PolicyChangeAcceptance;
import com.alin.lin.entity.PolicyChangeItem;
import com.alin.lin.enums.PolicyRideKey;
import com.alin.lin.service.CodeDescriptionService;
import com.alin.lin.service.PolicyChangeSupportService;
import com.alin.lin.util.PolicyChangeFieldUtil.FieldChange;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

import static com.alin.lin.util.PolicyChangeFieldUtil.requireNotNull;
import static com.alin.lin.util.PolicyChangeFieldUtil.requireText;

@Service
public class PolicyChangeSupportServiceImpl implements PolicyChangeSupportService {
    private final PolicyChangeDao policyChangeDao;
    private final CodeDescriptionService codeDescriptionService;

    public PolicyChangeSupportServiceImpl(PolicyChangeDao policyChangeDao, CodeDescriptionService codeDescriptionService) {
        this.policyChangeDao = policyChangeDao;
        this.codeDescriptionService = codeDescriptionService;
    }

    @Override
    public MainPolicyMaster requirePolicy(String policyNo, Integer policySeq) {
        requireText(policyNo, "policyNo");
        requireNotNull(policySeq, "policySeq");
        MainPolicyMaster master = policyChangeDao.findMaster(policyNo, policySeq);
        if (master == null) {
            throw new NoSuchElementException("找不到保單: " + policyNo + "-" + policySeq);
        }
        return master;
    }

    @Override
    public MainPolicyRide requireMainRide(String policyNo, Integer policySeq) {
        return policyChangeDao.findRides(policyNo, policySeq).stream()
                .filter(ride -> codeDescriptionService.mainRideTypeCode().equals(ride.getRideType())
                        || PolicyRideKey.MAIN.getRideOrder().equals(ride.getRideOrder()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("找不到主約資料: " + policyNo + "-" + policySeq));
    }

    @Override
    public void ensureChangeCaseSaved(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
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

    @Override
    public void insertFieldChange(String policyNo, Integer policySeq, String changeCaseNo, String changeItem, FieldChange fieldChange) {
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
}
