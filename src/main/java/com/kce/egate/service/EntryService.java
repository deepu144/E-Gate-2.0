package com.kce.egate.service;

import com.kce.egate.response.CommonResponse;
import com.kce.egate.util.exceptions.InvalidBatchException;

import javax.management.InvalidAttributeValueException;

public interface EntryService {
    CommonResponse addOrUpdateEntry(String rollNumber) throws InvalidBatchException, InvalidAttributeValueException;

    CommonResponse getTodayInCount();

    CommonResponse getTodayOutCount();
}
