package com.carsharing.services;

import com.carsharing.entities.Payment;
import com.carsharing.entities.Ride;
import com.carsharing.repositories.PaymentRepository;
import com.carsharing.repositories.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RideRepository rideRepository;
    private final UserService userService;

    public PaymentService(PaymentRepository paymentRepository, RideRepository rideRepository, UserService userService) {
        this.paymentRepository = paymentRepository;
        this.rideRepository = rideRepository;
        this.userService = userService;
    }

    @Transactional
    public Payment createPaymentForRide(UUID rideId, String paymentMethod) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        
        if (!ride.isCompleted()) {
            throw new IllegalStateException("Ride must be completed to create payment");
        }
        
        if (!userService.isUserActive(ride.getUserId())) {
            throw new IllegalArgumentException("User not found or inactive");
        }
        
        List<Payment> existingPayments = paymentRepository.findByRideId(rideId);
        if (!existingPayments.isEmpty()) {
            throw new IllegalStateException("Payment already exists for this ride");
        }
        
        Payment payment = new Payment(rideId, ride.getUserId(), ride.getTotalCost());
        payment.setPaymentMethod(paymentMethod);
        
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment completePayment(UUID paymentId, String transactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        
        if (!payment.isPending()) {
            throw new IllegalStateException("Payment is not pending");
        }
        
        payment.completePayment(payment.getPaymentMethod(), transactionId);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment failPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        
        payment.failPayment();
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refundPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        
        if (!payment.isCompleted()) {
            throw new IllegalStateException("Only completed payments can be refunded");
        }
        
        payment.refundPayment();
        return paymentRepository.save(payment);
    }

    public Optional<Payment> getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public List<Payment> getPaymentsByUserId(UUID userId) {
        return paymentRepository.findByUserId(userId);
    }

    public List<Payment> getPaymentsByRideId(UUID rideId) {
        return paymentRepository.findByRideId(rideId);
    }

    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }

    public BigDecimal getTotalRevenue() {
        List<Payment> completedPayments = paymentRepository.findByStatus("completed");
        return completedPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalRevenueByUser(UUID userId) {
        List<Payment> userPayments = paymentRepository.findByUserId(userId);
        return userPayments.stream()
                .filter(Payment::isCompleted)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Payment> getPaymentsOlderThan(LocalDateTime cutoffDate) {
        return paymentRepository.findAll().stream()
                .filter(payment -> payment.getCreatedAt().isBefore(cutoffDate))
                .toList();
    }

    public boolean isPaymentEligibleForLongTermRetention(Payment payment) {
        LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5);
        return payment.getCreatedAt().isBefore(fiveYearsAgo);
    }

    public PaymentStatistics getPaymentStatistics() {
        List<Payment> allPayments = paymentRepository.findAll();
        
        long totalPayments = allPayments.size();
        long completedPayments = allPayments.stream().filter(Payment::isCompleted).count();
        long pendingPayments = allPayments.stream().filter(Payment::isPending).count();
        long failedPayments = allPayments.stream().filter(p -> "failed".equals(p.getStatus())).count();
        long refundedPayments = allPayments.stream().filter(p -> "refunded".equals(p.getStatus())).count();
        
        BigDecimal totalAmount = allPayments.stream()
                .filter(Payment::isCompleted)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new PaymentStatistics(
                totalPayments, completedPayments, pendingPayments, 
                failedPayments, refundedPayments, totalAmount
        );
    }

    public record PaymentStatistics(
            long totalPayments,
            long completedPayments,
            long pendingPayments,
            long failedPayments,
            long refundedPayments,
            BigDecimal totalAmount
    ) {}
}