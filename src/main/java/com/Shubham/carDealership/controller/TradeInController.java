// src/main/java/com/Shubham/carDealership/controller/TradeInController.java
package com.Shubham.carDealership.controller;

import com.Shubham.carDealership.config.JwtUtil;
import com.Shubham.carDealership.model.TradeIn;
import com.Shubham.carDealership.model.User;
import com.Shubham.carDealership.repository.UserRepository;
import com.Shubham.carDealership.service.TradeInService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trade-in")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class TradeInController {

    @Autowired
    private TradeInService tradeInService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User getAuthenticatedUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.extractUserId(token);
                return userRepository.findById(userId).orElse(null);
            }
        }
        return null;
    }

    private boolean isAdmin(User user) {
        return user != null && ("ADMIN".equals(user.getRole()) || "SUPER_ADMIN".equals(user.getRole()));
    }

    @PostMapping("/estimate")
    public ResponseEntity<?> estimateTradeIn(@RequestBody Map<String, Object> request) {
        String make = (String) request.get("make");
        String model = (String) request.get("model");
        Integer year = (Integer) request.get("year");
        Integer mileage = (Integer) request.get("mileage");
        String condition = (String) request.get("condition");

        BigDecimal estimate = tradeInService.calculateEstimatedValue(make, model, year, mileage, condition);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("estimatedValue", estimate);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/request")
    public ResponseEntity<?> createTradeIn(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser(httpRequest);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Please login first"));
        }

        String rego = (String) request.get("rego");
        if (rego == null || rego.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Registration plate (rego) is required"));
        }

        if (tradeInService.regoExists(rego)) {
            TradeIn existing = tradeInService.getTradeInByRego(rego);
            String status = existing.getStatus();
            String message = "";

            if ("PENDING".equals(status)) {
                message = "A trade-in request for this vehicle (Rego: " + rego + ") is already pending review.";
            } else if ("APPROVED".equals(status)) {
                message = "This vehicle (Rego: " + rego + ") has already been approved for trade-in.";
            } else if ("REJECTED".equals(status)) {
                message = "This vehicle (Rego: " + rego + ") was previously rejected.";
            } else if ("COMPLETED".equals(status)) {
                message = "This vehicle (Rego: " + rego + ") has already been traded in.";
            }

            return ResponseEntity.ok(Map.of("success", false, "message", message, "existingTradeIn", existing));
        }

        Long carId = request.get("carId") != null ? Long.valueOf(request.get("carId").toString()) : null;
        String make = (String) request.get("make");
        String model = (String) request.get("model");
        Integer year = (Integer) request.get("year");
        Integer mileage = (Integer) request.get("mileage");
        String condition = (String) request.get("condition");
        String notes = (String) request.get("notes");

        TradeIn tradeIn = tradeInService.createTradeIn(user.getId(), carId, rego, make, model, year, mileage, condition, notes);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tradeIn", tradeIn);
        response.put("message", "Trade-in request submitted successfully!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-trade-ins")
    public ResponseEntity<?> getUserTradeIns(HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser(httpRequest);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Please login first"));
        }

        List<TradeIn> tradeIns = tradeInService.getUserTradeIns(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "tradeIns", tradeIns));
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingTradeIns(HttpServletRequest httpRequest) {
        User admin = getAuthenticatedUser(httpRequest);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        List<TradeIn> pending = tradeInService.getPendingTradeIns();
        return ResponseEntity.ok(Map.of("success", true, "tradeIns", pending));
    }

    @PutMapping("/admin/{tradeInId}/approve")
    public ResponseEntity<?> approveTradeIn(@PathVariable Long tradeInId,
                                            @RequestBody Map<String, Object> request,
                                            HttpServletRequest httpRequest) {
        User admin = getAuthenticatedUser(httpRequest);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        BigDecimal finalValue = BigDecimal.valueOf(Double.parseDouble(request.get("finalValue").toString()));
        TradeIn tradeIn = tradeInService.approveTradeIn(tradeInId, finalValue);

        if (tradeIn == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Trade-in not found"));
        }

        return ResponseEntity.ok(Map.of("success", true, "tradeIn", tradeIn));
    }

    @PutMapping("/admin/{tradeInId}/reject")
    public ResponseEntity<?> rejectTradeIn(@PathVariable Long tradeInId,
                                           @RequestBody Map<String, Object> request,
                                           HttpServletRequest httpRequest) {
        User admin = getAuthenticatedUser(httpRequest);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        String reason = (String) request.get("reason");
        TradeIn tradeIn = tradeInService.rejectTradeIn(tradeInId, reason);

        if (tradeIn == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Trade-in not found"));
        }

        return ResponseEntity.ok(Map.of("success", true, "tradeIn", tradeIn));
    }
}