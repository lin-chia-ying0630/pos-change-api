package com.alin.lin.controller;

import com.alin.lin.dto.AddressChangeRequest;
import com.alin.lin.dto.AddressChangeDto;
import com.alin.lin.dto.CreateChangeCaseRequest;
import com.alin.lin.dto.CreateChangeCaseDto;
import com.alin.lin.dto.MainAmountChangeDto;
import com.alin.lin.dto.MainAmountChangeRequest;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.dto.PolicyDetailDto;
import com.alin.lin.dto.ResponseBodyDto;
import com.alin.lin.dto.RiderAmountChangeListRequest;
import com.alin.lin.dto.UpdateChangeCaseStatusDto;
import com.alin.lin.dto.UpdateChangeCaseStatusRequest;
import com.alin.lin.service.PolicyChangeService;
import com.alin.lin.util.ResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://127.0.0.1:5173",
        "http://127.0.0.1:5174"
})
@RestController
@RequestMapping("/api")
public class PolicyChangeController {
    private final PolicyChangeService policyChangeService;

    public PolicyChangeController(PolicyChangeService policyChangeService) {
        this.policyChangeService = policyChangeService;
    }

    @GetMapping("/policies/{policyNo}/{policySeq}")
    public ResponseEntity<ResponseBodyDto<PolicyDetailDto>> findPolicyDetail(@PathVariable String policyNo, @PathVariable Integer policySeq) {
        return ResponseUtil.ok(policyChangeService.findPolicyDetail(policyNo, policySeq));
    }

    @PostMapping("/change-cases")
    public ResponseEntity<ResponseBodyDto<CreateChangeCaseDto>> createChangeCase(@RequestBody CreateChangeCaseRequest request) {
        return ResponseUtil.created(policyChangeService.createChangeCase(request));
    }

    @PostMapping("/change-cases/address-change")
    public ResponseEntity<ResponseBodyDto<AddressChangeDto>> saveAddressChange(@RequestBody AddressChangeRequest request) {
        return ResponseUtil.ok(policyChangeService.saveAddressChange(request));
    }

    @PostMapping("/change-cases/main-amount-change")
    public ResponseEntity<ResponseBodyDto<MainAmountChangeDto>> saveMainAmountChange(@RequestBody MainAmountChangeRequest request) {
        return ResponseUtil.ok(policyChangeService.saveMainAmountChange(request));
    }

    @PostMapping("/change-cases/rider-amount-change")
    public ResponseEntity<ResponseBodyDto<MainAmountChangeDto>> saveRiderAmountChange(@RequestBody RiderAmountChangeListRequest request) {
        return ResponseUtil.ok(policyChangeService.saveRiderAmountChange(request));
    }

    @GetMapping("/policies/{policyNo}/change-cases")
    public ResponseEntity<ResponseBodyDto<List<PolicyChangeCaseDto>>> findChangeCases(@PathVariable String policyNo) {
        return ResponseUtil.ok(policyChangeService.findChangeCases(policyNo));
    }

    @PatchMapping("/change-cases/{changeCaseNo}/status")
    public ResponseEntity<ResponseBodyDto<UpdateChangeCaseStatusDto>> updateChangeCaseStatus(
            @PathVariable String changeCaseNo,
            @RequestBody UpdateChangeCaseStatusRequest request
    ) {
        return ResponseUtil.ok(policyChangeService.updateChangeCaseStatus(changeCaseNo, request));
    }
}
