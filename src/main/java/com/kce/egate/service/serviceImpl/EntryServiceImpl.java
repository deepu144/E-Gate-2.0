package com.kce.egate.service.serviceImpl;

import com.kce.egate.constant.Constant;
import com.kce.egate.entity.*;
import com.kce.egate.enumeration.ResponseStatus;
import com.kce.egate.enumeration.Status;
import com.kce.egate.repository.BatchRepository;
import com.kce.egate.repository.DailyUtilsRepository;
import com.kce.egate.repository.EntryRepository;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.response.EntryResponse;
import com.kce.egate.service.EntryService;
import com.kce.egate.util.exceptions.InvalidBatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import javax.management.InvalidAttributeValueException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntryServiceImpl implements EntryService {
    private final EntryRepository entryRepository;
    private final MongoTemplate mongoTemplate;
    private final BatchRepository batchRepository;
    private final DailyUtilsRepository dailyUtilsRepository;
    @Override
    public CommonResponse addOrUpdateEntry(String rollNumber) throws InvalidBatchException, InvalidAttributeValueException {
        if(rollNumber.length()<5){
            throw new InvalidAttributeValueException(Constant.INVALID_ROLL_NUMBER);
        }
        Optional<Entry> optionalEntry = entryRepository.findByRollNumber(rollNumber);
        String batch;
        if(rollNumber.length()==5){
            batch = "Staff";
        }else{
            batch = getCollection(rollNumber);
        }
        Query queryForBatchInformation = new Query();
        queryForBatchInformation.addCriteria(Criteria.where("rollNumber").is(rollNumber));
        BatchInformation batchInformation = mongoTemplate.findOne(queryForBatchInformation, BatchInformation.class,batch);
        if(batchInformation==null){
            throw new InvalidAttributeValueException(Constant.INVALID_ROLL_NUMBER);
        }
        if(optionalEntry.isEmpty()){
            List<String> batchList = batchRepository.findAll()
                    .parallelStream()
                    .map(Batch::getBatchName)
                    .toList();
            if(!batchList.contains(batch)) {
                throw new InvalidBatchException(Constant.INVALID_BATCH);
            }
            Entry entry = new Entry();
            entry.setUniqueId(UUID.randomUUID().toString());
            entry.setRollNumber(rollNumber);
            entry.setBatch(batch);
            entry.setStatus(Status.OUT);
            entry.setOutDate(LocalDate.now());
            entry.setOutTime(LocalTime.now());
            entryRepository.save(entry);
            updateTodayUtils(true);
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
        Entry entry = optionalEntry.get();
        entry.setInDate(LocalDate.now());
        entry.setInTime(LocalTime.now());
        entry.setStatus(Status.IN);
        Query query = new Query();
        query.addCriteria(Criteria.where("rollNumber").is(entry.getRollNumber()));
        BatchEntry batchEntry = mongoTemplate.findOne(query,BatchEntry.class,batch);
        if(batchEntry==null){
            batchEntry = new BatchEntry();
            batchEntry.setUniqueId(UUID.randomUUID().toString());
            batchEntry.setRollNumber(entry.getRollNumber());
            batchEntry.getInDateList().add(entry.getInDate());
            batchEntry.getOutDateList().add(entry.getOutDate());
            batchEntry.getInTimeList().add(entry.getInTime());
            batchEntry.getOutTimeList().add(entry.getOutTime());
            batchEntry.setTotalEntry(batchEntry.getInDateList().size());
            mongoTemplate.save(batchEntry,batch);
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
                .build();
        entryRepository.delete(entry);
        updateTodayUtils(false);
        return CommonResponse.builder()
                .data(response)
                .successMessage(Constant.ENTRY_DELETED_SUCCESS)
                .status(ResponseStatus.SUCCESS)
                .code(200)
                .build();
    }

    @Override
    public CommonResponse getTodayInCount() {
        Optional<DailyUtils> dailyUtilsOptional = dailyUtilsRepository.findByToday(LocalDate.now());
        if(dailyUtilsOptional.isEmpty()){
            return CommonResponse.builder()
                    .code(200)
                    .successMessage(Constant.FETCH_IN_COUNT_SUCCESS)
                    .status(ResponseStatus.SUCCESS)
                    .data(0)
                    .build();
        }
        return CommonResponse.builder()
                .code(200)
                .successMessage(Constant.FETCH_IN_COUNT_SUCCESS)
                .status(ResponseStatus.SUCCESS)
                .data(dailyUtilsOptional.get().getInCount())
                .build();
    }

    @Override
    public CommonResponse getTodayOutCount() {
        Optional<DailyUtils> dailyUtilsOptional = dailyUtilsRepository.findByToday(LocalDate.now());
        if(dailyUtilsOptional.isEmpty()){
            return CommonResponse.builder()
                    .code(200)
                    .successMessage(Constant.FETCH_IN_COUNT_SUCCESS)
                    .status(ResponseStatus.SUCCESS)
                    .data(0)
                    .build();
        }
        return CommonResponse.builder()
                .code(200)
                .successMessage(Constant.FETCH_IN_COUNT_SUCCESS)
                .status(ResponseStatus.SUCCESS)
                .data(dailyUtilsOptional.get().getOutCount())
                .build();
    }

    private void updateTodayUtils(boolean check) {
        LocalDate today = LocalDate.now();
        Optional<DailyUtils> utilsOptional = dailyUtilsRepository.findByToday(today);
        DailyUtils dailyUtils;
        if(utilsOptional.isPresent()){
            dailyUtils = utilsOptional.get();
            if(check){
                dailyUtils.setOutCount(dailyUtils.getOutCount()+1);
            }else{
                dailyUtils.setInCount(dailyUtils.getInCount()+1);
            }
        }else{
            dailyUtils = new DailyUtils();
            dailyUtils.setUniqueId(UUID.randomUUID().toString());
            dailyUtils.setToday(today);
            if(check){
                dailyUtils.setOutCount(1L);
                dailyUtils.setInCount(0L);
            }else{
                dailyUtils.setOutCount(0L);
                dailyUtils.setInCount(1L);
            }
        }
        dailyUtilsRepository.save(dailyUtils);
    }

    public static String getCollection(String rollNumber) {
        int rollNumberLength = rollNumber.length();
        String collection;
        int batch = 0;
        if(rollNumberLength>=10 && rollNumberLength<=12){
            batch = Integer.parseInt(rollNumber.substring(4,6));
            if(rollNumberLength==10){
                int lateral = Integer.parseInt(rollNumber.substring(7,10));
                if(lateral>500 && lateral<800){
                    batch-=1;
                }
            }
        }else if(rollNumberLength>=6 && rollNumberLength<=8){
            batch = Integer.parseInt(rollNumber.substring(0,2));
        }
        collection = "Batch_"+batch+"-"+(batch+4);
        return collection;
    }
}