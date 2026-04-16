package com.kce.egate.controller.controllerImpl;

import com.kce.egate.controller.AdminController;
import com.kce.egate.enumeration.ResponseStatus;
import com.kce.egate.request.PasswordChangeRequest;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/kce/admin")
@RequiredArgsConstructor
public class AdminControllerImpl implements AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminControllerImpl.class);
    private final AdminService adminService;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/entry")
    public ResponseEntity<CommonResponse> getAllEntry(@RequestParam(required = false) String rollNumber,
                                                      @RequestParam(required = false) LocalDate fromDate,
                                                      @RequestParam(required = false) LocalDate toDate,
                                                      @RequestParam(required = false) LocalTime fromTime,
                                                      @RequestParam(required = false) LocalTime toTime,
                                                      @RequestParam(required = false) String batch,
                                                      @RequestParam(defaultValue = "desc") String order,
                                                      @RequestParam(defaultValue = "inDate") String orderBy,
                                                      @RequestParam int page,
                                                      @RequestParam int size
    ){
        try {
            log.debug("[CONTROLLER] getAllEntry method called");
            return ResponseEntity.status(HttpStatus.OK).body(adminService.getAllEntry(rollNumber,fromDate,toDate,fromTime,toTime,batch,page,size,order,orderBy));
        }catch (Exception e){
            log.error("** getAllEntry : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/batch/add")
    public ResponseEntity<SseEmitter> addBatch(@RequestParam String batch,
                                               @RequestParam boolean isIncremental,
                                               @RequestParam("file") MultipartFile multipartFile) {
        try {
            log.debug("[CONTROLLER] addBatch is called. batch {}, isIncremental {}", batch, isIncremental);
            return ResponseEntity.ok(adminService.addBatch(batch,multipartFile, isIncremental));
        }catch (Exception e) {
            log.error("** addBatch : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/batch")
    public ResponseEntity<CommonResponse> getAllBatch() {
        try {
            log.debug("[CONTROLLER] getAllBatch is called");
            return ResponseEntity.status(HttpStatus.OK).body(adminService.getAllBatch());
        }catch (Exception e){
            log.error("** getAllBatch : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/batch")
    public ResponseEntity<CommonResponse> deleteBatch(@RequestParam String batch){
        try {
            log.debug("[CONTROLLER] deleteBatch is called. Batch {}", batch);
            return ResponseEntity.status(HttpStatus.OK).body(adminService.deleteBatch(batch));
        }catch (Exception e) {
            log.error("** deleteBatch : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/pwd/change")
    public ResponseEntity<CommonResponse> changeAdminPassword(@RequestBody PasswordChangeRequest passwordChangeRequest){
        try {
            log.debug("[CONTROLLER] change password is called. Email {}", passwordChangeRequest.getEmail());
            return ResponseEntity.status(HttpStatus.OK).body(adminService.changeAdminPassword(passwordChangeRequest));
        }catch (Exception e){
            log.error("** changeAdminPassword : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<CommonResponse> addAdmin(@RequestParam String email){
        try {
            log.debug("[CONTROLLER] addAdmin is called.");
            return ResponseEntity.status(HttpStatus.OK).body(adminService.addAdmin(email));
        }catch (Exception e){
            log.error("** addAdmin: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/today/entry")
    public ResponseEntity<CommonResponse> getAllTodayEntry(@RequestParam int page ,@RequestParam int size){
        try {
            log.debug("[CONTROLLER] getAllTodayEntry is called. Page {}, size {}", page, size);
            return ResponseEntity.status(HttpStatus.OK).body(adminService.getAllTodayEntry(page,size));
        }catch (Exception e){
            log.error("** getAllEntry: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/today/utils")
    public ResponseEntity<CommonResponse> getTodayUtils(){
        try {
            log.debug("[CONTROLLER] getTodayUtils is called.");
            return ResponseEntity.status(HttpStatus.OK).body(adminService.getTodayUtils());
        }catch (Exception e){
            log.error("** getTodayOutCount : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    public CommonResponse setServerError(Exception e){
        e.printStackTrace();
        CommonResponse commonResponse = new CommonResponse();
        commonResponse.setCode(500);
        commonResponse.setStatus(ResponseStatus.FAILED);
        commonResponse.setData(null);
        commonResponse.setErrorMessage(e.getMessage());
        return commonResponse;
    }

}
