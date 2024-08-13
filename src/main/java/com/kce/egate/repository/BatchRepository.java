package com.kce.egate.repository;

import com.kce.egate.entity.Batch;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BatchRepository extends MongoRepository<Batch,String> {
    Optional<Batch> findByBatchName(String batchName);
    boolean existsByBatchName(String batchName);
}
