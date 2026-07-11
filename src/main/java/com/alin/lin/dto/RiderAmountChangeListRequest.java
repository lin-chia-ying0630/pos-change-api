package com.alin.lin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
    // 附約保額變更清單
    @Valid
    @NotEmpty(message = "rides 不可空白")
    private List<RideAmountChangeRequest> rides;
}
