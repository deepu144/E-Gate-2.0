package com.kce.egate.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PasswordChangeRequest {
    private String email;
    private String oldPassword;
    private String newPassword;
}
