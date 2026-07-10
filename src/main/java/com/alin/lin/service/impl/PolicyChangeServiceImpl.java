package com.alin.lin.service.impl;

import com.alin.lin.dao.PolicyChangeDao;
import com.alin.lin.dto.AddressChangeDto;
import com.alin.lin.dto.AddressChangeRequest;
import com.alin.lin.dto.CreateChangeCaseDto;
import com.alin.lin.dto.CreateChangeCaseRequest;
import com.alin.lin.dto.MainAmountChangeDto;
import com.alin.lin.dto.MainAmountChangeRequest;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.dto.PolicyDetailDto;
import com.alin.lin.dto.PostalCodeAreaDto;
import com.alin.lin.dto.RideAmountChangeRequest;
import com.alin.lin.dto.RiderAmountChangeListRequest;
import com.alin.lin.dto.UpdateChangeCaseStatusDto;
import com.alin.lin.dto.UpdateChangeCaseStatusRequest;
import com.alin.lin.entity.CodeDescription;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import com.alin.lin.entity.PolicyChangeAcceptance;
import com.alin.lin.entity.PolicyChangeField;
import com.alin.lin.entity.PolicyChangeFile;
import com.alin.lin.entity.PolicyChangeItem;
import com.alin.lin.service.PolicyChangeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@Service
public class PolicyChangeServiceImpl implements PolicyChangeService {
    private static final String COMMUNICATION_ADDRESS = "01";
    private static final String ADDRESS_CHANGE_ITEM = "001";
    private static final String MAIN_AMOUNT_CHANGE_ITEM = "002";
    private static final String RIDER_AMOUNT_CHANGE_ITEM = "003";
    private static final String POSTAL_CODE_GROUP = "postal-code";
    private static final String ZIP_CODE3_FIELD = "zip_code3";
    private static final String MASTER_AMOUNT_FIELD = "main_policy_master.insured_amount";
    private static final String MAIN_RIDE_ORDER = "000";
    private static final String MAIN_RIDE_TYPE = "1";
    private static final String RIDE_AMOUNT_FIELD_PREFIX = "main_policy_ride.";
    private static final String RIDE_AMOUNT_FIELD_SUFFIX = ".insured_amount";
    private static final String RIDE_PREMIUM_FIELD_SUFFIX = ".premium";
    private static final String ACCEPTANCE_COMPLETE = "S";
    private static final String ACCEPTANCE_CANCEL = "C";
    private static final String ACCEPTANCE_PENDING = "P";
    private static final String ZIP_CODE_PATTERN = "\\d{3}";
    private static final Set<String> MASTER_CHANGE_FIELDS = Set.of(
            "main_product_code",
            "main_policy_years",
            "insured_amount"
    );
    private static final int SERIAL_MAX = 999;
    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MMdd");

    private final PolicyChangeDao policyChangeDao;
    private final ObjectMapper objectMapper;
    private String currentCaseNoPrefix;
    private int currentCaseNoSerial;

    public PolicyChangeServiceImpl(PolicyChangeDao policyChangeDao, ObjectMapper objectMapper) {
        this.policyChangeDao = policyChangeDao;
        this.objectMapper = objectMapper;
    }

    @Override
    public PolicyDetailDto findPolicyDetail(String policyNo, Integer policySeq) {
        MainPolicyMaster master = requirePolicy(policyNo, policySeq);
        List<MainPolicyAddress> addressList = policyChangeDao.findAddresses(policyNo, policySeq);
        List<MainPolicyRide> rideList = policyChangeDao.findRides(policyNo, policySeq);
        MainPolicyAddress communicationAddress = addressList.stream()
                .filter(address -> COMMUNICATION_ADDRESS.equals(address.getAddressType()))
                .findFirst()
                .orElse(null);
        return PolicyDetailDto.builder()
                .master(master)
                .communicationAddress(communicationAddress)
                .addressList(addressList)
                .rideList(rideList)
                .addressTypes(policyChangeDao.findCodes("main-policy-address", "address_type"))
                .acceptanceStatuses(policyChangeDao.findCodes("policy-change-acceptance", "acceptance_status"))
                .changeItems(policyChangeDao.findCodes("policy-change-item", "change_item"))
                .build();
    }

