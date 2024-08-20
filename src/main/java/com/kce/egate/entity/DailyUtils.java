package com.kce.egate.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
public class DailyUtils {
    private String _id;
    private String uniqueId;
    private LocalDate today;
    private Long studentInCount;
    private Long studentOutCount;
    private Long staffInCount;
    private Long staffOutCount;
}
