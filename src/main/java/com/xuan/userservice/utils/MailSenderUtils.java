package com.xuan.userservice.utils;

import com.xuan.userservice.entity.properties.MailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailSenderUtils {

    private final MailProperties mailProperties;
    private final JavaMailSender mailSender;

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 正文
     */
    public void sendText(String to, String subject, String content) {
        send(to, subject, content, false);
    }

    /**
     * 发送 HTML 邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content HTML 正文
     */
    public void sendHtml(String to, String subject, String content) {
        send(to, subject, content, true);
    }

    private void send(String to, String subject, String content, boolean html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getUsername());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, html);
            mailSender.send(message);
            log.info("邮件发送成功 -> to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败 -> to={}, subject={}", to, subject, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
}
