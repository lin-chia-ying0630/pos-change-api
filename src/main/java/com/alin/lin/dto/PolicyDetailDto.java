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
    private MainPolicyMaster master;
    private MainPolicyAddress communicationAddress;
    private List<MainPolicyAddress> addressList;
    private List<MainPolicyRide> rideList;
    private List<CodeDescription> addressTypes;
    private List<CodeDescription> acceptanceStatuses;
    private List<CodeDescription> changeItems;
}
