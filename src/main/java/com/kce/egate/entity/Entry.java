package com.kce.egate.entity;

import com.kce.egate.enumeration.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document
public class Entry {
    @Id
    private String _id;
    private String uniqueId;
    private String rollNumber;
    private String batch;
    private LocalDate inDate;
    private LocalDate outDate;
    private LocalTime inTime;
    private LocalTime outTime;
    private Status status;
}
