package com.kce.egate.controller.controllerImpl;

import com.kce.egate.constant.Constant;
import com.kce.egate.controller.EntryController;
import com.kce.egate.enumeration.ResponseStatus;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.service.EntryService;
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
    public ResponseEntity<CommonResponse> addOrUpdateEntry(@RequestParam String rollNumber){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(entryService.addOrUpdateEntry(rollNumber));
        }catch (Exception e){
            LOG.error("** addOrUpdateEntry : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @GetMapping("/inCount")
    public ResponseEntity<CommonResponse> getTodayInCount(){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(entryService.getTodayInCount());
        }catch (Exception e){
            LOG.error("** getTodayInCount : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    @GetMapping("/outCount")
    public ResponseEntity<CommonResponse> getTodayOutCount(){
        try {
            return ResponseEntity.status(HttpStatus.OK).body(entryService.getTodayOutCount());
        }catch (Exception e){
            LOG.error("** getTodayOutCount : {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(setServerError(e));
        }
    }

    public CommonResponse setServerError(Exception e){
        CommonResponse commonResponse = new CommonResponse();
        commonResponse.setCode(500);
        commonResponse.setStatus(ResponseStatus.FAILED);
        commonResponse.setData(e.getMessage());
        commonResponse.setErrorMessage(Constant.SERVER_ERROR_MESSAGE);
        return commonResponse;
    }
}
