package com.kce.egate.request;

import com.kce.egate.constant.Constant;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeOTPRequest {
    @NotNull(message = Constant.EMAIL_NOT_PROVIDED)
    @Size(min = 5)
    private String email;
    @NotNull(message = Constant.PASSWORD_NOT_PROVIDED)
    @Size(min = 8)
    private String password;
}