    @Override
    public PostalCodeAreaDto findPostalCodeArea(String postalCode) {
        requireText(postalCode, "postalCode");
        String normalizedPostalCode = postalCode.trim();
        if (!normalizedPostalCode.matches("\\d{3}(\\d{3})?")) {
            throw new IllegalArgumentException("郵遞區號前三碼必填，後三碼可空白；若填寫需為 3 碼");
        }

        String zipCode3 = normalizedPostalCode.substring(0, 3);
        CodeDescription code = policyChangeDao.findCode(POSTAL_CODE_GROUP, ZIP_CODE3_FIELD, zipCode3);
        if (code == null) {
            throw new NoSuchElementException("找不到郵遞區號前三碼: " + zipCode3);
        }

        String[] cityAndDistrict = parseCityAndDistrict(code);
        return PostalCodeAreaDto.builder()
                .postalCode(normalizedPostalCode)
                .zipCode3(zipCode3)
                .city(cityAndDistrict[0])
                .district(cityAndDistrict[1])
                .addressPrefix(String.join("", cityAndDistrict))
                .halfWidthAddressPrefix(code.getCodeDescription())
                .build();
    }

    @Override
    public CreateChangeCaseDto createChangeCase(CreateChangeCaseRequest request) {
        requirePolicy(request.getPolicyNo(), request.getPolicySeq());
        requireText(request.getChangeItem(), "changeItem");

        String changeCaseNo = generateChangeCaseNo();

        return CreateChangeCaseDto.builder()
                .policyNo(request.getPolicyNo())
                .policySeq(request.getPolicySeq())
                .changeCaseNo(changeCaseNo)
                .acceptanceStatus(ACCEPTANCE_PENDING)
                .changeItem(request.getChangeItem())
                .build();
    }

    @Override
    @Transactional
    public AddressChangeDto saveAddressChange(String changeCaseNo, AddressChangeRequest request) {
        String addressType = request.getAddressType() == null || request.getAddressType().isBlank()
                ? COMMUNICATION_ADDRESS
                : request.getAddressType();

        requirePolicy(request.getPolicyNo(), request.getPolicySeq());
        requireText(changeCaseNo, "changeCaseNo");

        MainPolicyAddress beforeAddress = policyChangeDao.findAddress(request.getPolicyNo(), request.getPolicySeq(), addressType);
        if (beforeAddress == null) {
            throw new NoSuchElementException("找不到地址資料: " + request.getPolicyNo() + "-" + request.getPolicySeq() + "-" + addressType);
        }

        String zipCode3 = normalizeBlank(request.getZipCode3());
        String zipCode2 = normalizeBlank(request.getZipCode2());
        validateAddressPostalCode(zipCode3, zipCode2);

        MainPolicyAddress afterAddress = MainPolicyAddress.builder()
                .policyNo(request.getPolicyNo())
                .policySeq(request.getPolicySeq())
                .addressType(addressType)
                .zipCode3(zipCode3)
                .zipCode2(zipCode2)
                .fullWidthAddress(normalizeBlank(request.getFullWidthAddress()))
                .halfWidthAddress(normalizeBlank(request.getHalfWidthAddress()))
                .build();

        List<FieldChange> fieldChanges = collectFieldChanges(beforeAddress, afterAddress);
        if (fieldChanges.isEmpty()) {
            return AddressChangeDto.builder()
                    .changeCaseNo(changeCaseNo)
                    .changeItem(ADDRESS_CHANGE_ITEM)
                    .changedFieldCount(0)
                    .build();
        }

        ensureChangeCaseSaved(request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, ADDRESS_CHANGE_ITEM);
        fieldChanges.forEach(fieldChange -> insertFieldChange(request, changeCaseNo, fieldChange));

        policyChangeDao.insertChangeFile(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                ADDRESS_CHANGE_ITEM,
                "main_policy_address",
                toJson(addressSnapshot(beforeAddress)),
                toJson(addressSnapshot(afterAddress))
        );

        return AddressChangeDto.builder()
                .changeCaseNo(changeCaseNo)
                .changeItem(ADDRESS_CHANGE_ITEM)
                .changedFieldCount(fieldChanges.size())
                .build();
    }

