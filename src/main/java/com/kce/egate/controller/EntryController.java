package com.kce.egate.controller;

import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface EntryController {
    ResponseEntity<CommonResponse> addOrUpdateEntry(@RequestParam String rollNumber,HttpServletRequest request);
    ResponseEntity<CommonResponse> getTodayUtils(HttpServletRequest request);
    ResponseEntity<CommonResponse> userLogin(AuthenticationRequest request);
    ResponseEntity<CommonResponse> userLogout(HttpServletResponse response, HttpServletRequest request);
}
