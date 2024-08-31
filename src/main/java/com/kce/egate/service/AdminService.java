package com.kce.egate.service;

import com.kce.egate.request.PasswordChangeRequest;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.util.exceptions.*;
import org.springframework.web.multipart.MultipartFile;

import javax.management.InvalidAttributeValueException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.time.LocalDate;
import java.time.LocalTime;

public interface AdminService {
    CommonResponse getAllEntry(
            String rollNumber,
            LocalDate fromDate,
            LocalDate toDate,
            LocalTime fromTime,
            LocalTime toTime,
            String batch,
            int page,
            int size,
            String order,
            String orderBy
    ) throws InvalidFilterException, UserNotFoundException;

    CommonResponse addAdmin(String email) throws InvalidEmailException;

    CommonResponse addBatch(String batch, MultipartFile multipartFile) throws DuplicateInformationFoundException, IOException;

    CommonResponse getAllBatch();

    CommonResponse deleteBatch(String batch) throws ClassNotFoundException;

    CommonResponse changeAdminPassword(PasswordChangeRequest passwordChangeRequest) throws InvalidObjectException, PasswordNotMatchException, InvalidAttributeValueException;

    CommonResponse getAllTodayEntry(int page,int size) throws UserNotFoundException;

    CommonResponse getTodayUtils();
}
