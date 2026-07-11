package com.alin.lin.dto;

import com.alin.lin.entity.CodeDescription;
import com.alin.lin.entity.MainPolicyAddress;
import com.alin.lin.entity.MainPolicyMaster;
import com.alin.lin.entity.MainPolicyRide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDetailDto {
    // 保單主檔
    private MainPolicyMaster master;

    // 通訊地址
    private MainPolicyAddress communicationAddress;

    // 保單地址清單
    private List<MainPolicyAddress> addressList;

    // 保單主附約清單
    private List<MainPolicyRide> rideList;

    // 地址型態代碼清單
    private List<CodeDescription> addressTypes;

    // 受理狀態代碼清單
    private List<CodeDescription> acceptanceStatuses;

    // 保全變更項目代碼清單
    private List<CodeDescription> changeItems;
}
