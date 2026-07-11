package com.alin.lin.service;

import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.util.PolicyChangeFieldUtil.FieldChange;

public interface PolicyChangeSupportService {
    MainPolicyMaster requirePolicy(String policyNo, Integer policySeq);

    MainPolicyRide requireMainRide(String policyNo, Integer policySeq);

    void ensureChangeCaseSaved(String policyNo, Integer policySeq, String changeCaseNo, String changeItem);

    void insertFieldChange(String policyNo, Integer policySeq, String changeCaseNo, String changeItem, FieldChange fieldChange);
}
