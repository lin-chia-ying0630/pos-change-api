package com.alin.lin.service.impl;

import com.alin.lin.config.PosChangeProperties;
import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.entity.PolicyChangeAcceptance;
import com.alin.lin.entity.PolicyChangeCaseReservation;
import com.alin.lin.entity.PolicyChangeItem;
import com.alin.lin.enums.PolicyRideKey;
import com.alin.lin.exception.ChangeCaseConflictException;
import com.alin.lin.service.CodeDescriptionService;
import com.alin.lin.service.CurrentUserService;
import com.alin.lin.service.PolicyChangeSupportService;
import com.alin.lin.util.PolicyChangeFieldUtil.FieldChange;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import static com.alin.lin.util.PolicyChangeFieldUtil.requireNotNull;
import static com.alin.lin.util.PolicyChangeFieldUtil.requireText;

@Service
public class PolicyChangeSupportServiceImpl implements PolicyChangeSupportService {
    private final PolicyChangeDao policyChangeDao;
    private final CodeDescriptionService codeDescriptionService;
    private final CurrentUserService currentUserService;
    private final ZoneId changeCaseZoneId;

    public PolicyChangeSupportServiceImpl(
            PolicyChangeDao policyChangeDao,
            CodeDescriptionService codeDescriptionService,
            CurrentUserService currentUserService,
            PosChangeProperties posChangeProperties
    ) {
        this.policyChangeDao = policyChangeDao;
        this.codeDescriptionService = codeDescriptionService;
        this.currentUserService = currentUserService;
        this.changeCaseZoneId = ZoneId.of(posChangeProperties.getZoneId());
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
    public void validateChangeCaseAccess(
            String policyNo,
            Integer policySeq,
            String changeCaseNo,
            String changeItem
    ) {
        requireText(policyNo, "policyNo");
        requireNotNull(policySeq, "policySeq");
        requireText(changeCaseNo, "changeCaseNo");
        requireText(changeItem, "changeItem");

        String username = currentUserService.username();
        PolicyChangeAcceptance acceptance = policyChangeDao.findAcceptanceForUpdate(policyNo, policySeq, changeCaseNo);
        if (acceptance != null) {
            requireAcceptanceOwner(acceptance, username);
            if (!codeDescriptionService.pendingStatusCode().equals(acceptance.getAcceptanceStatus())) {
                throw new ChangeCaseConflictException("只有 P-受理中的案件可以修改");
            }
            List<String> existingItems = policyChangeDao.findChangeItemsByCaseNo(policyNo, policySeq, changeCaseNo);
            if (existingItems.contains(changeItem)) {
                return;
            }
            requireReservedChangeItem(policyNo, policySeq, changeCaseNo, changeItem, username, false);
            return;
        }

        requireReservedChangeItem(policyNo, policySeq, changeCaseNo, changeItem, username, true);
    }

    private PolicyChangeCaseReservation requireReservedChangeItem(
            String policyNo,
            Integer policySeq,
            String changeCaseNo,
            String changeItem,
            String username,
            boolean requireNotExpired
    ) {
        PolicyChangeCaseReservation reservation = policyChangeDao.findCaseReservationForUpdate(changeCaseNo);
        if (reservation == null) {
            throw new NoSuchElementException("找不到有效的變更案號: " + changeCaseNo);
        }
        if (!Objects.equals(policyNo, reservation.getPolicyNo())
                || !Objects.equals(policySeq, reservation.getPolicySeq())) {
            throw new IllegalArgumentException("案號與保單不符");
        }
        if (!policyChangeDao.findReservedChangeItems(changeCaseNo).contains(changeItem)) {
            throw new IllegalArgumentException("此案號未選擇保全變更項目: " + changeItem);
        }
        if (requireNotExpired && !reservation.getExpiresAt().isAfter(LocalDateTime.now(changeCaseZoneId))) {
            throw new ChangeCaseConflictException("變更案號已逾期，請重新產生案號");
        }
        if (!Objects.equals(username, reservation.getReservedBy())) {
            throw new AccessDeniedException("此變更案號不是由目前帳號產生");
        }
        return reservation;
    }

    @Override
    public void ensureChangeCaseSaved(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        validateChangeCaseAccess(policyNo, policySeq, changeCaseNo, changeItem);
        if (policyChangeDao.existsChangeItem(policyNo, policySeq, changeCaseNo, changeItem) > 0) {
            return;
        }
        PolicyChangeAcceptance acceptance = policyChangeDao.findAcceptanceForUpdate(policyNo, policySeq, changeCaseNo);
        if (acceptance == null) {
            policyChangeDao.insertAcceptance(PolicyChangeAcceptance.builder()
                    .policyNo(policyNo)
                    .policySeq(policySeq)
                    .changeCaseNo(changeCaseNo)
                    .acceptanceStatus(codeDescriptionService.pendingStatusCode())
                    .createdBy(currentUserService.username())
                    .build());

            PolicyChangeCaseReservation reservation = policyChangeDao.findCaseReservationForUpdate(changeCaseNo);
            if (reservation.getConsumedAt() == null
                    && policyChangeDao.consumeCaseReservation(changeCaseNo, currentUserService.username()) != 1) {
                throw new ChangeCaseConflictException("變更案號已失效，請重新產生案號");
            }
        }
        policyChangeDao.insertChangeItem(PolicyChangeItem.builder()
                .policyNo(policyNo)
                .policySeq(policySeq)
                .changeCaseNo(changeCaseNo)
                .changeItem(changeItem)
                .build());
    }

    @Override
    public void upsertFieldChange(String policyNo, Integer policySeq, String changeCaseNo, String changeItem, FieldChange fieldChange) {
        policyChangeDao.upsertChangeField(
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

    @Override
    public void removeEmptyChangeItemAndAcceptance(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        int fieldCount = policyChangeDao.countChangeFieldsByItem(policyNo, policySeq, changeCaseNo, changeItem);
        int fileCount = policyChangeDao.countChangeFilesByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (fieldCount > 0 || fileCount > 0) {
            return;
        }

        policyChangeDao.deleteChangeItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (policyChangeDao.countChangeItemsByCaseNo(policyNo, policySeq, changeCaseNo) == 0) {
            policyChangeDao.deleteAcceptance(
                    policyNo,
                    policySeq,
                    changeCaseNo,
                    codeDescriptionService.pendingStatusCode()
            );
        }
    }

    private void requireAcceptanceOwner(PolicyChangeAcceptance acceptance, String username) {
        if (currentUserService.securityEnabled() && !Objects.equals(username, acceptance.getCreatedBy())) {
            throw new AccessDeniedException("只有原建檔經辦可以修改此案件");
        }
    }
}
