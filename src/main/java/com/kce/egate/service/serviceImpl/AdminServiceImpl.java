package com.kce.egate.service.serviceImpl;

import com.kce.egate.constant.Constant;
import com.kce.egate.entity.*;
import com.kce.egate.enumeration.ResponseStatus;
import com.kce.egate.enumeration.Status;
import com.kce.egate.exceptions.*;
import com.kce.egate.repository.*;
import com.kce.egate.request.EmailDetailRequest;
import com.kce.egate.request.PasswordChangeRequest;
import com.kce.egate.response.*;
import com.kce.egate.service.AdminService;
import com.kce.egate.util.EmailUtils;
import com.kce.egate.util.FileUtils;
import com.kce.egate.util.Mapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.management.InvalidAttributeValueException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.security.InvalidParameterException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final EntryRepository entryRepository;
    private final MongoTemplate mongoTemplate;
    private final BatchRepository batchRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailUtils emailUtils;
    private final AdminsRepository adminsRepository;
    private final EntryServiceImpl entryService;
    private final IdentityIndexRepository identityIndexRepository;

    @Override
    public CommonResponse getAllEntry(
            String rollNumber,
            LocalDate fromDate,
            LocalDate toDate,
            LocalTime fromTime,
            LocalTime toTime,
            String batch,
            int page,
            int size,
            String order,
            String orderBy
    ) throws InvalidFilterException, UserNotFoundException {
        log.info("[SERVICE] getAllEntry called, filters: RollNumber {}, fromDate {}, toDate {}, batch {}, page {}, size {}, order {}, OrderBy {}", rollNumber, fromDate, toDate, batch, page, size, order, orderBy);
        List<EntryObject> entryObjects = getAllEntryObject(rollNumber,fromDate,toDate,batch,order,orderBy);
        
        // Early return if no entries found
        if(entryObjects.isEmpty()){
            ListResponse listResponse = new ListResponse(0, new ArrayList<>());
            return CommonResponse.builder()
                    .status(ResponseStatus.SUCCESS)
                    .code(200)
                    .data(listResponse)
                    .successMessage(Constant.ENTRY_FETCH_SUCCESS)
                    .build();
        }

        int fromIndex = Math.min(page * size, entryObjects.size());
        int toIndex = Math.min(fromIndex + size, entryObjects.size());
        List<EntryObject> paginatedEntries = entryObjects.subList(fromIndex, toIndex);
        log.debug("[SERVICE] subList created, fromIndex {}, toIndex {}, page {}, size {}", fromIndex, toIndex, page, size);

        // Batch fetch all IdentityIndex and BatchInformation data to fix N+1 problem
        Set<String> rollNumbers = paginatedEntries.stream()
                .map(EntryObject::getRollNumber)
                .collect(Collectors.toSet());
        
        List<IdentityIndex> identityIndices = identityIndexRepository.findByRollNumberIn(new ArrayList<>(rollNumbers));
        Map<String, String> rollNumberToBatchMap = identityIndices.stream()
                .collect(Collectors.toMap(IdentityIndex::getRollNumber, IdentityIndex::getBatch));
        
        // Group roll numbers by batch for efficient BatchInformation fetching
        Map<String, List<String>> batchToRollNumbers = rollNumberToBatchMap.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
        
        // Batch fetch BatchInformation from each batch's collection
        Map<String, BatchInformation> rollNumberToInfoMap = new HashMap<>();
        log.debug("[SERVICE] Fetching student Information");
        for (Map.Entry<String, List<String>> entry : batchToRollNumbers.entrySet()) {
            String batchName = entry.getKey();
            List<String> batchRollNumbers = entry.getValue();

            List<BatchInformation> infos = mongoTemplate.find(
                    new Query(Criteria.where("rollNumber").in(batchRollNumbers)),
                    BatchInformation.class,
                    batchName + "_Information"
            );
            
            for (BatchInformation info : infos) {
                rollNumberToInfoMap.put(info.getRollNumber(), info);
            }
        }
        
        // Build responses without additional DB calls
        List<EntryResponse> entryResponses = new ArrayList<>();
        for(EntryObject entryObject : paginatedEntries){
            BatchInformation info = rollNumberToInfoMap.get(entryObject.getRollNumber());
            if(info == null){
                log.info("[SERVICE] Student Information not Found. Check RollNumber {}", entryObject.getRollNumber());
            }
            EntryResponse response = getEntryResponseObject(entryObject, info);
            entryResponses.add(response);
        }
        ListResponse listResponse = new ListResponse(entryObjects.size(), entryResponses);
        return CommonResponse.builder()
                .status(ResponseStatus.SUCCESS)
                .code(200)
                .data(listResponse)
                .successMessage(Constant.ENTRY_FETCH_SUCCESS)
                .build();
    }

    private static EntryResponse getEntryResponseObject(EntryObject entryObject, BatchInformation information) {
        EntryResponse response = new EntryResponse();
        response.setRollNumber(entryObject.getRollNumber());
        response.setOutTime(entryObject.getOutTime());
        response.setInTime(entryObject.getInTime());
        response.setOutDate(entryObject.getOutDate());
        response.setInDate(entryObject.getInDate());
        response.setStatus(entryObject.getStatus());
        if(information == null) {
            return response;
        }
        response.setName(information.getName());
        response.setDept(information.getDept());
        response.setBatch(information.getBatch());
        return response;
    }

    public List<EntryObject> getAllEntryObject(
            String rollNumber,
            LocalDate fromDate,
            LocalDate toDate,
            String batch,
            String order,
            String orderBy
    ) throws InvalidFilterException, UserNotFoundException {
        if(order==null){
            order = "asc";
        }
        if(orderBy==null){
            orderBy="inDate";
        }
        if(fromDate!=null && toDate!=null){
            if(toDate.isBefore(fromDate)){
                log.error("[SERVICE] Invalid Filter, From Date Should be less than or equal to To Date");
                throw new InvalidFilterException(Constant.INVALID_FILTER);
            }
        }
        if(toDate!=null){
            if(fromDate==null){
                log.error("[SERVICE] Invalid Filter, Select From Date");
                throw new InvalidFilterException(Constant.INVALID_FILTER);
            }
        }
        if(fromDate!=null){
            if(toDate==null){
                log.debug("[SERVICE] Setting toDate to Current Date");
                toDate = LocalDate.now();
            }
        }
        String collection;
        Query query = new Query();
        List<EntryObject> entryObjects = new ArrayList<>();
        if(batch!=null && !batch.isEmpty()){
            log.debug("[SERVICE] getting batch entry objects");
            collection = batch;
            if(rollNumber!=null && !rollNumber.isBlank()){
                log.debug("[SERVICE] adding rollNumber to query");
                query.addCriteria(Criteria.where("rollNumber").is(rollNumber));
                addEntryFromEntryRepository(rollNumber, fromDate, toDate, entryObjects);
            }
            getEntry(fromDate, toDate, orderBy, collection, query, entryObjects, order);
            if(rollNumber==null || rollNumber.isBlank()){
                List<Entry> entryList;
                if(fromDate!=null){
                    entryList = entryRepository.findByBatchAndOutDateBetween(batch, fromDate, toDate);
                }else{
                    entryList = entryRepository.findByBatch(batch);
                }
                for(Entry entry : entryList){
                    entryObjects.addFirst(Mapper.convertToEntryObject(entry));
                }
            }
        }else{
            List<Entry> entryList;
            if(rollNumber!=null && !rollNumber.isBlank()){
                log.debug("[SERVICE] adding rollNumber to query, batchName filter is null");
                Optional<IdentityIndex> optionalIdentityIndex = identityIndexRepository.findByRollNumber(rollNumber);
                if(optionalIdentityIndex.isEmpty()){
                    log.error("[SERVICE] RollNumber Not Exists");
                    throw new InvalidFilterException(Constant.INVALID_FILTER);
                }
                collection = optionalIdentityIndex.get().getBatch();
                query.addCriteria(Criteria.where("rollNumber").is(rollNumber));
                getEntry(fromDate, toDate, orderBy, collection, query, entryObjects,order);
                addEntryFromEntryRepository(rollNumber, fromDate, toDate, entryObjects);
            }else{
                log.debug("[SERVICE] Getting all batches Entry Objects");
                List<String> collections = batchRepository.findAll()
                        .parallelStream()
                        .map(Batch::getBatchName)
                        .toList();
                for(String c : collections){
                    getEntry(fromDate,toDate,orderBy,c,query,entryObjects,order);
                }
                entryList = entryRepository.findAll();
                for(Entry entry : entryList){
                    if(toDate!=null){
                        if(entry.getOutDate().isEqual(fromDate) || entry.getOutDate().isEqual(toDate) || (entry.getOutDate().isAfter(fromDate) && entry.getOutDate().isBefore(toDate))){
                            entryObjects.addFirst(Mapper.convertToEntryObject(entry));
                        }
                    }else{
                        entryObjects.addFirst(Mapper.convertToEntryObject(entry));
                    }
                }
            }
        }
        return entryObjects;
    }

    private void addEntryFromEntryRepository(String rollNumber, LocalDate fromDate, LocalDate toDate, List<EntryObject> entryObjects) {
        Optional<Entry> optionalEntry = entryRepository.findByRollNumber(rollNumber);
        if(optionalEntry.isPresent()){
            Entry entry = optionalEntry.get();
            if(fromDate != null) {
                if(entry.getOutDate().isEqual(fromDate) || entry.getOutDate().isEqual(toDate) || (entry.getOutDate().isAfter(fromDate) && entry.getOutDate().isBefore(toDate))){
                    entryObjects.addFirst(Mapper.convertToEntryObject(entry));
                }
            }else {
                entryObjects.addFirst(Mapper.convertToEntryObject(entry));
            }
        }
    }

    private void getEntry(LocalDate fromDate, LocalDate toDate, String orderBy, String collection, Query query, List<EntryObject> entryObjects , String order) {
        // Add projection to reduce data transfer - only fetch needed fields
        query.fields().include("inDateList").include("outDateList")
                     .include("inTimeList").include("outTimeList")
                     .include("rollNumber");
        
        List<BatchEntry> batchEntryList = mongoTemplate.find(query, BatchEntry.class , collection);

        if(toDate==null){
            log.debug("[SERVICE] fetched all entry from batchEntry");
            for(BatchEntry batchEntry : batchEntryList){
                int size = batchEntry.getInDateList().size();
                for(int i = 0 ; i < size ; i++){
                    EntryObject entryObject = getEntryObject(batchEntry, i);
                    entryObjects.add(entryObject);
                }
            }
        }else{
            log.debug("[SERVICE] fetched from fromDate {}  to toDate {} entry from batchEntry", fromDate, toDate);
            if(orderBy.equalsIgnoreCase("inDate")){
                log.debug("[SERVICE] orderBy inDate");
                for(BatchEntry batchEntry : batchEntryList){
                    int start = ceilBinarySearch(batchEntry.getInDateList(),fromDate);
                    int end = floorBinarySearch(batchEntry.getInDateList(),toDate);
                    if(start == -1 || end == -1) continue;
                    for(int i = start ; i <= end ; i++){
                        EntryObject entryObject = getEntryObject(batchEntry, i);
                        entryObjects.add(entryObject);
                    }
                }
            }else{
                log.debug("[SERVICE] orderBy outDate");
                for(BatchEntry batchEntry : batchEntryList){
                    int start = ceilBinarySearch(batchEntry.getOutDateList(),fromDate);
                    int end = floorBinarySearch(batchEntry.getOutDateList(),toDate);
                    if(start == -1 || end == -1) continue;
                    for(int i = start ; i <= end ; i++){
                        EntryObject entryObject = getEntryObject(batchEntry, i);
                        entryObjects.add(entryObject);
                    }
                }
            }
        }
        sortEntryObjects(entryObjects, orderBy, order);
    }

    private void sortEntryObjects(List<EntryObject> entryObjects, String orderBy, String order) {
        Comparator<EntryObject> comparator;
        
        if (orderBy.equalsIgnoreCase("inDate")) {
            comparator = Comparator.comparing(
                EntryObject::getInDate,
                Comparator.nullsLast(Comparator.naturalOrder())
            ).thenComparing(
                EntryObject::getInTime,
                Comparator.nullsLast(Comparator.naturalOrder())
            );

        } else {
            comparator = Comparator.comparing(
                EntryObject::getOutDate,
                Comparator.nullsLast(Comparator.naturalOrder())
            ).thenComparing(
                EntryObject::getOutTime,
                Comparator.nullsLast(Comparator.naturalOrder())
            );
        }
        
        if (order.equalsIgnoreCase("desc")) {
            comparator = comparator.reversed();
        }
        log.debug("[SERVICE] sorting[{}] entry objects", order);
        entryObjects.sort(comparator);
    }

    private EntryObject getEntryObject(BatchEntry batchEntry, int index) {
        EntryObject entryObject = new EntryObject();
        entryObject.setRollNumber(batchEntry.getRollNumber());
        entryObject.setStatus(Status.IN);
        entryObject.setOutTime(batchEntry.getOutTimeList().get(index));
        entryObject.setOutDate(batchEntry.getOutDateList().get(index));
        entryObject.setInTime(batchEntry.getInTimeList().get(index));
        entryObject.setInDate(batchEntry.getInDateList().get(index));
        return entryObject;
    }

    @Override
    public SseEmitter addBatch(String batchName, MultipartFile multipartFile, boolean isIncremental) throws DuplicateInformationFoundException, IOException {
        log.info("[SERVICE] addBatch called. BatchName {}", batchName);
        boolean isBatchExists = batchRepository.existsByBatchName(batchName);

        if(isIncremental && !isBatchExists) {
            log.error("[SERVICE] Batch does not exist");
            throw new InvalidParameterException(Constant.BATCH_NOT_FOUND);
        }

        if(!isIncremental && isBatchExists){
            log.error("[SERVICE] Batch already exists");
            throw new InvalidParameterException(Constant.DUPLICATE_BATCH_FOUND);
        }
        if(!batchName.startsWith("Batch_")){
            log.error("[SERVICE] Batch Name Always starts with Batch_");
            throw new InvalidParameterException(Constant.BATCH_NAME_FORMAT);
        }

        AtomicBoolean isRunning = new AtomicBoolean(true);
        SseEmitter sseEmitter = new SseEmitter(0L);
        sseEmitter.send(SseEmitter.event().name("START").data("Batch Upload Started"));

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
            () -> {
                if(isRunning.get()) {
                    try {
                        sseEmitter.send(
                                SseEmitter.event()
                                    .name("STATUS")
                                    .data("Uploading Batch Information")
                        );
                    } catch (Exception ex) {
                        isRunning.set(false);
                        sseEmitter.completeWithError(ex);
                    }
                }
            }, 0, 3, TimeUnit.SECONDS
        );

        Thread.ofVirtual()
                .name("batch-"+batchName+"-upload-thread")
                .start(() -> runAddBatchAndSendEvent(sseEmitter, batchName, multipartFile, isRunning, scheduler));

        return sseEmitter;
    }

    private void runAddBatchAndSendEvent(SseEmitter sseEmitter, String batchName, MultipartFile multipartFile, AtomicBoolean isRunning, ScheduledExecutorService scheduler) {
        try {
            Set<BatchInformation> batchInformationList = FileUtils.uploadBatchInformation(multipartFile);
            String batchInformation = batchName+"_Information";
            mongoTemplate.insert(batchInformationList, batchInformation);

            Batch batch = new Batch();
            batch.setUniqueId(UUID.randomUUID().toString());
            batch.setBatchName(batchName);
            batchRepository.save(batch);

            isRunning.set(false);
            scheduler.shutdown();

            sseEmitter.send(SseEmitter.event()
                    .name("END")
                    .data("Success"));
            sseEmitter.complete();

        } catch (Exception ex) {
            isRunning.set(false);
            scheduler.shutdown();
            try {
                sseEmitter.send(SseEmitter.event()
                        .name("ERROR")
                        .data(ex.getMessage()));
            } catch (IOException ignored) {}
            sseEmitter.completeWithError(ex);
        }

    }

    @Override
    public CommonResponse getAllBatch() {
        log.info("[SERVICE] getAllBatch called");
        List<Batch> batchList = batchRepository.findAll();
        List<BatchObject> batchObjectList = new ArrayList<>();
        for(Batch batch : batchList){
            batchObjectList.add(Mapper.convertToBatchObject(batch));
        }
        ListResponse listResponse = new ListResponse(batchObjectList.size(), batchObjectList);
        return CommonResponse.builder()
                .code(200)
                .successMessage(Constant.FETCH_BATCH_SUCCESS)
                .data(listResponse)
                .status(ResponseStatus.SUCCESS)
                .build();
    }

    @Override
    public CommonResponse addAdmin(String email) throws Exception {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("[SERVICE] Logged As {}, AddAdmin called, Admin Email to add {}", username, email);
        if(adminsRepository.existsByAdminEmail(email)){
            log.error("[SERVICE] Email {} is already Admin", email);
            throw new Exception(Constant.ALREADY_ADMIN);
        }
        EmailDetailRequest request = new EmailDetailRequest();
        String subject = "Welcome to E-Gate 2.0 - Your Admin Access Details";
        String body = """
        <html>
        <head>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    background-color: #f4f4f4;
                    color: #333;
                    margin: 0;
                    padding: 20px;
                }
                .container {
                    background-color: #ffffff;
                    border-radius: 8px;
                    padding: 20px;
                    max-width: 600px;
                    margin: auto;
                    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
                }
                .header {
                    background-color: #007BFF;
                    color: #ffffff;
                    padding: 10px 20px;
                    border-radius: 8px 8px 0 0;
                    text-align: center;
                }
                .content {
                    margin: 20px 0;
                }
                .button {
                    display: inline-block;
                    font-size: 16px;
                    color: #ffffff;
                    background-color: #007BFF;
                    padding: 10px 20px;
                    text-decoration: none;
                    border-radius: 5px;
                }
                .footer {
                    font-size: 12px;
                    color: #777;
                    text-align: center;
                    margin-top: 20px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    Welcome to E-Gate v2.0
                </div>
                <div class="content">
                    <p>Dear Administrator,</p>
                    <p>We are pleased to inform you that you have been granted administrative access to E-Gate v2.0. Your default password is <strong>"karpagam"</strong>. For security reasons, we encourage you to change your password immediately after logging in.</p>
                    <p>To access the E-Gate v2.0 system, please use the following link:</p>
                    <p><a href="#" class="button">Log in to E-Gate v2.0</a></p>
                    <p>If you encounter any issues or have any questions regarding your new role or the system, please do not hesitate to reach out to our support team.</p>
                    <p>Thank you for your attention to this matter. We look forward to your effective management within E-Gate v2.0.</p>
                </div>
                <div class="footer">
                    <p>This is an automated message. Please do not reply to this email.</p>
                    <p>&copy; 2026 E-gate v2.0. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """;
        request.setSubject(subject);
        request.setRecipient(email);
        request.setMsgBody(body);
        Thread.ofVirtual()
                .name("email-thread-"+email)
                .start(() -> emailUtils.sendMimeMessage(request));
        Admins admins = new Admins();
        admins.setAdminEmail(email);
        admins.setAddedBy(username);
        adminsRepository.save(admins);
        log.info("[SERVICE] New Admin added successfully by {}, New Admin Email: {}", email, username);
        return CommonResponse.builder()
                .code(201)
                .successMessage(Constant.ADMIN_ADDED_SUCCESS)
                .data(email)
                .status(ResponseStatus.CREATED)
                .build();
    }

    @Override
    public CommonResponse deleteBatch(String batchName) throws ClassNotFoundException {
        log.info("[SERVICE] deleteBatch called, Batch Name: {}", batchName);
        if(!batchName.startsWith("Batch_")) {
            log.error("[SERVICE] Batch Name Always starts with Batch_");
            throw new InvalidParameterException(Constant.BATCH_NAME_FORMAT);
        }
        Optional<Batch> optionalBatch = batchRepository.findByBatchName(batchName);
        if(optionalBatch.isEmpty()) {
            log.error("[SERVICE] Batch Not Found");
            throw new ClassNotFoundException(Constant.NO_BATCH_FOUND);
        }
        Batch batch = optionalBatch.get();
        batchRepository.deleteById(batch.get_id());
        log.info("[SERVICE] Batch Deleted Successfully");
        return CommonResponse.builder()
                .code(200)
                .status(ResponseStatus.DELETED)
                .successMessage(Constant.BATCH_DELETE_SUCCESS)
                .data(batchName)
                .build();
    }

    @Override
    public CommonResponse changeAdminPassword(PasswordChangeRequest passwordChangeRequest) throws InvalidObjectException, PasswordNotMatchException, InvalidAttributeValueException {
        log.info("[SERVICE] changeAdminPassword called, email {}", passwordChangeRequest.getEmail());
        if(passwordChangeRequest.getNewPassword().length()<8){
            log.error("[SERVICE] New Password length less than 8 characters");
            throw new InvalidAttributeValueException(Constant.PASSWORD_SIZE_NOT_MATCH);
        }
        Optional<User> userOptional = userRepository.findByEmail(passwordChangeRequest.getEmail());
        if(userOptional.isEmpty()){
            log.error("[SERVICE] User Not Found");
            throw new InvalidObjectException(Constant.USER_NOT_FOUND);
        }
        User user = userOptional.get();
        if(!passwordEncoder.matches(passwordChangeRequest.getOldPassword(), user.getPassword())){
            log.error("[SERVICE] Old Password Does Not Match");
            throw new PasswordNotMatchException(Constant.PASSWORD_NOT_MATCH);
        }
        user.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        userRepository.save(user);
        log.info("[SERVICE] Password Changed Successfully");

        var request = new EmailDetailRequest();
        String body = String.format(
                """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Password Change Confirmation</title>
                            <style>
                                body {
                                    font-family: Arial, sans-serif;
                                    background-color: #f4f4f4;
                                    margin: 0;
                                    padding: 0;
                                }
                                .container {
                                    max-width: 600px;
                                    margin: 20px auto;
                                    background-color: #ffffff;
                                    padding: 20px;
                                    border-radius: 8px;
                                    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
                                }
                                .header {
                                    background-color: #2c3e50;
                                    padding: 20px;
                                    border-radius: 8px 8px 0 0;
                                    text-align: center;
                                    color: #ffffff;
                                }
                                .header h1 {
                                    margin: 0;
                                    font-size: 24px;
                                }
                                .content {
                                    padding: 20px;
                                    font-size: 16px;
                                    line-height: 1.6;
                                    color: #333333;
                                }
                                .content h2 {
                                    color: #2c3e50;
                                    font-size: 20px;
                                }
                                .content p {
                                    margin: 10px 0;
                                }
                                .content ul {
                                    list-style-type: none;
                                    padding: 0;
                                }
                                .content ul li {
                                    background-color: #ecf0f1;
                                    margin: 5px 0;
                                    padding: 10px;
                                    border-radius: 4px;
                                }
                                .footer {
                                    text-align: center;
                                    padding: 20px;
                                    font-size: 14px;
                                    color: #777777;
                                    background-color: #ecf0f1;
                                    border-radius: 0 0 8px 8px;
                                }
                                .footer a {
                                    color: #2c3e50;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>Password Change Notification</h1>
                                </div>
                                <div class="content">
                                    <h2>Dear Admin,</h2>
                                    <p>We are pleased to inform you that your password for the E-gate 2.0 system has been successfully updated.</p>
                                    <p><strong>Summary of Changes:</strong></p>
                                    <ul>
                                        <li><strong>Account:</strong> %s</li>
                                        <li><strong>Date and Time of Change:</strong> %s</li>
                                    </ul>
                                    <p>If you did not request this change, please contact our support team immediately to ensure the security of your account.</p>
                                    <p>For your protection, please avoid sharing your password with anyone and ensure it is stored securely.</p>
                                </div>
                                <div class="footer">
                                    <p>If you have any questions or need assistance, feel free to <a href="%s">contact us</a>.</p>
                                    <p>Best regards,<br>The E-gate 2.0 Team</p>
                                </div>
                            </div>
                        </body>
                        </html>
               """
        ,user.getEmail(),LocalDate.now()+" "+LocalTime.now(),"mailto:kce.egate@gmail.com");
        request.setRecipient(user.getEmail());
        request.setMsgBody(body);
        request.setSubject("E-gate 2.0: Your Password Has Been Reset Successfully");

        Thread.ofVirtual()
                .name("email-thread-"+user.getEmail())
                .start(() -> emailUtils.sendMimeMessage(request));
        return CommonResponse.builder()
                .code(200)
                .successMessage(Constant.PASSWORD_CHANGED_SUCCESS)
                .data(null)
                .status(ResponseStatus.UPDATED)
                .build();
    }

    @Override
    public CommonResponse getAllTodayEntry(int page,int size) throws UserNotFoundException {
        log.debug("[SERVICE] Starting to process getAllTodayEntry");

        PageRequest pageable = PageRequest.of(page, size);
        List<Entry> entryList = entryRepository.findAll(pageable).getContent();
        List<EntryResponse> entryResponses = new ArrayList<>();
        for(Entry entry : entryList){
            Optional<IdentityIndex> optionalIdentityIndex = identityIndexRepository.findByRollNumber(entry.getRollNumber());
            if(optionalIdentityIndex.isEmpty()){
                log.error("[SERVICE] RollNumber Not Exists");
                throw new UserNotFoundException(Constant.STUDENT_NOT_FOUND);
            }
            String batch = optionalIdentityIndex.get().getBatch();
            var information = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("rollNumber").is(entry.getRollNumber())), BatchInformation.class,batch +"_Information");
            if(information==null){
                log.error("[SERVICE] Student Information not exists for RollNumber {}", entry.getRollNumber());
                throw new UserNotFoundException(Constant.STUDENT_NOT_FOUND);
            }
            EntryResponse response = getEntryResponseObjectFromEntry(entry, information);
            entryResponses.add(response);
        }
        long count = entryRepository.count();
        ListResponse listResponse = new ListResponse(count,entryResponses);
        return CommonResponse.builder()
                .code(200)
                .status(ResponseStatus.SUCCESS)
                .data(listResponse)
                .successMessage(Constant.ENTRY_FETCH_SUCCESS)
                .build();
    }

    @Override
    public CommonResponse getTodayUtils() {
        return entryService.getCommonTodayUtils();
    }

    private EntryResponse getEntryResponseObjectFromEntry(Entry entry, BatchInformation information) {
        EntryResponse response = new EntryResponse();
        response.setRollNumber(entry.getRollNumber());
        response.setStatus(entry.getStatus());
        response.setOutTime(entry.getOutTime());
        response.setOutDate(entry.getOutDate());
        response.setName(information.getName());
        response.setInTime(entry.getInTime());
        response.setInDate(entry.getInDate());
        response.setDept(information.getDept());
        response.setBatch(information.getBatch());
        return response;
    }

    public int floorBinarySearch(List<LocalDate> inDateList, LocalDate fromDate) {
        if(inDateList == null || inDateList.isEmpty()){
            return -1;
        }
        int low = 0;
        int high = inDateList.size() - 1;
        if(inDateList.getFirst().isAfter(fromDate)){
            return -1;
        }
        while (low <= high) {
            int mid = low + (high - low) / 2;
            LocalDate midDate = inDateList.get(mid);
            if (midDate.equals(fromDate)) {
                return mid;
            } else if (midDate.isBefore(fromDate)) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return --low;
    }
    
    public int ceilBinarySearch(List<LocalDate> inDateList, LocalDate toDate) {
        if(inDateList == null || inDateList.isEmpty()){
            return -1;
        }
        int low = 0;
        int high = inDateList.size() - 1;
        if(inDateList.getLast().isBefore(toDate)){
            return -1;
        }
        while (low <= high) {
            int mid = low + (high - low) / 2;
            LocalDate midDate = inDateList.get(mid);
            if (midDate.equals(toDate)) {
                return mid;
            } else if (midDate.isAfter(toDate)) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }
}
