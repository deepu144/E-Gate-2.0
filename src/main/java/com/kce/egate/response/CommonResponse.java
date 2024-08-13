package com.kce.egate.response;

import com.kce.egate.enumeration.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public final class CommonResponse {
    private ResponseStatus status;
    private String errorMessage;
    private String successMessage;
    private Object data;
    private int code;
}
