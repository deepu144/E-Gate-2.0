package com.kce.egate.service;

import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.util.exceptions.InvalidBatchException;
import com.kce.egate.util.exceptions.InvalidJWTTokenException;

import javax.management.InvalidAttributeValueException;

public interface EntryService {
    CommonResponse addOrUpdateEntry(String rollNumber,String header) throws InvalidBatchException, InvalidAttributeValueException, InvalidJWTTokenException, IllegalAccessException;

    CommonResponse getTodayUtils(String header) throws InvalidJWTTokenException, IllegalAccessException;

    CommonResponse userLogin(AuthenticationRequest request);

    CommonResponse userLogout(String email) throws IllegalAccessException;
}
