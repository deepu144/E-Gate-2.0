package com.kce.egate.controller;

import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface EntryController {
    @PostMapping("/add")
    ResponseEntity<CommonResponse> addOrUpdateEntry(@RequestParam  String rollNumber,HttpServletRequest request);
    @GetMapping("/today/utils")
    ResponseEntity<CommonResponse> getTodayUtils(HttpServletRequest request);
    @PostMapping("/login")
    ResponseEntity<CommonResponse> userLogin(AuthenticationRequest request);
    @GetMapping("/logout")
    ResponseEntity<CommonResponse> userLogout(String email);
}
