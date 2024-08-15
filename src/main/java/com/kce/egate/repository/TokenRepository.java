package com.kce.egate.repository;

import com.kce.egate.entity.Token;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends MongoRepository<Token,String> {
    List<Token> findByUserEmail(String userEmail);
    Optional<Token> findByToken(String token);
    void deleteTokensByUserEmail(String userEmail);
}