    @Override
    @Transactional
    public MainAmountChangeDto saveMainAmountChange(String changeCaseNo, MainAmountChangeRequest request) {
        MainPolicyMaster master = requirePolicy(request.getPolicyNo(), request.getPolicySeq());
        requireText(changeCaseNo, "changeCaseNo");
        if (request.getMasterInsuredAmount() == null) {
            throw new IllegalArgumentException("masterInsuredAmount 不可空白");
        }

        List<FieldChange> fieldChanges = new ArrayList<>();
        addAmountChangeIfDifferent(
                fieldChanges,
                MASTER_AMOUNT_FIELD,
                "MASTER",
                master.getInsuredAmount(),
                request.getMasterInsuredAmount()
        );
        MainPolicyRide mainRide = findMainRide(request.getPolicyNo(), request.getPolicySeq());
        addAmountChangeIfDifferent(
                fieldChanges,
                RIDE_AMOUNT_FIELD_PREFIX + MAIN_RIDE_ORDER + RIDE_AMOUNT_FIELD_SUFFIX,
                MAIN_RIDE_ORDER,
                mainRide.getInsuredAmount(),
                request.getMasterInsuredAmount()
        );

        if (fieldChanges.isEmpty()) {
            return MainAmountChangeDto.builder()
                    .changeCaseNo(changeCaseNo)
                    .changeItem(MAIN_AMOUNT_CHANGE_ITEM)
                    .changedFieldCount(0)
                    .build();
        }

        ensureChangeCaseSaved(request.getPolicyNo(), request.getPolicySeq(), changeCaseNo, MAIN_AMOUNT_CHANGE_ITEM);
        fieldChanges.forEach(fieldChange -> insertFieldChange(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                MAIN_AMOUNT_CHANGE_ITEM,
                fieldChange
        ));

        return MainAmountChangeDto.builder()
                .changeCaseNo(changeCaseNo)
                .changeItem(MAIN_AMOUNT_CHANGE_ITEM)
                .changedFieldCount(fieldChanges.size())
                .build();
    }

    @Override
    @Transactional
    public MainAmountChangeDto saveRiderAmountChange(String changeCaseNo, String policyNo, Integer policySeq, RiderAmountChangeListRequest request) {
        requirePolicy(policyNo, policySeq);
        requireText(changeCaseNo, "changeCaseNo");
        if (request.getRides() == null || request.getRides().isEmpty()) {
            throw new IllegalArgumentException("rides 不可空白");
        }

        List<MainPolicyRide> rides = policyChangeDao.findRides(policyNo, policySeq);
        Map<String, MainPolicyRide> rideMap = new LinkedHashMap<>();
        rides.forEach(ride -> rideMap.put(ride.getRideOrder(), ride));

        List<FieldChange> fieldChanges = new ArrayList<>();
        for (RideAmountChangeRequest changedRide : request.getRides()) {
            requireText(changedRide.getRideOrder(), "rideOrder");
            if (changedRide.getInsuredAmount() == null) {
                throw new IllegalArgumentException("ride insuredAmount 不可空白");
            }
            MainPolicyRide beforeRide = rideMap.get(changedRide.getRideOrder());
            if (beforeRide == null) {
                throw new NoSuchElementException("找不到附約: " + changedRide.getRideOrder());
            }
            if (MAIN_RIDE_TYPE.equals(beforeRide.getRideType())) {
                throw new IllegalArgumentException("003 附約保額變更不可修改主約");
            }
            addAmountChangeIfDifferent(
                    fieldChanges,
                    RIDE_AMOUNT_FIELD_PREFIX + changedRide.getRideOrder() + RIDE_AMOUNT_FIELD_SUFFIX,
                    changedRide.getRideOrder(),
                    beforeRide.getInsuredAmount(),
                    changedRide.getInsuredAmount()
            );
        }

        if (fieldChanges.isEmpty()) {
            return MainAmountChangeDto.builder()
                    .changeCaseNo(changeCaseNo)
                    .changeItem(RIDER_AMOUNT_CHANGE_ITEM)
                    .changedFieldCount(0)
                    .build();
        }

        ensureChangeCaseSaved(policyNo, policySeq, changeCaseNo, RIDER_AMOUNT_CHANGE_ITEM);
        fieldChanges.forEach(fieldChange -> insertFieldChange(
                policyNo,
                policySeq,
                changeCaseNo,
                RIDER_AMOUNT_CHANGE_ITEM,
                fieldChange
        ));

        return MainAmountChangeDto.builder()
                .changeCaseNo(changeCaseNo)
                .changeItem(RIDER_AMOUNT_CHANGE_ITEM)
                .changedFieldCount(fieldChanges.size())
                .build();
    }

