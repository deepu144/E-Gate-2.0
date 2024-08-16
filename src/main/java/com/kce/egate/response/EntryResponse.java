package com.kce.egate.response;

import com.kce.egate.enumeration.Status;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class EntryResponse {
    private String rollNumber;
    private String name;
    private String dept;
    private String batch;
    private LocalDate outDate;
    private LocalDate inDate;
    private LocalTime outTime;
    private LocalTime inTime;
    private Status status;
}
