package com.kce.egate.service;

import com.kce.egate.exceptions.DuplicateInformationFoundException;
import com.kce.egate.exceptions.InvalidFilterException;
import com.kce.egate.exceptions.PasswordNotMatchException;
import com.kce.egate.exceptions.UserNotFoundException;
import com.kce.egate.request.PasswordChangeRequest;
import com.kce.egate.response.CommonResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    CommonResponse addAdmin(String email) throws Exception;

    SseEmitter addBatch(String batch, MultipartFile multipartFile, boolean isIncremental) throws DuplicateInformationFoundException, IOException;

    CommonResponse getAllBatch();

    CommonResponse deleteBatch(String batch) throws ClassNotFoundException;

    CommonResponse changeAdminPassword(PasswordChangeRequest passwordChangeRequest) throws InvalidObjectException, PasswordNotMatchException, InvalidAttributeValueException;

    CommonResponse getAllTodayEntry(int page,int size) throws UserNotFoundException;

    CommonResponse getTodayUtils();
}
