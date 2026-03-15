package com.tuition.service;

import com.tuition.model.Enrollment;
import com.tuition.model.Subscription;
import com.tuition.model.Subscription.SubscriptionStatus;
import com.tuition.model.User;
import com.tuition.repository.EnrollmentRepository;
import com.tuition.repository.SubjectRepository;
import com.tuition.repository.SubscriptionRepository;
import com.tuition.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EmailService emailService;

    /**
     * Check if a user currently has an active subscription.
     * ADMIN and TUTOR are always treated as subscribed.
     */
    public boolean isSubscribed(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.TUTOR) return true;
        return subscriptionRepository.findActiveSubscription(userId, LocalDate.now()).isPresent();
    }

    /**
     * Admin activates a full-course subscription for a student.
     * - Cancels any previously active subscription
     * - Auto-enrolls student in ALL active subjects
     * - Sends activation email to student
     */
    @Transactional
    public Subscription activateSubscription(Long studentId, Long adminId, String planName,
                                              LocalDate startDate, LocalDate endDate,
                                              Double amountPaid, String paymentReference) {
        User student = userRepository.findById(studentId).orElseThrow();
        User admin   = userRepository.findById(adminId).orElseThrow();

        // Cancel any previously active subscriptions
        List<Subscription> existing = subscriptionRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        existing.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .forEach(s -> {
                    s.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(s);
                });

        // Create new subscription
        Subscription sub = Subscription.builder()
                .student(student)
                .activatedBy(admin)
                .planName(planName)
                .startDate(startDate)
                .endDate(endDate)
                .amountPaid(amountPaid)
                .paymentReference(paymentReference)
                .status(SubscriptionStatus.ACTIVE)
                .build();
        subscriptionRepository.save(sub);

        // Update user's subscribed flag
        student.setIsSubscribed(true);
        userRepository.save(student);

        // Auto-enroll student in all active subjects (full coaching model - not subject-wise)
        enrollStudentInAllSubjects(student);

        // Send email notification using original EmailService.sendHtmlMail
        try {
            String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:6px;overflow:hidden;">

				  <!-- Header -->
				  <div style="background:#6e1428;padding:20px;text-align:center;">
				    <h2 style="color:#ffffff;margin:0;">Tierra Agnivesha Entrance Coaching Classes</h2>
				    <p style="color:#c5cae9;margin:6px 0 0;font-size:14px;">
				      Online Coaching for AIAPGET Aspirants
				    </p>
				  </div>
				
				  <!-- Body -->
				  <div style="padding:30px;background:#f9f9f9;">
				  
				    <h3 style="color:#1a237e;margin-top:0;">Welcome, %s!</h3>
				
				    <p style="color:#333;font-size:15px;">
				      Your subscription has been <strong>successfully activated</strong>. 
				      You now have full access to the following resources:
				    </p>
				
				    <ul style="color:#333;line-height:1.9;font-size:14px;">
				      <li>✅ All Online Classes</li>
				      <li>✅ Subject-wise MCQ Tests</li>
				      <li>✅ Weekly Combined Mock Tests</li>
				      <li>✅ Study Notes & Materials</li>
				    </ul>
				
				    <!-- Plan Box -->
				    <div style="background:#e8eaf6;border-radius:8px;padding:15px;margin:25px 0;">
				      <p style="margin:0;"><strong>Subscription Plan:</strong> %s</p>
				      <p style="margin:6px 0 0;"><strong>Valid Until:</strong> %s</p>
				    </div>
				
				    <p style="color:#333;font-size:14px;">
				      You can now log in to the student portal and start learning.
				    </p>
				
				    <p style="color:#333;font-size:14px;">
				      If you face any issues accessing your account, please contact us.
				    </p>
				
				    <p style="font-size:14px;">
				      📞 <strong>Support:</strong> +91 77953 82064
				    </p>
				
				    <p style="margin-top:20px;font-size:14px;">
				      Thank you for choosing <strong>Tierra Agnivesha Entrance Coaching Classes</strong>.
				      <br>
				      We wish you success in your AIAPGET preparation.
				    </p>
				
				    <hr style="margin:25px 0;border:none;border-top:1px solid #ddd;">
				
				    <p style="font-size:12px;color:#777;">
				      <strong>Note:</strong> This is an automatically generated email. 
				      Please do not reply to this message.
				    </p>
				
				  </div>
				
				  <!-- Footer -->
				  <div style="background:#6e1428;padding:12px;text-align:center;">
				    <p style="color:#c5cae9;margin:0;font-size:12px;">
				      © 2025 Tierra Agnivesha Entrance Coaching Classes. All rights reserved.
				    </p>
				  </div>
				
				</div>

                """.formatted(student.getFullName(), planName, endDate.toString());

            emailService.sendHtmlMail(student.getEmail(),
                    "🎉 Subscription Activated – Tierra Agnivesha Entrance Coaching Classes", html);
        } catch (Exception e) {
            log.warn("Subscription activated but email failed for {}: {}", student.getEmail(), e.getMessage());
        }

        return sub;
    }

    /**
     * Enroll the student in every active subject (full coaching model).
     * Skips subjects where the student is already enrolled.
     */
    private void enrollStudentInAllSubjects(User student) {
        subjectRepository.findByIsActive(true).forEach(subject -> {
            if (!enrollmentRepository.existsByStudentIdAndSubjectId(student.getId(), subject.getId())) {
                Enrollment enrollment = Enrollment.builder()
                        .student(student)
                        .subject(subject)
                        .isActive(true)
                        .build();
                enrollmentRepository.save(enrollment);
                log.info("Enrolled student {} in subject {}", student.getUsername(), subject.getName());
            }
        });
    }

    /**
     * Admin cancels a student's subscription by subscription ID.
     */
    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId).orElseThrow();
        sub.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(sub);

        // Flip user's subscribed flag only if no other active subscription remains
        User student = sub.getStudent();
        boolean hasOtherActive = subscriptionRepository
                .findActiveSubscription(student.getId(), LocalDate.now()).isPresent();
        if (!hasOtherActive) {
            student.setIsSubscribed(false);
            userRepository.save(student);
            log.info("Student {} subscription cancelled, access revoked", student.getUsername());
        }
    }

    /**
     * Sync isSubscribed flag for ALL students.
     * Call via admin endpoint or scheduled task to fix any drift.
     */
    @Transactional
    public void syncSubscriptionStatuses() {
        userRepository.findByRole(User.Role.STUDENT).forEach(student -> {

            boolean active = subscriptionRepository
                    .findActiveSubscription(student.getId(), LocalDate.now())
                    .isPresent();

            if (active != student.getIsSubscribed()) {
                student.setIsSubscribed(active);
                userRepository.save(student);
            }
        });

        log.info("Subscription status sync completed");
    }
}
