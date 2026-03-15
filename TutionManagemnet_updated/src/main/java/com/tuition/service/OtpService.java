package com.tuition.service;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tuition.model.EmailOtp;
import com.tuition.model.OTPRequest;
import com.tuition.repository.EmailOtpRepository;




@Service
public class OtpService {

    @Autowired
    private EmailOtpRepository repo;

    @Autowired
    private EmailService emailService;

    private Map<String,Object> response(boolean status,String msg,Object data){
        Map<String,Object> map=new HashMap<>();
        map.put("status",status);
        map.put("message",msg);
        map.put("data",data);
        return map;
    }

    // 🔹 generate OTP
    public String generateOtp() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }

    // 🔹 send OTP mail
    public Map<String, Object> sendOtp(String mail) {

        try {
            String otp = generateOtp();

            EmailOtp entity = new EmailOtp();
            entity.setEmail(mail);
            entity.setOtp(otp);
            entity.setVerified(false);
            entity.setExpiryTime(LocalDateTime.now().plusMinutes(5));

            repo.save(entity);

            String html = """
                <div style='font-family:Arial'>
                <h2>Email Verification OTP</h2>
                <p>Your OTP is:</p>
                <h1>%s</h1>
                <p>Valid for 5 minutes</p>
                </div>
                """.formatted(otp);

            emailService.sendHtmlMail(mail, "OTP Verification", html);

            return response(true, "OTP sent successfully", null);

        } catch (Exception e) {

            return response(false, "Failed to send OTP email", e.getMessage());
        }
    }

    // 🔹 verify OTP
    public Map<String,Object> verifyOtp(String email, String otp) {

        Optional<EmailOtp> optional =
                repo.findTopByEmailOrderByExpiryTimeDesc(email);

        if (optional.isEmpty())
            return response(false,"OTP not found",null);

        EmailOtp record = optional.get();

        if (record.getExpiryTime().isBefore(LocalDateTime.now()))
            return response(false,"OTP expired",null);

        if (!record.getOtp().equals(otp))
            return response(false,"Invalid OTP",null);

        record.setVerified(true);
        repo.save(record);

        return response(true,"OTP verified",null);
    }
}
