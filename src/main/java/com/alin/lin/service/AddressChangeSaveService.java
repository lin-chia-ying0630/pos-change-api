package com.alin.lin.service;

import com.alin.lin.dto.AddressChangeDto;
import com.alin.lin.dto.AddressChangeRequest;

public interface AddressChangeSaveService {
    AddressChangeDto saveAddressChange(String changeCaseNo, AddressChangeRequest request);
}
