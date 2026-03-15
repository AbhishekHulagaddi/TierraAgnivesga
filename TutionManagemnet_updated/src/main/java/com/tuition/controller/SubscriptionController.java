package com.tuition.controller;

import com.tuition.model.Subscription;
import com.tuition.repository.SubscriptionRepository;
import com.tuition.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;

    // ---- ADMIN: Activate subscription for a student ----
    @PostMapping("/admin/subscriptions")
    public ResponseEntity<?> activateSubscription(@RequestBody Map<String, Object> req) {
        Long studentId = Long.parseLong(req.get("studentId").toString());
        Long adminId   = Long.parseLong(req.get("adminId").toString());
        String planName = req.getOrDefault("planName", "AIAPGET Full Course").toString();
        LocalDate startDate = LocalDate.parse(req.get("startDate").toString());
        LocalDate endDate   = LocalDate.parse(req.get("endDate").toString());
        Double amountPaid   = Double.parseDouble(req.getOrDefault("amountPaid", "0").toString());
        String paymentRef   = req.getOrDefault("paymentReference", "").toString();

        Subscription sub = subscriptionService.activateSubscription(
                studentId, adminId, planName, startDate, endDate, amountPaid, paymentRef);

        return ResponseEntity.ok(Map.of(
            "message", "Subscription activated successfully. Student enrolled in all subjects.",
            "subscriptionId", sub.getId(),
            "endDate", sub.getEndDate()
        ));
    }

    // ---- ADMIN: Cancel subscription ----
    @PutMapping("/admin/subscriptions/{id}/cancel")
    public ResponseEntity<?> cancelSubscription(@PathVariable Long id) {
        subscriptionService.cancelSubscription(id);
        return ResponseEntity.ok(Map.of("message", "Subscription cancelled"));
    }

    // ---- ADMIN: Sync all subscription statuses ----
    @PostMapping("/admin/subscriptions/sync")
    public ResponseEntity<?> syncSubscriptions() {
        subscriptionService.syncSubscriptionStatuses();
        return ResponseEntity.ok(Map.of("message", "Subscription statuses synced"));
    }

    // ---- ADMIN: Get all subscriptions ----
    @GetMapping("/admin/subscriptions")
    public ResponseEntity<?> getAllSubscriptions() {
        List<Subscription> all = subscriptionRepository.findAll();
        return ResponseEntity.ok(all.stream().map(this::mapSub).collect(Collectors.toList()));
    }

    // ---- STUDENT: Get own subscription status ----
    @GetMapping("/student/subscription/{studentId}")
    public ResponseEntity<?> getStudentSubscription(@PathVariable Long studentId) {
        boolean subscribed = subscriptionService.isSubscribed(studentId);
        List<Subscription> subs = subscriptionRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        return ResponseEntity.ok(Map.of(
            "isSubscribed", subscribed,
            "subscriptions", subs.stream().map(this::mapSub).collect(Collectors.toList())
        ));
    }

    // ---- CHECK: Lightweight subscription check (used by frontend guards) ----
    @GetMapping("/student/subscription/{studentId}/check")
    public ResponseEntity<?> checkSubscription(@PathVariable Long studentId) {
        return ResponseEntity.ok(Map.of("isSubscribed", subscriptionService.isSubscribed(studentId)));
    }

    private Map<String, Object> mapSub(Subscription s) {
        return Map.of(
            "id", s.getId(),
            "studentName", s.getStudent().getFullName(),
            "studentId", s.getStudent().getId(),
            "planName", s.getPlanName(),
            "startDate", s.getStartDate(),
            "endDate", s.getEndDate(),
            "status", s.getStatus().name(),
            "amountPaid", s.getAmountPaid() != null ? s.getAmountPaid() : 0,
            "paymentReference", s.getPaymentReference() != null ? s.getPaymentReference() : "",
            "createdAt", s.getCreatedAt()
        );
    }
}
