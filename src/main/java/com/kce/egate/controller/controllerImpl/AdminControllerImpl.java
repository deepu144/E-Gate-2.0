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
import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/kce/admin")
@RequiredArgsConstructor
public class AdminControllerImpl implements AdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminControllerImpl.class);
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
            return ResponseEntity.status(HttpStatus.OK).body(adminService.getAllEntry(rollNumber,fromDate,toDate,fromTime,toTime,batch,page,size,order,orderBy));
        }catch (Exception e){
            LOGGER.error("** getAllEntry : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/batch/add")
    public ResponseEntity<CommonResponse> addBatch(@RequestParam String batch , @RequestParam("file")MultipartFile multipartFile) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(adminService.addBatch(batch,multipartFile));
        }catch (Exception e) {
            LOGGER.error("** addBatch : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/batch")
    public ResponseEntity<CommonResponse> getAllBatch() {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.getAllBatch());
        }catch (Exception e){
            LOGGER.error("** getAllBatch : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/batch")
    public ResponseEntity<CommonResponse> deleteBatch(@RequestParam String batch){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.deleteBatch(batch));
        }catch (Exception e) {
            LOGGER.error("** deleteBatch : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/pwd/change")
    public ResponseEntity<CommonResponse> changeAdminPassword(@RequestBody PasswordChangeRequest passwordChangeRequest){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.changeAdminPassword(passwordChangeRequest));
        }catch (Exception e){
            LOGGER.error("** changeAdminPassword : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<CommonResponse> addAdmin(@RequestParam String email){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.addAdmin(email));
        }catch (Exception e){
            LOGGER.error("** addAdmin: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/today/entry")
    public ResponseEntity<CommonResponse> getAllTodayEntry(@RequestParam int page ,@RequestParam int size){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.getAllTodayEntry(page,size));
        }catch (Exception e){
            LOGGER.error("** getAllEntry: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/today/utils")
    public ResponseEntity<CommonResponse> getTodayUtils(){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(adminService.getTodayUtils());
        }catch (Exception e){
            LOGGER.error("** getTodayOutCount : {}",e.getMessage());
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
