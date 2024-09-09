package com.kce.egate.controller;

import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.request.PasswordChangeOTPRequest;
import com.kce.egate.request.VerifyOTPRequest;
import com.kce.egate.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

public interface AuthController {

    @PostMapping("/login")
    ResponseEntity<CommonResponse> userSignIn(@RequestBody @Valid AuthenticationRequest request , BindingResult result);
    @GetMapping("/logout")
    ResponseEntity<CommonResponse> logout(HttpServletRequest request, HttpServletResponse response);
    @PostMapping("/pwd/forgot")
    ResponseEntity<CommonResponse> forgotPassword(@RequestParam String email);
    @PostMapping("/pwd/otp/verify")
    ResponseEntity<CommonResponse> verifyOtp(@RequestBody @Valid VerifyOTPRequest request , BindingResult result);
    @PostMapping("/pwd/change/{unique-id}")
    ResponseEntity<CommonResponse> changePassword(@PathVariable("unique-id") String uniqueId , @RequestBody @Valid PasswordChangeOTPRequest request , BindingResult result);
    @PostMapping("/before/oAuth2")
    ResponseEntity<CommonResponse> beforeOAuth2(@RequestParam String email,@RequestParam String role);
}
