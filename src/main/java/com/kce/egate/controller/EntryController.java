package com.kce.egate.controller;

import com.kce.egate.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface EntryController {
    @PostMapping("/addEntry")
    ResponseEntity<CommonResponse> addOrUpdateEntry(@RequestParam  String rollNumber);
}
