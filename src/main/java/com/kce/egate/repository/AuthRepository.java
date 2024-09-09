package com.kce.egate.repository;

import com.kce.egate.entity.Auth;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends MongoRepository<Auth, ObjectId> {
    void deleteAllByEmail(String email);
    Optional<Auth> findByEmail(String email);
}
