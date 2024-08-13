package com.kce.egate.service.serviceImpl;

import com.kce.egate.constant.Constant;
import com.kce.egate.entity.Batch;
import com.kce.egate.entity.BatchEntry;
import com.kce.egate.entity.Entry;
import com.kce.egate.entity.Status;
import com.kce.egate.enumeration.ResponseStatus;
import com.kce.egate.repository.BatchRepository;
import com.kce.egate.repository.EntryRepository;
import com.kce.egate.response.CommonResponse;
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

    @Override
    public CommonResponse addOrUpdateEntry(String rollNumber) throws InvalidBatchException, InvalidAttributeValueException {
        if(rollNumber.length()<5){
            throw new InvalidAttributeValueException(Constant.INVALID_ROLL_NUMBER);
        }
        Optional<Entry> optionalEntry = entryRepository.findByRollNumber(rollNumber);
        String batch = getCollection(rollNumber);
        if(optionalEntry.isEmpty()){
            List<String> batchList = batchRepository.findAll()
                    .parallelStream()
                    .map(Batch::getBatchName)
                    .toList();
            if(!batchList.contains(batch)){
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
            return CommonResponse.builder()
                    .data(rollNumber)
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
        entryRepository.delete(entry);
        return CommonResponse.builder()
                .data(rollNumber)
                .successMessage(Constant.ENTRY_DELETED_SUCCESS)
                .status(ResponseStatus.SUCCESS)
                .code(200)
                .build();
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
