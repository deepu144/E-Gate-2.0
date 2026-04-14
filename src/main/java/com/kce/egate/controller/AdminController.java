package com.kce.egate.controller;

import com.kce.egate.request.PasswordChangeRequest;
import com.kce.egate.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AdminController {
    ResponseEntity<CommonResponse> getAllEntry(@RequestParam(required = false) String rollNumber,
                                               @RequestParam(required = false) LocalDate fromDate,
                                               @RequestParam(required = false) LocalDate toDate,
                                               @RequestParam(required = false) LocalTime fromTime,
                                               @RequestParam(required = false) LocalTime toTime,
                                               @RequestParam(required = false) String batch,
                                               @RequestParam(defaultValue = "desc") String order,
                                               @RequestParam(defaultValue = "inDate") String orderBy,
                                               @RequestParam int page,
                                               @RequestParam int size
    );
    ResponseEntity<CommonResponse> getAllTodayEntry(@RequestParam int page, @RequestParam int size);
    ResponseEntity<CommonResponse> addAdmin(@RequestParam String email);
    ResponseEntity<SseEmitter> addBatch(@RequestParam String batch, boolean isIncremental, @RequestParam("file") MultipartFile multipartFile);
    ResponseEntity<CommonResponse> getAllBatch();
    ResponseEntity<CommonResponse> deleteBatch(@RequestParam String batch);
    ResponseEntity<CommonResponse> changeAdminPassword(@RequestBody PasswordChangeRequest passwordChangeRequest);
    ResponseEntity<CommonResponse> getTodayUtils();
}
