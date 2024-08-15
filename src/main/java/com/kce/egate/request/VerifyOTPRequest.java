package com.kce.egate.request;

import com.kce.egate.constant.Constant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOTPRequest {
    @NotNull(message = Constant.EMAIL_NOT_PROVIDED)
    @Size(min = 5)
    private String email;
    @NotNull(message = Constant.OTP_NOT_PROVIDE)
    @Min(100000)
    private Integer otp;
}