    @Override
    public List<PolicyChangeCaseDto> findChangeCases(String policyNo) {
        requireText(policyNo, "policyNo");
        return policyChangeDao.findChangeCases(policyNo);
    }

    @Override
    @Transactional
    public UpdateChangeCaseStatusDto updateChangeCaseStatus(String changeCaseNo, UpdateChangeCaseStatusRequest request) {
        requireText(changeCaseNo, "changeCaseNo");
        requireText(request.getPolicyNo(), "policyNo");
        if (request.getPolicySeq() == null) {
            throw new IllegalArgumentException("policySeq 不可空白");
        }

        String acceptanceStatus = normalizeStatus(request.getAcceptanceStatus());
        if (!ACCEPTANCE_CANCEL.equals(acceptanceStatus) && !ACCEPTANCE_COMPLETE.equals(acceptanceStatus)) {
            throw new IllegalArgumentException("受理狀態只能改為 C 或 S");
        }

        PolicyChangeCaseDto changeCase = policyChangeDao.findChangeCase(request.getPolicyNo(), request.getPolicySeq(), changeCaseNo);
        if (changeCase == null) {
            throw new NoSuchElementException("找不到保全受理資料: " + changeCaseNo);
        }
        if (!ACCEPTANCE_PENDING.equals(normalizeStatus(changeCase.getAcceptanceStatus()))) {
            throw new IllegalStateException("只有 P-受理中可以改為 C 或 S");
        }

        int appliedItemCount = 0;
        if (ACCEPTANCE_COMPLETE.equals(acceptanceStatus)) {
            appliedItemCount = applyChangeCase(request.getPolicyNo(), request.getPolicySeq(), changeCaseNo);
        }

        policyChangeDao.updateAcceptanceStatus(PolicyChangeAcceptance.builder()
                .policyNo(request.getPolicyNo())
                .policySeq(request.getPolicySeq())
                .changeCaseNo(changeCaseNo)
                .acceptanceStatus(acceptanceStatus)
                .build());
        return UpdateChangeCaseStatusDto.builder()
                .policyNo(request.getPolicyNo())
                .policySeq(request.getPolicySeq())
                .changeCaseNo(changeCaseNo)
                .acceptanceStatus(acceptanceStatus)
                .appliedItemCount(appliedItemCount)
                .build();
    }

    private int applyChangeCase(String policyNo, Integer policySeq, String changeCaseNo) {
        List<String> changeItems = policyChangeDao.findChangeItemsByCaseNo(policyNo, policySeq, changeCaseNo);
        int appliedItemCount = 0;
        for (String changeItem : changeItems) {
            boolean premiumTotalShouldRefresh = false;
            if (ADDRESS_CHANGE_ITEM.equals(changeItem)) {
                applyAddressChanges(policyNo, policySeq, changeCaseNo, changeItem);
                appliedItemCount++;
                continue;
            }
            if (MAIN_AMOUNT_CHANGE_ITEM.equals(changeItem)) {
                premiumTotalShouldRefresh = applyMainAmountChanges(policyNo, policySeq, changeCaseNo, changeItem);
                appliedItemCount++;
                if (premiumTotalShouldRefresh) {
                    policyChangeDao.updateMasterTotalPremiumFromRides(policyNo, policySeq);
                }
                continue;
            }
            if (RIDER_AMOUNT_CHANGE_ITEM.equals(changeItem)) {
                premiumTotalShouldRefresh = applyRiderAmountChanges(policyNo, policySeq, changeCaseNo, changeItem);
                appliedItemCount++;
                if (premiumTotalShouldRefresh) {
                    policyChangeDao.updateMasterTotalPremiumFromRides(policyNo, policySeq);
                }
                continue;
            }
            applyMasterChanges(policyNo, policySeq, changeCaseNo, changeItem);
            appliedItemCount++;
        }
        return appliedItemCount;
    }

