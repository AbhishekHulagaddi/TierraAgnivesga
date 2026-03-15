package com.tuition.controller;

import com.tuition.config.JwtUtil;
import com.tuition.model.User;
import com.tuition.repository.UserRepository;
import com.tuition.service.OtpService;
import com.tuition.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final SubscriptionService subscriptionService;

    // ──────────────────────────────────────────────
    // LOGIN
    // ──────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {

        String username    = credentials.get("username");
        String rawPassword = credentials.get("password");

        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }

        User user = userOpt.get();

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            return ResponseEntity.status(403).body(Map.of("error", "Account is deactivated. Contact admin."));
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }

        String jwt = jwtUtil.generateToken(user.getUsername());

        // Determine effective subscription status
        boolean isSubscribed = subscriptionService.isSubscribed(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("token",        jwt);
        response.put("userId",       user.getId());
        response.put("username",     user.getUsername());
        response.put("fullName",     user.getFullName());
        response.put("email",        user.getEmail());
        response.put("role",         user.getRole().name());
        response.put("phone",        user.getPhone() != null ? user.getPhone() : "");
        response.put("isSubscribed", isSubscribed);
        response.put("city",     user.getCity());
        response.put("college",     user.getCollege());

        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────
    // REGISTER
    // ──────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {

        String username = request.get("username");
        String email    = request.get("email");
        String password = request.get("password");
        String phone    = request.get("phone");
        String fullName = request.get("fullName");
        String city     = request.get("city");
        String college  = request.get("college");
        String otp      = request.get("otp");

        if (username == null || email == null || password == null || fullName == null
                || phone == null || city == null || college == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required"));
        }

        if (userRepository.existsByUsername(username.trim())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already taken"));
        }

        if (userRepository.existsByEmail(email.trim())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }

        Map<String, Object> otpResponse = otpService.verifyOtp(email, otp);
        Boolean status = (Boolean) otpResponse.get("status");
        if (!status) {
            return ResponseEntity.badRequest().body(Map.of("error", "OTP entered is INVALID!"));
        }

        User user = User.builder()
                .username(username.trim())
                .email(email.trim())
                .password(passwordEncoder.encode(password))
                .fullName(fullName.trim())
                .phone(phone.trim())
                .city(city.trim())
                .college(college.trim())
                .role(User.Role.STUDENT)
                .isActive(true)
                .isSubscribed(false)   // new student — not subscribed until admin activates
                .build();

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Registration successful! Please contact admin to activate your subscription."
        ));
    }

    // ──────────────────────────────────────────────
    // GET CURRENT USER
    // ──────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing token"));
        }

        String token    = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();
        boolean isSubscribed = subscriptionService.isSubscribed(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("id",           user.getId());
        response.put("username",     user.getUsername());
        response.put("fullName",     user.getFullName());
        response.put("email",        user.getEmail());
        response.put("role",         user.getRole().name());
        response.put("phone",        user.getPhone() != null ? user.getPhone() : "");
        response.put("isSubscribed", isSubscribed);

        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────
    // GENERATE OTP
    // ──────────────────────────────────────────────
    @PostMapping("/generate-otp")
    public ResponseEntity<?> generateOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email required"));
        }
        otpService.sendOtp(email);
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }
    
 // ──────────────────────────────────────────────
 // CHANGE PASSWORD (authenticated)
 // ──────────────────────────────────────────────
 @PostMapping("/change-password")
 public ResponseEntity<?> changePassword(@RequestBody Map<String, String> req) {
     String username       = req.get("username");
     String currentPassword = req.get("currentPassword");
     String newPassword    = req.get("newPassword");

     if (username == null || currentPassword == null || newPassword == null)
         return ResponseEntity.badRequest().body(Map.of("error", "All fields required"));
     if (newPassword.length() < 6)
         return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));

     Optional<User> userOpt = userRepository.findByUsername(username.trim());
     if (userOpt.isEmpty())
         return ResponseEntity.status(404).body(Map.of("error", "User not found"));

     User user = userOpt.get();
     if (!passwordEncoder.matches(currentPassword, user.getPassword()))
         return ResponseEntity.status(401).body(Map.of("error", "Current password is incorrect"));

     user.setPassword(passwordEncoder.encode(newPassword));
     userRepository.save(user);
     return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
 }

 // ──────────────────────────────────────────────
 // RESET PASSWORD via OTP (forgot password)
 // ──────────────────────────────────────────────
 @PostMapping("/reset-password")
 public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> req) {
     String email       = req.get("email");
     String otp         = req.get("otp");
     String newPassword = req.get("newPassword");

     if (email == null || otp == null || newPassword == null)
         return ResponseEntity.badRequest().body(Map.of("error", "All fields required"));
     if (newPassword.length() < 6)
         return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));

     // Verify OTP
     Map<String, Object> otpResult = otpService.verifyOtp(email, otp);
     if (!Boolean.TRUE.equals(otpResult.get("status")))
         return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired OTP"));

     Optional<User> userOpt = userRepository.findByEmail(email.trim());
     if (userOpt.isEmpty())
         return ResponseEntity.status(404).body(Map.of("error", "No account found with this email"));

     User user = userOpt.get();
     user.setPassword(passwordEncoder.encode(newPassword));
     userRepository.save(user);
     return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
 }
}
