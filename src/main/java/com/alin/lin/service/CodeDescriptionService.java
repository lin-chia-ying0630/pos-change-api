package com.alin.lin.service;

import com.alin.lin.entity.CodeDescription;

import java.util.List;

public interface CodeDescriptionService {
    List<CodeDescription> findAddressTypes();

    List<CodeDescription> findAcceptanceStatuses();

    List<CodeDescription> findChangeItems();

    CodeDescription findPostalCodeZipCode3(String zipCode3);

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
