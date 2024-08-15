package com.kce.egate.repository;

import com.kce.egate.entity.OtpInfo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpInfoRepository extends MongoRepository<OtpInfo,String> {
    Optional<OtpInfo> findByEmail(String email);
}
