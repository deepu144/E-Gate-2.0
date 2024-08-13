package com.kce.egate.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document
public class BatchEntry {
    @Id
    private String _id;
    private String uniqueId;
    private String rollNumber;
    private long totalEntry;
    private List<LocalDate> inDateList = new ArrayList<>();
    private List<LocalDate> outDateList = new ArrayList<>();
    private List<LocalTime> inTimeList = new ArrayList<>();
    private List<LocalTime> outTimeList = new ArrayList<>();
}
