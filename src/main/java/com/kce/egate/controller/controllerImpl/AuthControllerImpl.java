package com.kce.egate.controller.controllerImpl;

import com.kce.egate.controller.AuthController;
import com.kce.egate.enumeration.ResponseStatus;
import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.request.PasswordChangeOTPRequest;
import com.kce.egate.request.VerifyOTPRequest;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthControllerImpl implements AuthController {

    public static final Logger log = LoggerFactory.getLogger(AuthControllerImpl.class);
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<CommonResponse> userSignIn(@RequestBody @Valid AuthenticationRequest request , BindingResult result){
        if(result.hasErrors()){
            log.error("** userSignIn: {}", Objects.requireNonNull(result.getFieldError()).getDefaultMessage());
            CommonResponse commonResponse = new CommonResponse();
            commonResponse.setCode(400);
            commonResponse.setStatus(ResponseStatus.FAILED);
            commonResponse.setData(null);
            commonResponse.setErrorMessage(Objects.requireNonNull(result.getFieldError()).getDefaultMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(commonResponse);
        }
        try{
            return ResponseEntity.status(HttpStatus.OK).body(userService.signInUser(request));
        }catch(Exception e) {
            log.error("** userSignIn: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<CommonResponse> logout(HttpServletRequest request, HttpServletResponse response){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(userService.logout(request,response));
        }catch (Exception e){
            log.error("** logout: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @PostMapping("/pwd/forgot")
    public ResponseEntity<CommonResponse> forgotPassword(@RequestParam String email){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(userService.forgotPassword(email));
        }catch (Exception e){
            log.error("** forgotPassword: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @PostMapping("/pwd/otp/verify")
    public ResponseEntity<CommonResponse> verifyOtp(@RequestBody @Valid VerifyOTPRequest request , BindingResult result){
        if(result.hasErrors()){
            log.error("** verifyOtp: {}", Objects.requireNonNull(result.getFieldError()).getDefaultMessage());
            CommonResponse commonResponse = new CommonResponse();
            commonResponse.setCode(400);
            commonResponse.setStatus(ResponseStatus.FAILED);
            commonResponse.setData(null);
            commonResponse.setErrorMessage(Objects.requireNonNull(result.getFieldError()).getDefaultMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(commonResponse);
        }
        try{
            return ResponseEntity.status(HttpStatus.OK).body(userService.verifyOtp(request));
        }catch(Exception e) {
            log.error("** verifyOtp: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @PostMapping("/pwd/change/{unique-id}")
    public ResponseEntity<CommonResponse> changePassword(@PathVariable("unique-id") String uniqueId , @RequestBody @Valid PasswordChangeOTPRequest request , BindingResult result){
        if(result.hasErrors()){
            log.error("** changePassword: {}", Objects.requireNonNull(result.getFieldError()).getDefaultMessage());
            CommonResponse commonResponse = new CommonResponse();
            commonResponse.setCode(400);
            commonResponse.setStatus(ResponseStatus.FAILED);
            commonResponse.setData(null);
            commonResponse.setErrorMessage(Objects.requireNonNull(result.getFieldError()).getDefaultMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(commonResponse);
        }
        try{
            return ResponseEntity.status(HttpStatus.OK).body(userService.changePassword(uniqueId,request));
        }catch(Exception e) {
            log.error("** changePassword: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @PostMapping("/before/oAuth2")
    public ResponseEntity<CommonResponse> beforeOAuth2(@RequestParam String email,@RequestParam String role){
        try{
            return ResponseEntity.status(HttpStatus.CREATED).body(userService.beforeOAuth2(email,role));
        }catch(Exception e) {
            log.error("** beforeOAuth2: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    public CommonResponse setServerError(Exception e){
        CommonResponse commonResponse = new CommonResponse();
        commonResponse.setCode(500);
        commonResponse.setStatus(ResponseStatus.FAILED);
        commonResponse.setData(null);
        commonResponse.setErrorMessage(e.getMessage());
        return commonResponse;
    }

}
