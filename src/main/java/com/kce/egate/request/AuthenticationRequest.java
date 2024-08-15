package com.kce.egate.request;

import com.kce.egate.constant.Constant;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthenticationRequest {
    @NotNull(message = Constant.EMAIL_NOT_PROVIDED)
    @Size(min = 5)
    private String email;
    @NotNull(message = Constant.PASSWORD_NOT_PROVIDED)
    @Size(min = 8 , message = Constant.PASSWORD_SIZE_NOT_MATCH)
    private String password;
}
