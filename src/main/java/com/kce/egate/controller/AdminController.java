package com.kce.egate.controller;

import com.kce.egate.request.PasswordChangeRequest;
import com.kce.egate.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface AdminController {
    @GetMapping("/entry")
    ResponseEntity<CommonResponse> getAllEntry(@RequestParam(required = false) String rollNumber,
                                               @RequestParam(required = false) LocalDate fromDate,
                                               @RequestParam(required = false) LocalDate toDate,
                                               @RequestParam(required = false) String batch,
                                               @RequestParam(defaultValue = "desc") String order,
                                               @RequestParam(defaultValue = "inTime") String orderBy,
                                               @RequestParam int page,
                                               @RequestParam int size
    );
    @GetMapping("/today/entry")
    ResponseEntity<CommonResponse> getAllTodayEntry(int page,int size);
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
}
