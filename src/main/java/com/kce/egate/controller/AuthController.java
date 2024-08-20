package com.kce.egate.controller;

import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.request.PasswordChangeOTPRequest;
import com.kce.egate.request.VerifyOTPRequest;
import com.kce.egate.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

public interface AuthController {

    @PostMapping("/login")
    ResponseEntity<CommonResponse> userSignIn(@RequestBody @Valid AuthenticationRequest request , BindingResult result);
    @GetMapping("/logout")
    ResponseEntity<CommonResponse> logout(HttpServletRequest request);
    @GetMapping("/oauth2/callback")
    ResponseEntity<CommonResponse> oauth2Callback(@RequestParam("email") String email,
                                                  @RequestParam("name") String name,
                                                  @RequestParam("picture") String picture,
                                                  @RequestParam("id") String id
    );
    @PostMapping("/pwd/forgot")
    ResponseEntity<CommonResponse> forgotPassword(@RequestParam String email);
    @PostMapping("/pwd/otp/verify")
    ResponseEntity<CommonResponse> verifyOtp(@RequestBody @Valid VerifyOTPRequest request , BindingResult result);
    @PostMapping("/pwd/change/{unique-id}")
    ResponseEntity<CommonResponse> changePassword(@PathVariable("unique-id") String uniqueId , @RequestBody @Valid PasswordChangeOTPRequest request , BindingResult result);

}
