package com.kce.egate.repository;

import com.kce.egate.entity.Auth;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRepository extends MongoRepository<Auth, ObjectId> {
}