    private void applyAddressChanges(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        List<PolicyChangeFile> changeFiles = policyChangeDao.findChangeFilesByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (changeFiles.isEmpty()) {
            throw new IllegalStateException("找不到地址變更檔案: " + changeCaseNo);
        }
        for (PolicyChangeFile changeFile : changeFiles) {
            MainPolicyAddress address = readAddress(changeFile.getContentAfter());
            policyChangeDao.updateAddress(address);
        }
    }

    private void applyMasterChanges(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        List<PolicyChangeField> changeFields = policyChangeDao.findChangeFieldsByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (changeFields.isEmpty()) {
            throw new IllegalStateException("找不到主檔變更欄位: " + changeCaseNo);
        }
        for (PolicyChangeField changeField : changeFields) {
            if (!MASTER_CHANGE_FIELDS.contains(changeField.getChangeField())) {
                throw new IllegalArgumentException("不支援回寫主檔欄位: " + changeField.getChangeField());
            }
            policyChangeDao.updateMasterField(policyNo, policySeq, changeField.getChangeField(), changeField.getContentAfter());
        }
    }

    private boolean applyMainAmountChanges(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        List<PolicyChangeField> changeFields = policyChangeDao.findChangeFieldsByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (changeFields.isEmpty()) {
            throw new IllegalStateException("找不到主保額變更欄位: " + changeCaseNo);
        }
        boolean premiumChanged = false;
        for (PolicyChangeField changeField : changeFields) {
            if (MASTER_AMOUNT_FIELD.equals(changeField.getChangeField())) {
                policyChangeDao.updateMasterField(policyNo, policySeq, "insured_amount", changeField.getContentAfter());
                continue;
            }
            if (changeField.getChangeField().startsWith(RIDE_AMOUNT_FIELD_PREFIX)
                    && changeField.getChangeField().endsWith(RIDE_AMOUNT_FIELD_SUFFIX)) {
                String rideOrder = resolveRideOrder(changeField);
                if (!MAIN_RIDE_ORDER.equals(rideOrder)) {
                    throw new IllegalArgumentException("002 主約保額變更不可回寫附約: " + rideOrder);
                }
                policyChangeDao.updateRideAmount(policyNo, policySeq, rideOrder, changeField.getContentAfter());
                continue;
            }
            if (changeField.getChangeField().startsWith(RIDE_AMOUNT_FIELD_PREFIX)
                    && changeField.getChangeField().endsWith(RIDE_PREMIUM_FIELD_SUFFIX)) {
                String rideOrder = resolveRideOrder(changeField, RIDE_PREMIUM_FIELD_SUFFIX);
                if (!MAIN_RIDE_ORDER.equals(rideOrder)) {
                    throw new IllegalArgumentException("002 主約變更不可回寫附約保費: " + rideOrder);
                }
                policyChangeDao.updateRidePremium(policyNo, policySeq, rideOrder, changeField.getContentAfter());
                premiumChanged = true;
                continue;
            }
            throw new IllegalArgumentException("不支援回寫主約保額欄位: " + changeField.getChangeField());
        }
        return premiumChanged;
    }

    private boolean applyRiderAmountChanges(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
        List<PolicyChangeField> changeFields = policyChangeDao.findChangeFieldsByItem(policyNo, policySeq, changeCaseNo, changeItem);
        if (changeFields.isEmpty()) {
            throw new IllegalStateException("找不到附約保額變更欄位: " + changeCaseNo);
        }
        boolean premiumChanged = false;
        for (PolicyChangeField changeField : changeFields) {
            if (changeField.getChangeField().startsWith(RIDE_AMOUNT_FIELD_PREFIX)
                    && changeField.getChangeField().endsWith(RIDE_AMOUNT_FIELD_SUFFIX)) {
                String rideOrder = resolveRideOrder(changeField);
                if (MAIN_RIDE_ORDER.equals(rideOrder)) {
                    throw new IllegalArgumentException("003 附約保額變更不可回寫主約");
                }
                policyChangeDao.updateRideAmount(policyNo, policySeq, rideOrder, changeField.getContentAfter());
                continue;
            }
            if (changeField.getChangeField().startsWith(RIDE_AMOUNT_FIELD_PREFIX)
                    && changeField.getChangeField().endsWith(RIDE_PREMIUM_FIELD_SUFFIX)) {
                String rideOrder = resolveRideOrder(changeField, RIDE_PREMIUM_FIELD_SUFFIX);
                if (MAIN_RIDE_ORDER.equals(rideOrder)) {
                    throw new IllegalArgumentException("003 附約變更不可回寫主約保費");
                }
                policyChangeDao.updateRidePremium(policyNo, policySeq, rideOrder, changeField.getContentAfter());
                premiumChanged = true;
                continue;
            }
            throw new IllegalArgumentException("不支援回寫附約保額欄位: " + changeField.getChangeField());
        }
        return premiumChanged;
    }

