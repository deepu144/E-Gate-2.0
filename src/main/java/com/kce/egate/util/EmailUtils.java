package com.kce.egate.util;

import com.kce.egate.request.EmailDetailRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailUtils {
    private static final Logger log = LoggerFactory.getLogger(EmailUtils.class);
    private final JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String sender;
    public void sendMimeMessage(EmailDetailRequest emailDetailRequest) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(sender);
            helper.setTo(emailDetailRequest.getRecipient());
            helper.setSubject(emailDetailRequest.getSubject());
            helper.setText(emailDetailRequest.getMsgBody(), true);
            log.debug("[SERVICE] Email sending to {}", emailDetailRequest.getRecipient());
            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("[SERVICE] Failed to send email", e);
        }
    }
}
