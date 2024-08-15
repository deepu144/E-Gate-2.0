package com.kce.egate.util;

import com.kce.egate.request.EmailDetailRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailUtils {
    private final JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String sender;
    public boolean sendMimeMessage(EmailDetailRequest emailDetailRequest) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(sender);
            helper.setTo(emailDetailRequest.getRecipient());
            helper.setSubject(emailDetailRequest.getSubject());
            helper.setText(emailDetailRequest.getMsgBody(), true);
            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
