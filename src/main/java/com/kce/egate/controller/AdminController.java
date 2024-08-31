package com.kce.egate.controller;

import com.kce.egate.request.PasswordChangeRequest;
import com.kce.egate.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AdminController {
    @GetMapping("/entry")
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
    @GetMapping("/today/entry")
    ResponseEntity<CommonResponse> getAllTodayEntry(@RequestParam int page, @RequestParam int size);
    @PostMapping("/add")
    ResponseEntity<CommonResponse> addAdmin(@RequestParam String email);
    @PostMapping("/batch/add")
    ResponseEntity<CommonResponse> addBatch(@RequestParam String batch, @RequestParam("file") MultipartFile multipartFile);
    @GetMapping("/batch")
    ResponseEntity<CommonResponse> getAllBatch();
    @DeleteMapping("/batch")
    ResponseEntity<CommonResponse> deleteBatch(@RequestParam String batch);
    @PutMapping("/pwd/change")
    ResponseEntity<CommonResponse> changeAdminPassword(@RequestBody PasswordChangeRequest passwordChangeRequest);
    @GetMapping("/today/utils")
    ResponseEntity<CommonResponse> getTodayUtils();
}
