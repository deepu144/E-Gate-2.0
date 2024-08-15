package com.kce.egate.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailDetailRequest {
    private String recipient;
    private String msgBody;
    private String subject;
}
