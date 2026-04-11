package com.kce.egate.service.serviceImpl;

import com.kce.egate.constant.Constant;
import com.kce.egate.entity.*;
import com.kce.egate.enumeration.ResponseStatus;
import com.kce.egate.enumeration.Status;
import com.kce.egate.repository.*;
import com.kce.egate.request.AuthenticationRequest;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.response.DailyUtilsObject;
import com.kce.egate.response.EntryResponse;
import com.kce.egate.service.EntryService;
import com.kce.egate.util.JWTUtils;
import com.kce.egate.util.Mapper;
import com.kce.egate.exceptions.InvalidBatchException;
import com.kce.egate.exceptions.InvalidJWTTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.management.InvalidAttributeValueException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntryServiceImpl implements EntryService {
    private static final Logger log = LoggerFactory.getLogger(EntryServiceImpl.class);
    private final EntryRepository entryRepository;
    private final MongoTemplate mongoTemplate;
//    private final BatchRepository batchRepository;
    private final DailyUtilsRepository dailyUtilsRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTUtils jwtUtils;
    private final EntryLoginUtilsRepository loginUtilsRepository;
    private final IdentityIndexRepository identityIndexRepository;

    @Override
    public synchronized CommonResponse addOrUpdateEntry(String rollNumber,String header) throws InvalidBatchException, InvalidAttributeValueException, InvalidJWTTokenException, IllegalAccessException {
        authorizeToken(header);
        log.info("[SERVICE] User Authenticated, entry request: RollNumber = {}", rollNumber);
        if(rollNumber.length()<5){
            log.error("[SERVICE] Invalid RollNumber: {} ", rollNumber);
            throw new InvalidAttributeValueException(Constant.INVALID_ROLL_NUMBER);
        }
        Optional<Entry> optionalEntry = entryRepository.findByRollNumber(rollNumber);
        Optional<IdentityIndex> optionalIdentityIndex = identityIndexRepository.findByRollNumber(rollNumber);
        if(optionalIdentityIndex.isEmpty()){
            log.error("[SERVICE] RollNumber Unregistered: {} ", rollNumber);
            throw new InvalidAttributeValueException(Constant.INVALID_ROLL_NUMBER);
        }
        String batch = optionalIdentityIndex.get().getBatch();
        Query queryForBatchInformation = new Query();
        queryForBatchInformation.addCriteria(Criteria.where("rollNumber").is(rollNumber));
        BatchInformation batchInformation = mongoTemplate.findOne(queryForBatchInformation, BatchInformation.class,batch+"_Information");
        if(batchInformation == null) {
            log.error("[SERVICE] RollNumber Unregistered: {} ", rollNumber);
            throw new InvalidAttributeValueException(Constant.INVALID_ROLL_NUMBER);
        }
        if(optionalEntry.isEmpty()){
            return addEntry(rollNumber, batch, batchInformation);
        }
        return deleteEntry(rollNumber, optionalEntry.get(), batch, batchInformation);
    }

    private CommonResponse deleteEntry(String rollNumber, Entry entry, String batch, BatchInformation batchInformation) {
        entry.setInDate(LocalDate.now());
        entry.setInTime(LocalTime.now());
        entry.setStatus(Status.IN);

        Query query = new Query();
        query.addCriteria(Criteria.where("rollNumber").is(entry.getRollNumber()));
        BatchEntry batchEntry = mongoTemplate.findOne(query, BatchEntry.class, batch);
        if(batchEntry == null){
            batchEntry = new BatchEntry();
            batchEntry.setUniqueId(UUID.randomUUID().toString());
            batchEntry.setRollNumber(entry.getRollNumber());
            batchEntry.getInDateList().add(entry.getInDate());
            batchEntry.getOutDateList().add(entry.getOutDate());
            batchEntry.getInTimeList().add(entry.getInTime());
            batchEntry.getOutTimeList().add(entry.getOutTime());
            batchEntry.setTotalEntry(batchEntry.getInDateList().size());
            mongoTemplate.save(batchEntry, batch);
        }else{
            Query updateQuery = new Query(Criteria.where("_id").is(batchEntry.get_id()));
            Update update = new Update();
            update.inc("totalEntry", 1);
            update.push("inDateList", entry.getInDate());
            update.push("outDateList", entry.getOutDate());
            update.push("inTimeList", entry.getInTime());
            update.push("outTimeList", entry.getOutTime());
            mongoTemplate.findAndModify(updateQuery, update, BatchEntry.class, batch);
        }
        EntryResponse response = EntryResponse.builder()
                .name(batchInformation.getName())
                .rollNumber(rollNumber)
                .batch(batch)
                .dept(batchInformation.getDept())
                .inDate(entry.getInDate())
                .outDate(entry.getOutDate())
                .inTime(entry.getInTime())
                .outTime(entry.getOutTime())
                .status(Status.IN)
                .build();
        entryRepository.delete(entry);
        updateTodayUtils(false, rollNumber.length() != 5);
        return CommonResponse.builder()
                .data(response)
                .successMessage(Constant.ENTRY_DELETED_SUCCESS)
                .status(ResponseStatus.SUCCESS)
                .code(200)
                .build();
    }

    private CommonResponse addEntry(String rollNumber, String batch, BatchInformation batchInformation) throws InvalidBatchException {
        log.info("[SERVICE] Student Going Out: RollNumber = {}, batch = {}", rollNumber, batch);
//        if(!batchRepository.existsByBatchName(batch)) {
//            log.error("[SERVICE] Invalid Batch Name {}. Update Batch Details", batch);
//            throw new InvalidBatchException(Constant.INVALID_BATCH);
//        }
        Entry entry = new Entry();
        entry.setUniqueId(UUID.randomUUID().toString());
        entry.setRollNumber(rollNumber);
        entry.setBatch(batch);
        entry.setStatus(Status.OUT);
        entry.setOutDate(LocalDate.now());
        entry.setOutTime(LocalTime.now());
        entryRepository.save(entry);
        log.debug("[SERVICE] Entry Saved Successfully. Entry: {}", entry);
        updateTodayUtils(true, rollNumber.length() != 5);
        EntryResponse entryResponse = EntryResponse.builder()
                .rollNumber(rollNumber)
                .name(batchInformation.getName())
                .dept(batchInformation.getDept())
                .status(Status.OUT)
                .batch(batch)
                .inDate(null)
                .outDate(entry.getOutDate())
                .inTime(null)
                .outTime(entry.getOutTime())
                .build();
        return CommonResponse.builder()
                .data(entryResponse)
                .successMessage(Constant.ENTRY_CREATED_SUCCESS)
                .status(ResponseStatus.SUCCESS)
                .code(200)
                .build();
    }

    private String authorizeToken(String header) throws InvalidJWTTokenException, IllegalAccessException {
        if(header == null || header.isBlank()){
            log.error("[SERVICE] Authorization header is null or blank");
            throw new InvalidJWTTokenException(Constant.INVALID_JWT_TOKEN);
        }
        String token = header.substring(7);
        String issuer = jwtUtils.extractIssuer(token);
        if(issuer == null || !issuer.equals("717822F110 717822P212")){
            log.error("[SERVICE] Invalid issuer of JWT token");
            throw new InvalidJWTTokenException(Constant.INVALID_JWT_TOKEN);
        }
        String uniqueId = jwtUtils.extractValue(token,"uniqueId");
        if(!loginUtilsRepository.existsByUniqueId(uniqueId)){
            log.error("[SERVICE] invalid session");
            throw new IllegalAccessException(Constant.ILLEGAL_ACCESS);
        }
        if(!jwtUtils.extractValue(token,"roles").equals("USER")){
            throw new IllegalAccessException(ResponseStatus.UNAUTHORIZED.name());
        }
        return uniqueId;
    }

    @Override
    public CommonResponse getTodayUtils(String header) throws InvalidJWTTokenException, IllegalAccessException {
        authorizeToken(header);
        log.debug("[SERVICE] User Authorized successfully");
        return getCommonTodayUtils();
    }

    public CommonResponse getCommonTodayUtils() {
        Optional<DailyUtils> dailyUtilsOptional = dailyUtilsRepository.findByToday(LocalDate.now());
        log.debug("[SERVICE] Fetching today's utils");
        if(dailyUtilsOptional.isEmpty()){
            log.debug("[SERVICE] Today's utils not found, returning default values");
            return CommonResponse.builder()
                    .code(200)
                    .successMessage(Constant.FETCH_IN_COUNT_SUCCESS)
                    .status(ResponseStatus.SUCCESS)
                    .data(
                            DailyUtilsObject.builder()
                                    .staffInCount(0L)
                                    .studentInCount(0L)
                                    .studentOutCount(0L)
                                    .staffOutCount(0L)
                                    .build()
                    )
                    .build();
        }
        log.debug("[SERVICE] Today's utils found");
        DailyUtilsObject dailyUtilsObject = Mapper.convertToDailyUtilsObject(dailyUtilsOptional.get());
        return CommonResponse.builder()
                .code(200)
                .successMessage(Constant.FETCH_IN_COUNT_SUCCESS)
                .status(ResponseStatus.SUCCESS)
                .data(dailyUtilsObject)
                .build();
    }

    @Override
    public CommonResponse userLogin(AuthenticationRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        log.info("[SERVICE] user authenticated successfully email: {} ", request.getEmail());
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        log.debug("[SERVICE] userDetails: {} ", userDetails);
        return checkAndGetUserToken(userDetails.getUsername());
    }

    public CommonResponse checkAndGetUserToken(String userName) {
        Optional<EntryLoginUtils> loginUtilsOptional = loginUtilsRepository.findByEmail(userName);

        loginUtilsOptional.ifPresent(entryLoginUtils -> {
                log.warn("[SERVICE] Deleting old session: email = {}, sessionId = {}", entryLoginUtils.getEmail(), entryLoginUtils.getUniqueId());
                loginUtilsRepository.deleteById(entryLoginUtils.get_id());
            }
        );

        EntryLoginUtils loginUtils = new EntryLoginUtils();
        loginUtils.setEmail(userName);
        String uniqueId = UUID.randomUUID().toString();
        loginUtils.setUniqueId(uniqueId);
        loginUtilsRepository.save(loginUtils);

        log.info("[SERVICE] created user session - sessionId = {}",uniqueId);

        HashMap<String,Object> claims = new HashMap<>();
        claims.put("roles","USER");
        claims.put("uniqueId",uniqueId);
        String token = jwtUtils.generateUserToken(claims, userName);

        log.info("[SERVICE] JWT token generated. Token = {}",token);

        return CommonResponse.builder()
                .code(200)
                .status(ResponseStatus.SUCCESS)
                .data(List.of(token,userName))
                .successMessage(Constant.SIGN_IN_SUCCESS)
                .build();
    }

    @Override
    public CommonResponse userLogout(HttpServletResponse response, HttpServletRequest request) throws IllegalAccessException, InvalidJWTTokenException {
        String uniqueId = authorizeToken(request.getHeader("Authorization"));
        log.debug("[SERVICE] JWT Token validated, user session: {}", uniqueId);
        loginUtilsRepository.deleteByUniqueId(uniqueId);
        try {
            log.debug("[SERVICE] Redirecting to Login page: http://localhost:3000/entry");
            response.sendRedirect("http://localhost:3000/entry");
            return null;
        } catch (IOException e) {
            log.error("SERVICE] Login Page Redirect Failed");
            return CommonResponse.builder()
                    .code(500)
                    .status(ResponseStatus.FAILED)
                    .data(null)
                    .errorMessage(Constant.LOGOUT_ERROR)
                    .build();
        }
    }

    private void updateTodayUtils(boolean isCheckOut, boolean isStudent) {
        LocalDate today = LocalDate.now();
        Optional<DailyUtils> utilsOptional = dailyUtilsRepository.findByToday(today);
        if(utilsOptional.isPresent()){
            Query query = new Query(Criteria.where("_id").is(utilsOptional.get().get_id()));
            Update update = new Update();
            if(isCheckOut){
                if(isStudent) update.inc("studentOutCount",1);
                else update.inc("staffOutCount",1);
            }else{
                if(isStudent) update.inc("studentInCount",1);
                else update.inc("staffInCount",1);
            }
            mongoTemplate.findAndModify(query,update, DailyUtils.class,"dailyUtils");
        }else{
            DailyUtils dailyUtils = new DailyUtils();
            dailyUtils.setUniqueId(UUID.randomUUID().toString());
            dailyUtils.setToday(today);
            if(isCheckOut){
                if(isStudent){
                    dailyUtils.setStudentOutCount(1L);
                    dailyUtils.setStudentInCount(0L);
                    dailyUtils.setStaffInCount(0L);
                    dailyUtils.setStaffOutCount(0L);
                }else{
                    dailyUtils.setStudentOutCount(0L);
                    dailyUtils.setStudentInCount(0L);
                    dailyUtils.setStaffInCount(0L);
                    dailyUtils.setStaffOutCount(1L);
                }
            }else{
                if(isStudent){
                    dailyUtils.setStudentOutCount(0L);
                    dailyUtils.setStudentInCount(1L);
                    dailyUtils.setStaffInCount(0L);
                    dailyUtils.setStaffOutCount(0L);
                }else{
                    dailyUtils.setStudentOutCount(0L);
                    dailyUtils.setStudentInCount(0L);
                    dailyUtils.setStaffInCount(1L);
                    dailyUtils.setStaffOutCount(0L);
                }
            }
            dailyUtilsRepository.save(dailyUtils);
        }
    }

//    public static String getBatchCollectionName(String rollNumber) {
//        int rollNumberLength = rollNumber.length();
//        if(rollNumberLength == 5) return "Staff";
//        String collection;
//        int batch = 0;
//        if(rollNumberLength >= 10 && rollNumberLength <= 12){
//            batch = Integer.parseInt(rollNumber.substring(4,6)); //717822F110
//            if(rollNumberLength == 10){
//                int lateral = Integer.parseInt(rollNumber.substring(7));
//                if(lateral >= 500){
//                    batch-=1;
//                }
//            }
//        }else if(rollNumberLength >= 6 && rollNumberLength <= 8){
//            batch = Integer.parseInt(rollNumber.substring(0,2));
//        }
//        int year = (LocalDate.now().getYear() / 100) * 100;
//        collection = "Batch_" + (year + batch)+ "-" + (year + batch + 4);
//        return collection;
//    }
}