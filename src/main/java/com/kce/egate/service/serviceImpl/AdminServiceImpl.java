package com.kce.egate.service.serviceImpl;

import com.kce.egate.constant.Constant;
import com.kce.egate.entity.Batch;
import com.kce.egate.entity.BatchEntry;
import com.kce.egate.entity.Entry;
import com.kce.egate.entity.Status;
import com.kce.egate.enumeration.ResponseStatus;
import com.kce.egate.repository.BatchRepository;
import com.kce.egate.repository.EntryRepository;
import com.kce.egate.response.BatchObject;
import com.kce.egate.response.CommonResponse;
import com.kce.egate.response.EntryObject;
import com.kce.egate.response.ListResponse;
import com.kce.egate.service.AdminService;
import com.kce.egate.util.Mapper;
import com.kce.egate.util.exceptions.InvalidFilterException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
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

    @Override
    public CommonResponse getAllEntry(
            String rollNumber,
            LocalDate fromDate,
            LocalDate toDate,
            String batch,
            int page,
            int size,
            String order,
            String orderBy
    ) throws InvalidFilterException {
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
                    entryObjects.add(Mapper.convertToEntryObject(entry));
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
                            entryObjects.add(Mapper.convertToEntryObject(entry));
                        }
                    }else{
                        entryObjects.add(Mapper.convertToEntryObject(entry));
                    }
                }
            }
        }
        int fromIndex = Math.min(page * size, entryObjects.size());
        int toIndex = Math.min(fromIndex + size, entryObjects.size());
        List<EntryObject> paginatedEntries = entryObjects.subList(fromIndex, toIndex);
        ListResponse listResponse = new ListResponse(entryObjects.size(), paginatedEntries);
        return CommonResponse.builder()
                .status(ResponseStatus.SUCCESS)
                .code(200)
                .data(listResponse)
                .successMessage(Constant.ENTRY_FETCH_SUCCESS)
                .build();
    }

    private void addEntryFromEntryRepository(String rollNumber, LocalDate fromDate, LocalDate toDate, List<EntryObject> entryObjects) {
        Optional<Entry> optionalEntry = entryRepository.findByRollNumber(rollNumber);
        if(optionalEntry.isPresent()){
            Entry entry = optionalEntry.get();
            if(fromDate !=null){
                if(entry.getOutDate().isEqual(fromDate) || entry.getOutDate().isEqual(toDate) || (entry.getOutDate().isAfter(fromDate) && entry.getOutDate().isBefore(toDate))){
                    entryObjects.add(Mapper.convertToEntryObject(entry));
                }
            }else{
                entryObjects.add(Mapper.convertToEntryObject(entry));
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
    public CommonResponse addBatch(String batchName) {
        if(batchRepository.existsByBatchName(batchName)){
            throw new InvalidParameterException(Constant.DUPLICATE_BATCH_FOUND);
        }
        if(!batchName.startsWith("Batch_")){
            throw new InvalidParameterException(Constant.INVALID_BATCH);
        }
        if(!(batchName.charAt(8)=='-')){
            throw new InvalidParameterException(Constant.INVALID_BATCH);
        }
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
    public CommonResponse deleteBatch(String batchName) throws ClassNotFoundException {
        if(!batchName.startsWith("Batch_")){
            throw new InvalidParameterException(Constant.INVALID_BATCH);
        }
        if(!(batchName.charAt(8)=='-')){
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
