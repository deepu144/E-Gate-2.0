package com.kce.egate.controller.controllerImpl;

import com.kce.egate.controller.EntryController;
import com.kce.egate.enumeration.ResponseStatus;
import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.service.EntryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kce/entry/")
@RequiredArgsConstructor
public class EntryControllerImpl implements EntryController {

    private final EntryService entryService;
    private static final Logger LOG = LoggerFactory.getLogger(EntryControllerImpl.class);

    @Override
    @PostMapping("/add")
    public ResponseEntity<CommonResponse> addOrUpdateEntry(@RequestParam String rollNumber,HttpServletRequest request){
        try {
            String header = request.getHeader("Authorization");
            return ResponseEntity.status(HttpStatus.OK).body(entryService.addOrUpdateEntry(rollNumber,header));
        }catch (Exception e){
            LOG.error("** addOrUpdateEntry : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }
    @Override
    @GetMapping("/today/utils")
    public ResponseEntity<CommonResponse> getTodayUtils(HttpServletRequest request){
        try {
            String header = request.getHeader("Authorization");
            return ResponseEntity.status(HttpStatus.OK).body(entryService.getTodayUtils(header));
        }catch (Exception e){
            LOG.error("** getTodayOutCount : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<CommonResponse> userLogin(@RequestBody AuthenticationRequest request){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(entryService.userLogin(request));
        }catch (Exception e){
            LOG.error("** userLogin : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @Override
    @GetMapping("/logout")
    public ResponseEntity<CommonResponse> userLogout(HttpServletResponse response, HttpServletRequest request){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(entryService.userLogout(response,request));
        }catch (Exception e){
            LOG.error("** userLogout : {}",e.getMessage());
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
