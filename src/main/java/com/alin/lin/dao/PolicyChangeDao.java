package com.alin.lin.dao;

import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.entity.CodeDescription;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.entity.PolicyChangeAcceptance;
import com.alin.lin.entity.PolicyChangeCaseReservation;
import com.alin.lin.entity.PolicyChangeCaseReservationItem;
import com.alin.lin.entity.PolicyChangeField;
import com.alin.lin.entity.PolicyChangeFile;
import com.alin.lin.entity.PolicyChangeItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * MyBatis 直接產生此 DAO 的代理實作，維持 Controller -> Service -> DAO 三層架構。
 */
@Mapper
public interface PolicyChangeDao {
    MainPolicyMaster findMaster(@Param("policyNo") String policyNo, @Param("policySeq") Integer policySeq);

    MainPolicyMaster findMasterForUpdate(@Param("policyNo") String policyNo,
                                         @Param("policySeq") Integer policySeq);

    MainPolicyAddress findAddress(@Param("policyNo") String policyNo,
                                  @Param("policySeq") Integer policySeq,
                                  @Param("addressType") String addressType);

    MainPolicyAddress findAddressForUpdate(@Param("policyNo") String policyNo,
                                           @Param("policySeq") Integer policySeq,
                                           @Param("addressType") String addressType);

    List<MainPolicyAddress> findAddresses(@Param("policyNo") String policyNo,
                                          @Param("policySeq") Integer policySeq);

    List<MainPolicyRide> findRides(@Param("policyNo") String policyNo,
                                   @Param("policySeq") Integer policySeq);

    List<MainPolicyRide> findRidesForUpdate(@Param("policyNo") String policyNo,
                                            @Param("policySeq") Integer policySeq);

    List<CodeDescription> findCodes(@Param("codeGroup") String codeGroup, @Param("codeField") String codeField);

    List<CodeDescription> findCodesByGroup(@Param("codeGroup") String codeGroup);

    CodeDescription findCode(@Param("codeGroup") String codeGroup,
                             @Param("codeField") String codeField,
                             @Param("codeBefore") String codeBefore);

    int incrementCaseSequence(@Param("sequenceDate") LocalDate sequenceDate);

    Long findLastInsertedSequence();

    int insertCaseReservation(PolicyChangeCaseReservation reservation);

    int insertCaseReservationItem(PolicyChangeCaseReservationItem reservationItem);

    PolicyChangeCaseReservation findCaseReservationForUpdate(@Param("changeCaseNo") String changeCaseNo);

    List<String> findReservedChangeItems(@Param("changeCaseNo") String changeCaseNo);

    int consumeCaseReservation(@Param("changeCaseNo") String changeCaseNo,
                               @Param("reservedBy") String reservedBy);

    List<PolicyChangeCaseDto> findChangeCases(@Param("policyNo") String policyNo);

    PolicyChangeCaseDto findChangeCase(@Param("policyNo") String policyNo,
                                       @Param("policySeq") Integer policySeq,
                                       @Param("changeCaseNo") String changeCaseNo);

    // 依保單與保全變更項目取得最近一筆已受理案件，供申請資格檢核。
    PolicyChangeCaseDto findLatestChangeCaseByItem(@Param("policyNo") String policyNo,
                                                    @Param("policySeq") Integer policySeq,
                                                    @Param("changeItem") String changeItem);

    PolicyChangeAcceptance findAcceptanceForUpdate(@Param("policyNo") String policyNo,
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

    List<PolicyChangeFile> findChangeFilesByCaseNo(@Param("policyNo") String policyNo,
                                                   @Param("policySeq") Integer policySeq,
                                                   @Param("changeCaseNo") String changeCaseNo);

    List<PolicyChangeField> findChangeFieldsByCaseNo(@Param("policyNo") String policyNo,
                                                     @Param("policySeq") Integer policySeq,
                                                     @Param("changeCaseNo") String changeCaseNo);

    int insertAcceptance(PolicyChangeAcceptance acceptance);

    int insertChangeItem(PolicyChangeItem changeItem);

    int existsChangeItem(@Param("policyNo") String policyNo,
                         @Param("policySeq") Integer policySeq,
                         @Param("changeCaseNo") String changeCaseNo,
                         @Param("changeItem") String changeItem);

    int upsertChangeField(@Param("policyNo") String policyNo,
                          @Param("policySeq") Integer policySeq,
                          @Param("changeCaseNo") String changeCaseNo,
                          @Param("changeItem") String changeItem,
                          @Param("changeField") String changeField,
                          @Param("changeKey") String changeKey,
                          @Param("contentBefore") String contentBefore,
                          @Param("contentAfter") String contentAfter);

    int upsertChangeFile(@Param("policyNo") String policyNo,
                         @Param("policySeq") Integer policySeq,
                         @Param("changeCaseNo") String changeCaseNo,
                         @Param("changeItem") String changeItem,
                         @Param("changeFile") String changeFile,
                         @Param("changeKey") String changeKey,
                         @Param("contentBefore") String contentBefore,
                         @Param("contentAfter") String contentAfter);

    int deleteChangeFieldsByItem(@Param("policyNo") String policyNo,
                                 @Param("policySeq") Integer policySeq,
                                 @Param("changeCaseNo") String changeCaseNo,
                                 @Param("changeItem") String changeItem);

    int deleteChangeFieldsByItemAndKey(@Param("policyNo") String policyNo,
                                       @Param("policySeq") Integer policySeq,
                                       @Param("changeCaseNo") String changeCaseNo,
                                       @Param("changeItem") String changeItem,
                                       @Param("changeKey") String changeKey);

    int deleteChangeFileByItemAndKey(@Param("policyNo") String policyNo,
                                     @Param("policySeq") Integer policySeq,
                                     @Param("changeCaseNo") String changeCaseNo,
                                     @Param("changeItem") String changeItem,
                                     @Param("changeFile") String changeFile,
                                     @Param("changeKey") String changeKey);

    int countChangeFieldsByItem(@Param("policyNo") String policyNo,
                                @Param("policySeq") Integer policySeq,
                                @Param("changeCaseNo") String changeCaseNo,
                                @Param("changeItem") String changeItem);

    int countChangeFilesByItem(@Param("policyNo") String policyNo,
                               @Param("policySeq") Integer policySeq,
                               @Param("changeCaseNo") String changeCaseNo,
                               @Param("changeItem") String changeItem);

    int deleteChangeItem(@Param("policyNo") String policyNo,
                         @Param("policySeq") Integer policySeq,
                         @Param("changeCaseNo") String changeCaseNo,
                         @Param("changeItem") String changeItem);

    int countChangeItemsByCaseNo(@Param("policyNo") String policyNo,
                                 @Param("policySeq") Integer policySeq,
                                 @Param("changeCaseNo") String changeCaseNo);

    int deleteAcceptance(@Param("policyNo") String policyNo,
                         @Param("policySeq") Integer policySeq,
                         @Param("changeCaseNo") String changeCaseNo,
                         @Param("expectedStatus") String expectedStatus);

    int updateAcceptanceStatusIfCurrent(@Param("acceptance") PolicyChangeAcceptance acceptance,
                                        @Param("expectedStatus") String expectedStatus);

    int updateAddress(MainPolicyAddress address);

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
