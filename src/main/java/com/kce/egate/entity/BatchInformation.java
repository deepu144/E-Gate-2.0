package com.kce.egate.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
@AllArgsConstructor
@NoArgsConstructor
public class BatchInformation {
    private String _id;
    private String rollNumber;
    private String name;
    private String dept;
    private String batch;
}
