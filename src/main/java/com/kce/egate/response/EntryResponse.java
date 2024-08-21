package com.kce.egate.response;

import com.kce.egate.enumeration.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
