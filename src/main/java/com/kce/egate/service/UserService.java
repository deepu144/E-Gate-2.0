package com.kce.egate.service;

import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.request.PasswordChangeOTPRequest;
import com.kce.egate.request.VerifyOTPRequest;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.exceptions.InvalidPassword;
import com.kce.egate.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.management.InvalidAttributeValueException;

public interface UserService {
    CommonResponse signInUser(AuthenticationRequest authenticationRequest) throws IllegalAccessException, InvalidPassword;
    CommonResponse oauth2Callback(String email,String name,String picture,String id,String role) throws IllegalAccessException;
    CommonResponse logout(HttpServletRequest request, HttpServletResponse response);
    CommonResponse forgotPassword(String email) throws IllegalAccessException;
    CommonResponse verifyOtp(VerifyOTPRequest request) throws UserNotFoundException, IllegalAccessException, InvalidAttributeValueException;
    CommonResponse changePassword(String uniqueId, PasswordChangeOTPRequest request) throws IllegalAccessException, UserNotFoundException;
    CommonResponse beforeOAuth2(String role);
}
