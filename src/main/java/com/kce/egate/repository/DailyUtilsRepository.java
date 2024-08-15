package com.kce.egate.repository;

import com.kce.egate.entity.DailyUtils;
import org.springframework.cglib.core.Local;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyUtilsRepository extends MongoRepository<DailyUtils,String> {
    Optional<DailyUtils> findByToday(LocalDate today);
}
