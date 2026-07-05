package com.alin.lin.controller;

import com.alin.lin.dto.AddressChangeRequest;
import com.alin.lin.dto.AddressChangeDto;
import com.alin.lin.dto.CreateChangeCaseRequest;
import com.alin.lin.dto.CreateChangeCaseDto;
import com.alin.lin.dto.MainAmountChangeDto;
import com.alin.lin.dto.MainAmountChangeRequest;
import com.alin.lin.dto.PolicyChangeCaseDto;
import com.alin.lin.dto.PolicyDetailDto;
import com.alin.lin.dto.PostalCodeAreaDto;
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

    // 畫面對應：新增保全變更頁載入保單主檔、通訊地址、全部地址清單、主附約資料與變更項目。
    @GetMapping("/policies/{policyNo}/{policySeq}")
    public ResponseEntity<ResponseBodyDto<PolicyDetailDto>> findPolicyDetail(@PathVariable String policyNo, @PathVariable Integer policySeq) {
        return ResponseUtil.ok(policyChangeService.findPolicyDetail(policyNo, policySeq));
    }

    // 畫面對應：新增保全變更頁的地址變更 Dialog，輸入 3+3 郵遞區號後帶入全型/半形地址前綴。
    @GetMapping("/postal-codes/{postalCode}")
    public ResponseEntity<ResponseBodyDto<PostalCodeAreaDto>> findPostalCodeArea(@PathVariable String postalCode) {
        return ResponseUtil.ok(policyChangeService.findPostalCodeArea(postalCode));
    }

    // 畫面對應：新增保全變更頁的「產生案號」按鈕，只先取得 P-受理中案號，不立即寫受理檔。
    @PostMapping("/change-cases")
    public ResponseEntity<ResponseBodyDto<CreateChangeCaseDto>> createChangeCase(@RequestBody CreateChangeCaseRequest request) {
        return ResponseUtil.created(policyChangeService.createChangeCase(request));
    }

    // 畫面對應：新增保全變更頁的 001 地址變更 Dialog 儲存。
    @PostMapping("/change-cases/address-change")
    public ResponseEntity<ResponseBodyDto<AddressChangeDto>> saveAddressChange(@RequestBody AddressChangeRequest request) {
        return ResponseUtil.ok(policyChangeService.saveAddressChange(request));
    }

    // 畫面對應：新增保全變更頁的 002 主約保額變更 Dialog 儲存。
    @PostMapping("/change-cases/main-amount-change")
    public ResponseEntity<ResponseBodyDto<MainAmountChangeDto>> saveMainAmountChange(@RequestBody MainAmountChangeRequest request) {
        return ResponseUtil.ok(policyChangeService.saveMainAmountChange(request));
    }

    // 畫面對應：新增保全變更頁的 003 附約保額變更 Dialog 儲存。
    @PostMapping("/change-cases/rider-amount-change")
    public ResponseEntity<ResponseBodyDto<MainAmountChangeDto>> saveRiderAmountChange(@RequestBody RiderAmountChangeListRequest request) {
        return ResponseUtil.ok(policyChangeService.saveRiderAmountChange(request));
    }

    // 畫面對應：查詢保全變更頁與覆核頁，依保單號碼列出既有保全受理資料。
    @GetMapping("/policies/{policyNo}/change-cases")
    public ResponseEntity<ResponseBodyDto<List<PolicyChangeCaseDto>>> findChangeCases(@PathVariable String policyNo) {
        return ResponseUtil.ok(policyChangeService.findChangeCases(policyNo));
    }

    // 畫面對應：覆核頁將 P-受理中案件改為 S-完成或 C-取消，完成時才回寫主檔、地址或主附約。
    @PatchMapping("/change-cases/{changeCaseNo}/status")
    public ResponseEntity<ResponseBodyDto<UpdateChangeCaseStatusDto>> updateChangeCaseStatus(
            @PathVariable String changeCaseNo,
            @RequestBody UpdateChangeCaseStatusRequest request
    ) {
        return ResponseUtil.ok(policyChangeService.updateChangeCaseStatus(changeCaseNo, request));
    }
}
