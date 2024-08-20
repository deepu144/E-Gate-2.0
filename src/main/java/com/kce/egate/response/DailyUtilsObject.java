package com.kce.egate.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyUtilsObject {
    private Long studentInCount;
    private Long studentOutCount;
    private Long staffInCount;
    private Long staffOutCount;
}
