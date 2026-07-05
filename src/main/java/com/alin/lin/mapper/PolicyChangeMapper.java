package com.alin.lin.mapper;

import com.alin.lin.entity.CodeDescription;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.entity.PolicyChangeField;
import com.alin.lin.entity.PolicyChangeFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PolicyChangeMapper {
    MainPolicyMaster findMaster(@Param("policyNo") String policyNo, @Param("policySeq") Integer policySeq);

    MainPolicyAddress findAddress(@Param("policyNo") String policyNo,
                                  @Param("policySeq") Integer policySeq,
                                  @Param("addressType") String addressType);

    List<MainPolicyAddress> findAddresses(@Param("policyNo") String policyNo,
                                          @Param("policySeq") Integer policySeq);

    List<MainPolicyRide> findRides(@Param("policyNo") String policyNo,
                                   @Param("policySeq") Integer policySeq);

    List<CodeDescription> findCodes(@Param("codeGroup") String codeGroup, @Param("codeField") String codeField);

    CodeDescription findCode(@Param("codeGroup") String codeGroup,
                             @Param("codeField") String codeField,
                             @Param("codeBefore") String codeBefore);

    int existsChangeCaseNo(@Param("changeCaseNo") String changeCaseNo);

    String findMaxChangeCaseNoByPrefix(@Param("changeCaseNoPrefix") String changeCaseNoPrefix);

    List<PolicyChangeCaseDto> findChangeCases(@Param("policyNo") String policyNo);

    PolicyChangeCaseDto findChangeCase(@Param("policyNo") String policyNo,
                                       @Param("policySeq") Integer policySeq,
                                       @Param("changeCaseNo") String changeCaseNo);

    List<String> findChangeItemsByCaseNo(@Param("policyNo") String policyNo,
                                         @Param("policySeq") Integer policySeq,
                                         @Param("changeCaseNo") String changeCaseNo);

    List<PolicyChangeFile> findChangeFilesByItem(@Param("policyNo") String policyNo,
                                                 @Param("policySeq") Integer policySeq,
                                                 @Param("changeCaseNo") String changeCaseNo,
                                                 @Param("changeItem") String changeItem);

    List<PolicyChangeField> findChangeFieldsByItem(@Param("policyNo") String policyNo,
                                                   @Param("policySeq") Integer policySeq,
                                                   @Param("changeCaseNo") String changeCaseNo,
                                                   @Param("changeItem") String changeItem);

    int insertAcceptance(@Param("policyNo") String policyNo,
                         @Param("policySeq") Integer policySeq,
                         @Param("changeCaseNo") String changeCaseNo,
                         @Param("acceptanceStatus") String acceptanceStatus);

    int insertChangeItem(@Param("policyNo") String policyNo,
                         @Param("policySeq") Integer policySeq,
                         @Param("changeCaseNo") String changeCaseNo,
                         @Param("changeItem") String changeItem);

    int existsChangeItem(@Param("policyNo") String policyNo,
                         @Param("policySeq") Integer policySeq,
                         @Param("changeCaseNo") String changeCaseNo,
                         @Param("changeItem") String changeItem);

    int insertChangeField(@Param("policyNo") String policyNo,
                          @Param("policySeq") Integer policySeq,
                          @Param("changeCaseNo") String changeCaseNo,
                          @Param("changeItem") String changeItem,
                          @Param("changeField") String changeField,
                          @Param("changeKey") String changeKey,
                          @Param("contentBefore") String contentBefore,
                          @Param("contentAfter") String contentAfter);

    int insertChangeFile(@Param("policyNo") String policyNo,
                         @Param("policySeq") Integer policySeq,
                         @Param("changeCaseNo") String changeCaseNo,
                         @Param("changeItem") String changeItem,
                         @Param("changeFile") String changeFile,
                         @Param("contentBefore") String contentBefore,
                         @Param("contentAfter") String contentAfter);

    int updateAcceptanceStatus(@Param("policyNo") String policyNo,
                               @Param("policySeq") Integer policySeq,
                               @Param("changeCaseNo") String changeCaseNo,
                               @Param("acceptanceStatus") String acceptanceStatus);

    int updateAddress(MainPolicyAddress address);

    int updateMasterField(@Param("policyNo") String policyNo,
                          @Param("policySeq") Integer policySeq,
                          @Param("changeField") String changeField,
                          @Param("contentAfter") String contentAfter);

    int updateRideAmount(@Param("policyNo") String policyNo,
                         @Param("policySeq") Integer policySeq,
                         @Param("rideOrder") String rideOrder,
                         @Param("insuredAmount") String insuredAmount);

    int updateRidePremium(@Param("policyNo") String policyNo,
                          @Param("policySeq") Integer policySeq,
                          @Param("rideOrder") String rideOrder,
                          @Param("premium") String premium);

    int updateMasterTotalPremiumFromRides(@Param("policyNo") String policyNo,
                                          @Param("policySeq") Integer policySeq);
}
