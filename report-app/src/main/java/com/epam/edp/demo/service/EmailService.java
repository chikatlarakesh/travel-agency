package com.epam.edp.demo.service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SesClient sesClient;

    @Value("${report.mail.recipient}")
    private String recipient;

    @Value("${report.mail.from}")
    private String from;

    public void sendReportEmail(String subject, String body,
                                byte[] excelBytes, String excelName,
                                byte[] csvBytes,   String csvName,
                                byte[] pdfBytes,   String pdfName) {
        try {
            byte[] rawEmail = buildRawEmail(subject, body, excelBytes, excelName, csvBytes, csvName, pdfBytes, pdfName);

            sesClient.sendRawEmail(SendRawEmailRequest.builder()
                    .rawMessage(RawMessage.builder()
                            .data(SdkBytes.fromByteArray(rawEmail))
                            .build())
                    .build());

            log.info("email.sent to={} subject={}", recipient, subject);
        } catch (Exception e) {
            log.error("email.error subject={} reason={}", subject, e.getMessage(), e);
        }
    }

    private byte[] buildRawEmail(String subject, String body,
                                  byte[] excelBytes, String excelName,
                                  byte[] csvBytes,   String csvName,
                                  byte[] pdfBytes,   String pdfName)
            throws MessagingException, IOException {

        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(from));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        message.setSubject(subject, "UTF-8");

        MimeMultipart multipart = new MimeMultipart();

        // Plain text body
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body, "UTF-8");
        multipart.addBodyPart(textPart);

        // Excel attachment
        multipart.addBodyPart(attachmentPart(excelBytes, excelName,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        // CSV attachment
        multipart.addBodyPart(attachmentPart(csvBytes, csvName, "text/csv"));

        // PDF attachment
        multipart.addBodyPart(attachmentPart(pdfBytes, pdfName, "application/pdf"));

        message.setContent(multipart);
        message.saveChanges();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return out.toByteArray();
    }

    private MimeBodyPart attachmentPart(byte[] data, String filename, String mimeType)
            throws MessagingException {
        MimeBodyPart part = new MimeBodyPart();
        part.setContent(data, mimeType);
        part.setFileName(filename);
        return part;
    }
}
