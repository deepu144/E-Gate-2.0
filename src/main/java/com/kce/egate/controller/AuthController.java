package com.kce.egate.controller;

import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.request.PasswordChangeOTPRequest;
import com.kce.egate.request.VerifyOTPRequest;
import com.kce.egate.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

public interface AuthController {

    ResponseEntity<CommonResponse> userSignIn(AuthenticationRequest request , BindingResult result);
    ResponseEntity<CommonResponse> logout(HttpServletRequest request, HttpServletResponse response);
    ResponseEntity<CommonResponse> forgotPassword(String email);
    ResponseEntity<CommonResponse> verifyOtp(VerifyOTPRequest request , BindingResult result);
    ResponseEntity<CommonResponse> changePassword(String uniqueId , PasswordChangeOTPRequest request , BindingResult result);
    ResponseEntity<CommonResponse> beforeOAuth2(String role);
}
