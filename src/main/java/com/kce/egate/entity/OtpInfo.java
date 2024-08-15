package com.kce.egate.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class OtpInfo {
    @Id
    private String _id;
    private String uniqueId;
    private String email;
    private Integer otp;
    private Long createdAt;
    private Long expireAt;
}
