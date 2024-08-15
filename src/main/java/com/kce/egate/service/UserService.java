package com.kce.egate.service;

import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.request.PasswordChangeOTPRequest;
import com.kce.egate.request.VerifyOTPRequest;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.util.exceptions.InvalidEmailException;
import com.kce.egate.util.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import javax.management.InvalidAttributeValueException;

public interface UserService {
    CommonResponse signInUser(AuthenticationRequest authenticationRequest);
    CommonResponse oauth2Callback(String email,String name,String picture,String id) throws IllegalAccessException;
    CommonResponse logout(HttpServletRequest request);

    CommonResponse addAdmin(String email) throws InvalidEmailException;

    CommonResponse forgotPassword(String email) throws UserNotFoundException;

    CommonResponse verifyOtp(VerifyOTPRequest request) throws UserNotFoundException, IllegalAccessException, InvalidAttributeValueException;

    CommonResponse changePassword(String uniqueId, PasswordChangeOTPRequest request) throws IllegalAccessException, UserNotFoundException;

    CommonResponse resendOtp(String email);
}
