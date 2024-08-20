package com.kce.egate.repository;

import com.kce.egate.entity.EntryLoginUtils;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EntryLoginUtilsRepository extends MongoRepository<EntryLoginUtils,String> {
    boolean existsByUniqueId(String uniqueId);
    Optional<EntryLoginUtils> findByEmail(String email);
}
