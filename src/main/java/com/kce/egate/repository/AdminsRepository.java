package com.kce.egate.repository;

import com.kce.egate.entity.Admins;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminsRepository extends MongoRepository<Admins,String> {
}
