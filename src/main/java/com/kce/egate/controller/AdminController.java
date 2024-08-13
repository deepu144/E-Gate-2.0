package com.kce.egate.controller;

import com.kce.egate.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;

public interface AdminController {
    @GetMapping("/")
    ResponseEntity<CommonResponse> getAllEntry(@RequestParam(required = false) String rollNumber,
                                               @RequestParam(required = false) LocalDate fromDate,
                                               @RequestParam(required = false) LocalDate toDate,
                                               @RequestParam(required = false) String batch,
                                               @RequestParam(defaultValue = "desc") String order,
                                               @RequestParam(defaultValue = "inTime") String orderBy,
                                               @RequestParam int page,
                                               @RequestParam int size
    );
    @PostMapping("/batch/add")
    ResponseEntity<CommonResponse> addBatch(@RequestParam String batch);
    @GetMapping("/batch")
    ResponseEntity<CommonResponse> getAllBatch();
    @DeleteMapping("batch")
    ResponseEntity<CommonResponse> deleteBatch(@RequestParam String batch);
}
