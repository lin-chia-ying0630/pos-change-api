package com.alin.lin.dao.impl;

import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.entity.CodeDescription;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.entity.PolicyChangeAcceptance;
import com.alin.lin.entity.PolicyChangeField;
import com.alin.lin.entity.PolicyChangeFile;
import com.alin.lin.entity.PolicyChangeItem;
import com.alin.lin.mapper.PolicyChangeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PolicyChangeDaoImpl implements PolicyChangeDao {
    private final PolicyChangeMapper policyChangeMapper;

    public PolicyChangeDaoImpl(PolicyChangeMapper policyChangeMapper) {
        this.policyChangeMapper = policyChangeMapper;
    }

    @Override
    public MainPolicyMaster findMaster(String policyNo, Integer policySeq) {
        return policyChangeMapper.findMaster(policyNo, policySeq);
    }

    @Override
    public MainPolicyAddress findAddress(String policyNo, Integer policySeq, String addressType) {
        return policyChangeMapper.findAddress(policyNo, policySeq, addressType);
    }

    @Override
    public List<MainPolicyAddress> findAddresses(String policyNo, Integer policySeq) {
        return policyChangeMapper.findAddresses(policyNo, policySeq);
    }

    @Override
    public List<MainPolicyRide> findRides(String policyNo, Integer policySeq) {
        return policyChangeMapper.findRides(policyNo, policySeq);
    }

    @Override
    public List<CodeDescription> findCodes(String codeGroup, String codeField) {
        return policyChangeMapper.findCodes(codeGroup, codeField);
    }

    @Override
    public CodeDescription findCode(String codeGroup, String codeField, String codeBefore) {
        return policyChangeMapper.findCode(codeGroup, codeField, codeBefore);
    }

    @Override
    public boolean existsChangeCaseNo(String changeCaseNo) {
        return policyChangeMapper.existsChangeCaseNo(changeCaseNo) > 0;
    }

    @Override
    public String findMaxChangeCaseNoByPrefix(String changeCaseNoPrefix) {
        return policyChangeMapper.findMaxChangeCaseNoByPrefix(changeCaseNoPrefix);
    }

    @Override
    public List<PolicyChangeCaseDto> findChangeCases(String policyNo) {
        return policyChangeMapper.findChangeCases(policyNo);
    }

    @Override
    public PolicyChangeCaseDto findChangeCase(String policyNo, Integer policySeq, String changeCaseNo) {
        return policyChangeMapper.findChangeCase(policyNo, policySeq, changeCaseNo);
    }

    @Override
    public List<String> findChangeItemsByCaseNo(String policyNo, Integer policySeq, String changeCaseNo) {
        return policyChangeMapper.findChangeItemsByCaseNo(policyNo, policySeq, changeCaseNo);
    }

    @Override
    public List<PolicyChangeFile> findChangeFilesByItem(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        return policyChangeMapper.findChangeFilesByItem(policyNo, policySeq, changeCaseNo, changeItem);
    }

    @Override
    public List<PolicyChangeField> findChangeFieldsByItem(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        return policyChangeMapper.findChangeFieldsByItem(policyNo, policySeq, changeCaseNo, changeItem);
    }

    @Override
    public void insertAcceptance(PolicyChangeAcceptance acceptance) {
        policyChangeMapper.insertAcceptance(acceptance);
    }

    @Override
    public void insertChangeItem(PolicyChangeItem changeItem) {
        policyChangeMapper.insertChangeItem(changeItem);
    }

    @Override
    public boolean existsChangeItem(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        return policyChangeMapper.existsChangeItem(policyNo, policySeq, changeCaseNo, changeItem) > 0;
    }

    @Override
    public void insertChangeField(String policyNo, Integer policySeq, String changeCaseNo, String changeItem, String changeField, String changeKey, String contentBefore, String contentAfter) {
        policyChangeMapper.insertChangeField(policyNo, policySeq, changeCaseNo, changeItem, changeField, changeKey, contentBefore, contentAfter);
    }

    @Override
    public void insertChangeFile(String policyNo, Integer policySeq, String changeCaseNo, String changeItem, String changeFile, String contentBefore, String contentAfter) {
        policyChangeMapper.insertChangeFile(policyNo, policySeq, changeCaseNo, changeItem, changeFile, contentBefore, contentAfter);
    }

    @Override
    public void updateAcceptanceStatus(PolicyChangeAcceptance acceptance) {
        policyChangeMapper.updateAcceptanceStatus(acceptance);
    }

    @Override
    public void updateAddress(MainPolicyAddress address) {
        policyChangeMapper.updateAddress(address);
    }

    @Override
    public void updateMasterField(String policyNo, Integer policySeq, String changeField, String contentAfter) {
        policyChangeMapper.updateMasterField(policyNo, policySeq, changeField, contentAfter);
    }

    @Override
    public void updateRideAmount(String policyNo, Integer policySeq, String rideOrder, String insuredAmount) {
        policyChangeMapper.updateRideAmount(policyNo, policySeq, rideOrder, insuredAmount);
    }

    @Override
    public void updateRidePremium(String policyNo, Integer policySeq, String rideOrder, String premium) {
        policyChangeMapper.updateRidePremium(policyNo, policySeq, rideOrder, premium);
    }

    @Override
    public void updateMasterTotalPremiumFromRides(String policyNo, Integer policySeq) {
        policyChangeMapper.updateMasterTotalPremiumFromRides(policyNo, policySeq);
    }
}
