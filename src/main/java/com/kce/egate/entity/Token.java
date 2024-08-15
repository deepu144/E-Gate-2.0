package com.kce.egate.entity;

import com.kce.egate.enumeration.TokenType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Token {
    @Id
    private String _id;
    private String token;
    private TokenType tokenType;
    private boolean expired;
    private String userEmail;
}
