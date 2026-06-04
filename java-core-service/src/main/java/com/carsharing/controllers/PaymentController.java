package com.carsharing.controllers;

import com.carsharing.entities.Payment;
import com.carsharing.services.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Create payment for a completed ride
     * POST /api/payments
     * Validates: Requirements 2.4
     */
    @PostMapping
    public ResponseEntity<?> createPayment(@RequestBody CreatePaymentRequest request) {
        logger.info("Creating payment: rideId={}, paymentMethod={}", request.rideId(), request.paymentMethod());
        try {
            Payment payment = paymentService.createPaymentForRide(
                request.rideId(),
                request.paymentMethod()
            );
            logger.info("Payment created successfully: id={}, rideId={}", payment.getId(), request.rideId());
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create payment: rideId={}, error={}", request.rideId(), e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Cannot create payment: rideId={}, error={}", request.rideId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Complete a payment
     * POST /api/payments/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completePayment(
            @PathVariable UUID id,
            @RequestBody CompletePaymentRequest request) {
        logger.info("Completing payment: id={}, transactionId={}", id, request.transactionId());
        try {
            Payment payment = paymentService.completePayment(id, request.transactionId());
            logger.info("Payment completed: id={}", id);
            return ResponseEntity.ok(payment);
        } catch (IllegalArgumentException e) {
            logger.warn("Payment not found for completion: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Cannot complete payment: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Fail a payment
     * POST /api/payments/{id}/fail
     */
    @PostMapping("/{id}/fail")
    public ResponseEntity<?> failPayment(@PathVariable UUID id) {
        logger.info("Failing payment: id={}", id);
        try {
            Payment payment = paymentService.failPayment(id);
            logger.info("Payment failed: id={}", id);
            return ResponseEntity.ok(payment);
        } catch (IllegalArgumentException e) {
            logger.warn("Payment not found for failure: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Refund a payment
     * POST /api/payments/{id}/refund
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable UUID id) {
        logger.info("Refunding payment: id={}", id);
        try {
            Payment payment = paymentService.refundPayment(id);
            logger.info("Payment refunded: id={}", id);
            return ResponseEntity.ok(payment);
        } catch (IllegalArgumentException e) {
            logger.warn("Payment not found for refund: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Cannot refund payment: id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get payment by ID
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable UUID id) {
        logger.info("Getting payment by ID: id={}", id);
        try {
            var paymentOptional = paymentService.getPaymentById(id);
            if (paymentOptional.isPresent()) {
                return ResponseEntity.ok(paymentOptional.get());
            } else {
                logger.warn("Payment not found: id={}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Payment not found"));
            }
        } catch (Exception e) {
            logger.error("Error getting payment: id={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get payments by user ID
     * GET /api/payments/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getPaymentsByUserId(@PathVariable UUID userId) {
        logger.info("Getting payments for user: userId={}", userId);
        List<Payment> payments = paymentService.getPaymentsByUserId(userId);
        logger.info("Retrieved {} payments for user: userId={}", payments.size(), userId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payments by ride ID
     * GET /api/payments/ride/{rideId}
     */
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<List<Payment>> getPaymentsByRideId(@PathVariable UUID rideId) {
        logger.info("Getting payments for ride: rideId={}", rideId);
        List<Payment> payments = paymentService.getPaymentsByRideId(rideId);
        logger.info("Retrieved {} payments for ride: rideId={}", payments.size(), rideId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payments by status
     * GET /api/payments/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable String status) {
        logger.info("Getting payments by status: status={}", status);
        List<Payment> payments = paymentService.getPaymentsByStatus(status);
        logger.info("Retrieved {} payments with status: status={}", payments.size(), status);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get total revenue
     * GET /api/payments/revenue/total
     */
    @GetMapping("/revenue/total")
    public ResponseEntity<?> getTotalRevenue() {
        logger.info("Getting total revenue");
        try {
            var revenue = paymentService.getTotalRevenue();
            logger.info("Total revenue retrieved: {}", revenue);
            return ResponseEntity.ok(new RevenueResponse(revenue));
        } catch (Exception e) {
            logger.error("Error getting total revenue: error={}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get total revenue by user
     * GET /api/payments/revenue/user/{userId}
     */
    @GetMapping("/revenue/user/{userId}")
    public ResponseEntity<?> getTotalRevenueByUser(@PathVariable UUID userId) {
        logger.info("Getting total revenue by user: userId={}", userId);
        try {
            var revenue = paymentService.getTotalRevenueByUser(userId);
            logger.info("Total revenue by user retrieved: userId={}, revenue={}", userId, revenue);
            return ResponseEntity.ok(new RevenueResponse(revenue));
        } catch (Exception e) {
            logger.error("Error getting total revenue by user: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get payment statistics
     * GET /api/payments/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getPaymentStatistics() {
        logger.info("Getting payment statistics");
        try {
            var statistics = paymentService.getPaymentStatistics();
            logger.info("Payment statistics retrieved");
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error getting payment statistics: error={}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Request/Response records

    public record CreatePaymentRequest(UUID rideId, String paymentMethod) {}

    public record CompletePaymentRequest(String transactionId) {}

    public record RevenueResponse(java.math.BigDecimal revenue) {}

    public record ErrorResponse(String message) {}
}