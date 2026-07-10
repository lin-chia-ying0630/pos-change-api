package com.alin.lin.dao;

import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.entity.CodeDescription;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.entity.PolicyChangeAcceptance;
import com.alin.lin.entity.PolicyChangeField;
import com.alin.lin.entity.PolicyChangeFile;
import com.alin.lin.entity.PolicyChangeItem;

import java.util.List;

public interface PolicyChangeDao {
    MainPolicyMaster findMaster(String policyNo, Integer policySeq);

    MainPolicyAddress findAddress(String policyNo, Integer policySeq, String addressType);

    List<MainPolicyAddress> findAddresses(String policyNo, Integer policySeq);

    List<MainPolicyRide> findRides(String policyNo, Integer policySeq);

    List<CodeDescription> findCodes(String codeGroup, String codeField);

    CodeDescription findCode(String codeGroup, String codeField, String codeBefore);

    boolean existsChangeCaseNo(String changeCaseNo);

    String findMaxChangeCaseNoByPrefix(String changeCaseNoPrefix);

    List<PolicyChangeCaseDto> findChangeCases(String policyNo);

    PolicyChangeCaseDto findChangeCase(String policyNo, Integer policySeq, String changeCaseNo);

    List<String> findChangeItemsByCaseNo(String policyNo, Integer policySeq, String changeCaseNo);

    List<PolicyChangeFile> findChangeFilesByItem(String policyNo, Integer policySeq, String changeCaseNo, String changeItem);

    List<PolicyChangeField> findChangeFieldsByItem(String policyNo, Integer policySeq, String changeCaseNo, String changeItem);

    void insertAcceptance(PolicyChangeAcceptance acceptance);

    void insertChangeItem(PolicyChangeItem changeItem);

    boolean existsChangeItem(String policyNo, Integer policySeq, String changeCaseNo, String changeItem);

    void insertChangeField(String policyNo, Integer policySeq, String changeCaseNo, String changeItem, String changeField, String changeKey, String contentBefore, String contentAfter);

    void insertChangeFile(String policyNo, Integer policySeq, String changeCaseNo, String changeItem, String changeFile, String contentBefore, String contentAfter);

    void updateAcceptanceStatus(PolicyChangeAcceptance acceptance);

    void updateAddress(MainPolicyAddress address);

    void updateMasterField(String policyNo, Integer policySeq, String changeField, String contentAfter);

    void updateRideAmount(String policyNo, Integer policySeq, String rideOrder, String insuredAmount);

    void updateRidePremium(String policyNo, Integer policySeq, String rideOrder, String premium);

    void updateMasterTotalPremiumFromRides(String policyNo, Integer policySeq);
}
