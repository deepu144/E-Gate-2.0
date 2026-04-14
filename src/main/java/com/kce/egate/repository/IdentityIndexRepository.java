package com.kce.egate.repository;

import com.kce.egate.entity.IdentityIndex;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IdentityIndexRepository extends MongoRepository<IdentityIndex, String> {
    Optional<IdentityIndex> findByRollNumber(String rollNumber);
    boolean existsByRollNumber(String rollNumber);
    List<IdentityIndex> findByRollNumberIn(List<String> rollNumbers);
}
