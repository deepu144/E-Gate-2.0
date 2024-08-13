package com.kce.egate.repository;

import com.kce.egate.entity.Entry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntryRepository extends MongoRepository<Entry,String> {
    Optional<Entry> findByRollNumber(String rollNumber);
    List<Entry> findByBatch(String batch);
    List<Entry> findByBatchAndOutDateBetween(String batch , LocalDate fromDate , LocalDate toDate);
}
