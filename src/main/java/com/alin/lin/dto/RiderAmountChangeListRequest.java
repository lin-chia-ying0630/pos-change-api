package com.alin.lin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiderAmountChangeListRequest {
    private String policyNo;
    private Integer policySeq;
    private String changeCaseNo;
    private List<RideAmountChangeRequest> rides;
}
