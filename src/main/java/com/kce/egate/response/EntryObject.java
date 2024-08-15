package com.kce.egate.response;

import com.kce.egate.enumeration.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public final class EntryObject {
    private String rollNumber;
    private LocalDate inDate;
    private LocalDate outDate;
    private LocalTime inTime;
    private LocalTime outTime;
    private Status status;
}