    private String normalizeStatus(String acceptanceStatus) {
        requireText(acceptanceStatus, "acceptanceStatus");
        return acceptanceStatus.trim().toUpperCase();
    }

    private void ensureChangeCaseSaved(String policyNo, Integer policySeq, String changeCaseNo, String changeItem) {
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
                .acceptanceStatus(ACCEPTANCE_PENDING)
                .build());
        policyChangeDao.insertChangeItem(PolicyChangeItem.builder()
                .policyNo(policyNo)
                .policySeq(policySeq)
                .changeCaseNo(changeCaseNo)
                .changeItem(changeItem)
                .build());
    }

    private MainPolicyMaster requirePolicy(String policyNo, Integer policySeq) {
        requireText(policyNo, "policyNo");
        if (policySeq == null) {
            throw new IllegalArgumentException("policySeq 不可空白");
        }
        MainPolicyMaster master = policyChangeDao.findMaster(policyNo, policySeq);
        if (master == null) {
            throw new NoSuchElementException("找不到保單: " + policyNo + "-" + policySeq);
        }
        return master;
    }

    private MainPolicyRide findMainRide(String policyNo, Integer policySeq) {
        return policyChangeDao.findRides(policyNo, policySeq).stream()
                .filter(ride -> MAIN_RIDE_TYPE.equals(ride.getRideType()) || MAIN_RIDE_ORDER.equals(ride.getRideOrder()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("找不到主約資料: " + policyNo + "-" + policySeq));
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不可空白");
        }
    }

    private String[] parseCityAndDistrict(CodeDescription code) {
        String source = code.getCodeAfter() == null || code.getCodeAfter().isBlank()
                ? code.getCodeDescription()
                : code.getCodeAfter();
        String[] parts = source.split("\\|", 2);
        if (parts.length == 2) {
            return parts;
        }
        String description = code.getCodeDescription();
        if (description.length() < 4) {
            return new String[]{description, ""};
        }
        return new String[]{description.substring(0, 3), description.substring(3)};
    }

    private List<FieldChange> collectFieldChanges(MainPolicyAddress beforeAddress, MainPolicyAddress afterAddress) {
        List<FieldChange> fieldChanges = new ArrayList<>();
        addFieldChangeIfDifferent(fieldChanges, "zip_code3", beforeAddress.getAddressType(), beforeAddress.getZipCode3(), afterAddress.getZipCode3());
        addFieldChangeIfDifferent(fieldChanges, "zip_code2", beforeAddress.getAddressType(), beforeAddress.getZipCode2(), afterAddress.getZipCode2());
        addFieldChangeIfDifferent(fieldChanges, "full_width_address", beforeAddress.getAddressType(), beforeAddress.getFullWidthAddress(), afterAddress.getFullWidthAddress());
        addFieldChangeIfDifferent(fieldChanges, "half_width_address", beforeAddress.getAddressType(), beforeAddress.getHalfWidthAddress(), afterAddress.getHalfWidthAddress());
        return fieldChanges;
    }

    private void addFieldChangeIfDifferent(List<FieldChange> fieldChanges, String field, String key, String beforeValue, String afterValue) {
        String normalizedBefore = normalizeBlank(beforeValue);
        String normalizedAfter = normalizeBlank(afterValue);
        if (!Objects.equals(normalizedBefore, normalizedAfter)) {
            fieldChanges.add(new FieldChange(field, key, normalizedBefore, normalizedAfter));
        }
    }

    private void addAmountChangeIfDifferent(List<FieldChange> fieldChanges, String field, String key, BigDecimal beforeValue, BigDecimal afterValue) {
        if (beforeValue == null || afterValue == null || beforeValue.compareTo(afterValue) != 0) {
            fieldChanges.add(new FieldChange(field, key, amountToString(beforeValue), amountToString(afterValue)));
        }
    }

    private String amountToString(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void validateAddressPostalCode(String zipCode3, String zipCode2) {
        if (zipCode3 == null || !zipCode3.matches(ZIP_CODE_PATTERN) || (zipCode2 != null && !zipCode2.matches(ZIP_CODE_PATTERN))) {
            throw new IllegalArgumentException("郵遞區號前三碼必填，後三碼可空白；若填寫需為 3 碼");
        }
        if (policyChangeDao.findCode(POSTAL_CODE_GROUP, ZIP_CODE3_FIELD, zipCode3) == null) {
            throw new NoSuchElementException("找不到郵遞區號前三碼: " + zipCode3);
        }
    }

    private void insertFieldChange(AddressChangeRequest request, String changeCaseNo, FieldChange fieldChange) {
        insertFieldChange(
                request.getPolicyNo(),
                request.getPolicySeq(),
                changeCaseNo,
                ADDRESS_CHANGE_ITEM,
                fieldChange
        );
    }

    private void insertFieldChange(String policyNo, Integer policySeq, String changeCaseNo, String changeItem, FieldChange fieldChange) {
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

    private String resolveRideOrder(PolicyChangeField changeField) {
        return resolveRideOrder(changeField, RIDE_AMOUNT_FIELD_SUFFIX);
    }

    private String resolveRideOrder(PolicyChangeField changeField, String fieldSuffix) {
        if (changeField.getChangeKey() != null && !changeField.getChangeKey().isBlank()) {
            return changeField.getChangeKey();
        }
        return changeField.getChangeField()
                .substring(RIDE_AMOUNT_FIELD_PREFIX.length(), changeField.getChangeField().length() - fieldSuffix.length());
    }

    private Map<String, Object> addressSnapshot(MainPolicyAddress address) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("policyNo", address.getPolicyNo());
        snapshot.put("policySeq", address.getPolicySeq());
        snapshot.put("addressType", address.getAddressType());
        snapshot.put("zipCode3", address.getZipCode3());
        snapshot.put("zipCode2", address.getZipCode2());
        snapshot.put("fullWidthAddress", address.getFullWidthAddress());
        snapshot.put("halfWidthAddress", address.getHalfWidthAddress());
        return snapshot;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 轉換失敗", e);
        }
    }

    private MainPolicyAddress readAddress(String contentAfter) {
        try {
            MainPolicyAddress address = objectMapper.readValue(contentAfter, MainPolicyAddress.class);
            address.setZipCode3(normalizeBlank(address.getZipCode3()));
            address.setZipCode2(normalizeBlank(address.getZipCode2()));
            address.setFullWidthAddress(normalizeBlank(address.getFullWidthAddress()));
            address.setHalfWidthAddress(normalizeBlank(address.getHalfWidthAddress()));
            return address;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("地址變更內容轉換失敗", e);
        }
    }

    private synchronized String generateChangeCaseNo() {
        String prefix = buildTodayCaseNoPrefix();
        if (!prefix.equals(currentCaseNoPrefix)) {
            currentCaseNoPrefix = prefix;
            currentCaseNoSerial = findLatestSerial(prefix);
        }

        if (currentCaseNoSerial >= SERIAL_MAX) {
            throw new IllegalStateException("今日變更案號流水號已達上限");
        }

        currentCaseNoSerial++;
        return prefix + String.format("%03d", currentCaseNoSerial);
    }

    private String buildTodayCaseNoPrefix() {
        LocalDate today = LocalDate.now(TAIPEI_ZONE);
        int rocYear = today.getYear() - 1911;
        return "C" + String.format("%03d", rocYear) + today.format(MONTH_DAY_FORMATTER);
    }

    private int findLatestSerial(String prefix) {
        String maxChangeCaseNo = policyChangeDao.findMaxChangeCaseNoByPrefix(prefix);
        if (maxChangeCaseNo == null || maxChangeCaseNo.length() <= prefix.length()) {
            return 0;
        }
        return Integer.parseInt(maxChangeCaseNo.substring(prefix.length()));
    }

    private record FieldChange(String field, String key, String beforeValue, String afterValue) {
    }
}
