package com.kce.egate.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class IdentityIndex {
    @Id
    private String rollNumber;
    private String batch;
}
