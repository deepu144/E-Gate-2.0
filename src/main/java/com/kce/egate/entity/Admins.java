package com.kce.egate.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class Admins {
    @Id
    private String _id;
    private String adminEmail;
}
