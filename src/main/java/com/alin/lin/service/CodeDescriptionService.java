package com.alin.lin.service;

import com.alin.lin.entity.CodeDescription;

import java.util.List;
import java.util.Map;

public interface CodeDescriptionService {
    List<CodeDescription> findAllCodes();
    List<CodeDescription> findAddressTypes();

    List<CodeDescription> findAcceptanceStatuses();

    List<CodeDescription> findChangeItems();

    List<CodeDescription> findScreenPermissions();

    List<CodeDescription> findUserAuthorizationPermissions();

    CodeDescription findPostalCodeZipCode3(String zipCode3);

    Map<String, String> findChtFieldNames();

    String communicationAddressCode();

    String registeredAddressCode();

    String emailAddressCode();

    String addressChangeItemCode();

    String mainAmountChangeItemCode();

    String riderAmountChangeItemCode();

    String pendingStatusCode();

    String processingStatusCode();

    String completeStatusCode();

    String cancelStatusCode();

    String mainRideTypeCode();
}
