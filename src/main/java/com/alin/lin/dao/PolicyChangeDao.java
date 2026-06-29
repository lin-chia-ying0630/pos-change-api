package com.alin.lin.dao;

import com.alin.lin.entity.CodeDescription;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.entity.PolicyChangeField;
import com.alin.lin.entity.PolicyChangeFile;
import com.alin.lin.mapper.PolicyChangeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PolicyChangeDao {
    private final PolicyChangeMapper policyChangeMapper;

    public PolicyChangeDao(PolicyChangeMapper policyChangeMapper) {
        this.policyChangeMapper = policyChangeMapper;
    }

    public MainPolicyMaster findMaster(String policyNo, Integer policySeq) {
        return policyChangeMapper.findMaster(policyNo, policySeq);
    }

    public MainPolicyAddress findAddress(String policyNo, Integer policySeq, String addressType) {
        return policyChangeMapper.findAddress(policyNo, policySeq, addressType);
    }

    public List<MainPolicyAddress> findAddresses(String policyNo, Integer policySeq) {
        return policyChangeMapper.findAddresses(policyNo, policySeq);
    }

    public List<MainPolicyRide> findRides(String policyNo, Integer policySeq) {
        return policyChangeMapper.findRides(policyNo, policySeq);
    }

    public List<CodeDescription> findCodes(String codeGroup, String codeField) {
        return policyChangeMapper.findCodes(codeGroup, codeField);
    }

    public boolean existsChangeCaseNo(String changeCaseNo) {
        return policyChangeMapper.existsChangeCaseNo(changeCaseNo) > 0;
    }

    public String findMaxChangeCaseNoByPrefix(String changeCaseNoPrefix) {
        return policyChangeMapper.findMaxChangeCaseNoByPrefix(changeCaseNoPrefix);
    }

    public List<PolicyChangeCaseDto> findChangeCases(String policyNo) {
        return policyChangeMapper.findChangeCases(policyNo);
    }

    public PolicyChangeCaseDto findChangeCase(String policyNo, Integer policySeq, String changeCaseNo) {
        return policyChangeMapper.findChangeCase(policyNo, policySeq, changeCaseNo);
    }

    public List<String> findChangeItemsByCaseNo(String policyNo, Integer policySeq, String changeCaseNo) {
        return policyChangeMapper.findChangeItemsByCaseNo(policyNo, policySeq, changeCaseNo);
    }

    public List<PolicyChangeFile> findChangeFilesByItem(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        return policyChangeMapper.findChangeFilesByItem(policyNo, policySeq, changeCaseNo, changeItem);
    }

    public List<PolicyChangeField> findChangeFieldsByItem(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        return policyChangeMapper.findChangeFieldsByItem(policyNo, policySeq, changeCaseNo, changeItem);
    }

    public void insertAcceptance(String policyNo, Integer policySeq, String changeCaseNo, String acceptanceStatus) {
        policyChangeMapper.insertAcceptance(policyNo, policySeq, changeCaseNo, acceptanceStatus);
    }

    public void insertChangeItem(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        policyChangeMapper.insertChangeItem(policyNo, policySeq, changeCaseNo, changeItem);
    }

    public boolean existsChangeItem(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        return policyChangeMapper.existsChangeItem(policyNo, policySeq, changeCaseNo, changeItem) > 0;
    }

    public void insertChangeField(String policyNo, Integer policySeq, String changeCaseNo, String changeItem, String changeField, String changeKey, String contentBefore, String contentAfter) {
        policyChangeMapper.insertChangeField(policyNo, policySeq, changeCaseNo, changeItem, changeField, changeKey, contentBefore, contentAfter);
    }

    public void insertChangeFile(String policyNo, Integer policySeq, String changeCaseNo, String changeItem, String changeFile, String contentBefore, String contentAfter) {
        policyChangeMapper.insertChangeFile(policyNo, policySeq, changeCaseNo, changeItem, changeFile, contentBefore, contentAfter);
    }

    public void updateAcceptanceStatus(String policyNo, Integer policySeq, String changeCaseNo, String acceptanceStatus) {
        policyChangeMapper.updateAcceptanceStatus(policyNo, policySeq, changeCaseNo, acceptanceStatus);
    }

    public void updateAddress(MainPolicyAddress address) {
        policyChangeMapper.updateAddress(address);
    }

    public void updateMasterField(String policyNo, Integer policySeq, String changeField, String contentAfter) {
        policyChangeMapper.updateMasterField(policyNo, policySeq, changeField, contentAfter);
    }

    public void updateRideAmount(String policyNo, Integer policySeq, String rideOrder, String insuredAmount) {
        policyChangeMapper.updateRideAmount(policyNo, policySeq, rideOrder, insuredAmount);
    }
}
