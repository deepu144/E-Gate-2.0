package com.kce.egate.service.serviceImpl;

import com.kce.egate.constant.Constant;
import com.kce.egate.entity.*;
import com.kce.egate.enumeration.ResponseStatus;
import com.kce.egate.enumeration.Status;
import com.kce.egate.repository.AdminsRepository;
import com.kce.egate.repository.BatchRepository;
import com.kce.egate.repository.EntryRepository;
import com.kce.egate.repository.UserRepository;
import com.kce.egate.request.EmailDetailRequest;
import com.kce.egate.request.PasswordChangeRequest;
import com.kce.egate.response.*;
import com.kce.egate.service.AdminService;
import com.kce.egate.util.EmailUtils;
import com.kce.egate.util.FileUtils;
import com.kce.egate.util.Mapper;
import com.kce.egate.util.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.management.InvalidAttributeValueException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.security.InvalidParameterException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

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
        List<EntryObject> entryObjects = getAllEntryObject(rollNumber,fromDate,toDate,batch,order,orderBy);
        if(!(fromTime==null && toTime==null)){
            if (fromTime == null) fromTime = LocalTime.MIN;
            if (toTime == null) toTime = LocalTime.MAX;
            LocalTime finalFromTime = fromTime;
            LocalTime finalToTime = toTime;
            entryObjects =  entryObjects.stream()
                    .filter(e -> !e.getOutTime().isBefore(finalFromTime) && !e.getOutTime().isAfter(finalToTime))
                    .toList();
        }
        int fromIndex = Math.min(page * size, entryObjects.size());
        int toIndex = Math.min(fromIndex + size, entryObjects.size());
        List<EntryObject> paginatedEntries = entryObjects.subList(fromIndex, toIndex);
        List<EntryResponse> entryResponses = new ArrayList<>();
        for(EntryObject entryObject : paginatedEntries){
            BatchInformation information = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("rollNumber").is(entryObject.getRollNumber())), BatchInformation.class,EntryServiceImpl.getCollection(entryObject.getRollNumber())+"_Information");
            if(information==null){
                throw new UserNotFoundException(Constant.STUDENT_NOT_FOUND);
            }
            EntryResponse response = getEntryResponseObject(entryObject, information);
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
    ) throws InvalidFilterException {
        if(order==null){
            order = "asc";
        }
        if(orderBy==null){
            orderBy="inDate";
        }
        if(fromDate!=null && toDate!=null){
            if(toDate.isBefore(fromDate)){
                throw new InvalidFilterException(Constant.INVALID_FILTER);
            }
        }
        if(toDate!=null){
            if(fromDate==null){
                throw new InvalidFilterException(Constant.INVALID_FILTER);
            }
        }
        if(fromDate!=null){
            if(toDate==null){
                toDate = LocalDate.now();
            }
        }
        String collection;
        Query query = new Query();
        List<EntryObject> entryObjects = new ArrayList<>();
        if(batch!=null && !batch.isEmpty()){
            collection = batch;
            if(rollNumber!=null && !rollNumber.isEmpty()){
                collection = EntryServiceImpl.getCollection(rollNumber);
                if(!collection.equalsIgnoreCase(batch)){
                    throw new InvalidFilterException(Constant.INVALID_FILTER);
                }
            }
            if(rollNumber!=null && !rollNumber.isEmpty()){
                query.addCriteria(Criteria.where("rollNumber").is(rollNumber));
                addEntryFromEntryRepository(rollNumber, fromDate, toDate, entryObjects);
            }
            getEntry(fromDate, toDate, orderBy, collection, query, entryObjects,order);
            if(rollNumber==null || rollNumber.isEmpty()){
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
            if(rollNumber!=null && !rollNumber.isEmpty()){
                collection = EntryServiceImpl.getCollection(rollNumber);
                query.addCriteria(Criteria.where("rollNumber").is(rollNumber));
                getEntry(fromDate, toDate, orderBy, collection, query, entryObjects,order);
                addEntryFromEntryRepository(rollNumber, toDate, toDate, entryObjects);
            }else{
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
                        if(toDate.isAfter(entry.getOutDate()) || toDate.equals(entry.getOutDate())){
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
            if(fromDate !=null) {
                if(entry.getOutDate().isEqual(fromDate) || entry.getOutDate().isEqual(toDate) || (entry.getOutDate().isAfter(fromDate) && entry.getOutDate().isBefore(toDate))){
                    entryObjects.addFirst(Mapper.convertToEntryObject(entry));
                }
            }else {
                entryObjects.addFirst(Mapper.convertToEntryObject(entry));
            }
        }
    }

    private void getEntry(LocalDate fromDate, LocalDate toDate, String orderBy, String collection, Query query, List<EntryObject> entryObjects , String order) {
        List<BatchEntry> batchEntryList = mongoTemplate.find(query, BatchEntry.class , collection);
        if(toDate==null){
            for(BatchEntry batchEntry : batchEntryList){
                for(int i = 0 ; i < batchEntry.getInDateList().size() ; i++){
                    EntryObject entryObject = getEntryObject(batchEntry, i);
                    entryObjects.add(entryObject);
                }
            }
        }else{
            if(orderBy.equalsIgnoreCase("inDate")){
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
        if(orderBy.equalsIgnoreCase("inDate")) {
            if(order.equalsIgnoreCase("asc")){
                entryObjects.sort((a, b) -> {
                    int dateComparison = compareDates(b.getInDate(), a.getInDate());
                    if (dateComparison == 0) {
                        return compareTimes(a.getOutTime(), b.getOutTime());
                    }
                    return dateComparison;
                });
            }else{
                entryObjects.sort((a, b) -> {
                    int dateComparison = compareDates(a.getInDate(), b.getInDate());
                    if (dateComparison == 0) {
                        return compareTimes(a.getOutTime(), b.getOutTime());
                    }
                    return dateComparison;
                });
            }
        }else{
            if(order.equalsIgnoreCase("asc")){
                entryObjects.sort((a, b) -> {
                    int dateComparison = compareDates(b.getOutDate(), a.getOutDate());
                    if (dateComparison == 0) {
                        return compareTimes(a.getOutTime(), b.getOutTime());
                    }
                    return dateComparison;
                });
            }else{
                entryObjects.sort((a, b) -> {
                    int dateComparison = compareDates(a.getInDate(), b.getInDate());
                    if (dateComparison == 0) {
                        return compareTimes(a.getOutTime(), b.getOutTime());
                    }
                    return dateComparison;
                });
            }
        }
    }

    private int compareDates(LocalDate date1, LocalDate date2) {
        if (date1 == null && date2 == null) {
            return 0;
        } else if (date1 == null) {
            return 1;
        } else if (date2 == null) {
            return -1;
        } else {
            return date2.compareTo(date1);
        }
    }

    private int compareTimes(LocalTime time1, LocalTime time2) {
        if (time1 == null && time2 == null) {
            return 0;
        } else if (time1 == null) {
            return 1;
        } else if (time2 == null) {
            return -1;
        } else {
            return time2.compareTo(time1);
        }
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
    public CommonResponse addBatch(String batchName, MultipartFile multipartFile) throws DuplicateInformationFoundException, IOException {
        if(batchRepository.existsByBatchName(batchName)){
            throw new InvalidParameterException(Constant.DUPLICATE_BATCH_FOUND);
        }
        if(!batchName.startsWith("Batch_")){
            throw new InvalidParameterException(Constant.INVALID_BATCH);
        }
        if(!(batchName.charAt(10)=='-')){
            throw new InvalidParameterException(Constant.INVALID_BATCH);
        }
        Set<BatchInformation> batchInformationList = FileUtils.uploadBatchInformation(multipartFile);
        String batchInformation = batchName+"_Information";
        mongoTemplate.insert(batchInformationList,batchInformation);
        Batch batch = new Batch();
        batch.setUniqueId(UUID.randomUUID().toString());
        batch.setBatchName(batchName);
        batchRepository.save(batch);
        BatchObject batchObject = Mapper.convertToBatchObject(batch);
        return CommonResponse.builder()
                .code(201)
                .status(ResponseStatus.CREATED)
                .data(batchObject)
                .successMessage(Constant.BATCH_ADD_SUCCESS)
                .build();
    }
    @Override
    public CommonResponse getAllBatch() {
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
    public CommonResponse addAdmin(String email) throws InvalidEmailException {
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
                    <p>&copy; 2024 E-gate v2.0. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """;
        request.setSubject(subject);
        request.setRecipient(email);
        request.setMsgBody(body);
        boolean isSent = emailUtils.sendMimeMessage(request);
        if(!isSent){
            throw new InvalidEmailException(Constant.INVALID_EMAIL);
        }
        Admins admins = new Admins();
        admins.setAdminEmail(email);
        adminsRepository.save(admins);
        return CommonResponse.builder()
                .code(201)
                .successMessage(Constant.ADMIN_ADDED_SUCCESS)
                .data(email)
                .status(ResponseStatus.CREATED)
                .build();
    }

    @Override
    public CommonResponse deleteBatch(String batchName) throws ClassNotFoundException {
        if(!batchName.startsWith("Batch_")){
            throw new InvalidParameterException(Constant.INVALID_BATCH);
        }
        if(!(batchName.charAt(10)=='-')){
            throw new InvalidParameterException(Constant.INVALID_BATCH);
        }
        Optional<Batch> optionalBatch = batchRepository.findByBatchName(batchName);
        if(optionalBatch.isEmpty()){
            throw new ClassNotFoundException(Constant.NO_BATCH_FOUND);
        }
        Batch batch = optionalBatch.get();
        batchRepository.deleteById(batch.get_id());
        return CommonResponse.builder()
                .code(200)
                .status(ResponseStatus.DELETED)
                .successMessage(Constant.BATCH_DELETE_SUCCESS)
                .data(batchName)
                .build();
    }

    @Override
    public CommonResponse changeAdminPassword(PasswordChangeRequest passwordChangeRequest) throws InvalidObjectException, PasswordNotMatchException, InvalidAttributeValueException {
        if(passwordChangeRequest.getNewPassword().length()<8){
            throw new InvalidAttributeValueException(Constant.PASSWORD_SIZE_NOT_MATCH);
        }
        Optional<User> userOptional = userRepository.findByEmail(passwordChangeRequest.getEmail());
        if(userOptional.isEmpty()){
            throw new InvalidObjectException(Constant.USER_NOT_FOUND);
        }
        User user = userOptional.get();
        if(!passwordEncoder.matches(passwordChangeRequest.getOldPassword(), user.getPassword())){
            throw new PasswordNotMatchException(Constant.PASSWORD_NOT_MATCH);
        }
        user.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        userRepository.save(user);
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
                                    <p>Thank you for using E-gate 2.0.</p>
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
        request.setSubject("E-gate 2.0: Your Password Has Been Successfully Updated");
        emailUtils.sendMimeMessage(request);
        return CommonResponse.builder()
                .code(200)
                .successMessage(Constant.PASSWORD_CHANGED_SUCCESS)
                .data(null)
                .status(ResponseStatus.UPDATED)
                .build();
    }

    @Override
    public CommonResponse getAllTodayEntry(int page,int size) throws UserNotFoundException {
        PageRequest pageable = PageRequest.of(page, size);
        List<Entry> entryList = entryRepository.findAll(pageable).getContent();
        List<EntryResponse> entryResponses = new ArrayList<>();
        for(Entry entry : entryList){
            var information = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("rollNumber").is(entry.getRollNumber())), BatchInformation.class,EntryServiceImpl.getCollection(entry.getRollNumber())+"_Information");
            if(information==null){
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
        int low = 0;
        int high = inDateList.size() - 1;
        if(inDateList.isEmpty()){
            return -1;
        }
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
        int low = 0;
        int high = inDateList.size() - 1;
        if(inDateList.isEmpty()){
            return -1;
        }
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
