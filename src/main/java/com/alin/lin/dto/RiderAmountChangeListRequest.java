package com.alin.lin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "rides 一次最多 100 筆")
    private List<RideAmountChangeRequest> rides;
}
