// src/main/java/com/Shubham/carDealership/service/TradeInService.java
package com.Shubham.carDealership.service;

import com.Shubham.carDealership.model.TradeIn;
import com.Shubham.carDealership.repository.TradeInRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TradeInService {

    @Autowired
    private TradeInRepository tradeInRepository;

    public BigDecimal calculateEstimatedValue(String make, String model, Integer year, Integer mileage, String condition) {
        Map<String, Double> brandMultipliers = new HashMap<>();
        brandMultipliers.put("Toyota", 0.85);
        brandMultipliers.put("Honda", 0.82);
        brandMultipliers.put("Ford", 0.75);
        brandMultipliers.put("BMW", 0.70);
        brandMultipliers.put("Mercedes", 0.72);
        brandMultipliers.put("Audi", 0.71);
        brandMultipliers.put("Lamborghini", 0.85);
        brandMultipliers.put("Ferrari", 0.88);
        brandMultipliers.put("Rolls-Royce", 0.80);
        brandMultipliers.put("Bentley", 0.78);
        brandMultipliers.put("Bugatti", 0.75);
        brandMultipliers.put("Pagani", 0.82);

        Map<String, Double> conditionMultipliers = new HashMap<>();
        conditionMultipliers.put("EXCELLENT", 1.0);
        conditionMultipliers.put("GOOD", 0.85);
        conditionMultipliers.put("FAIR", 0.70);
        conditionMultipliers.put("POOR", 0.50);

        double brandMulti = brandMultipliers.getOrDefault(make, 0.65);
        double conditionMulti = conditionMultipliers.getOrDefault(condition, 0.70);

        int age = LocalDateTime.now().getYear() - year;
        double ageDepreciation = Math.max(0.3, 1.0 - (age * 0.10));

        double estimatedValue = 30000 * brandMulti * ageDepreciation * conditionMulti;
        double mileageAdjustment = Math.max(0.6, 1.0 - (mileage / 200000.0));
        estimatedValue = estimatedValue * mileageAdjustment;
        estimatedValue = Math.max(estimatedValue, 1000);

        return BigDecimal.valueOf(Math.round(estimatedValue / 100) * 100);
    }

    public boolean regoExists(String rego) {
        return tradeInRepository.existsByRego(rego.toUpperCase());
    }

    public TradeIn getTradeInByRego(String rego) {
        return tradeInRepository.findByRego(rego.toUpperCase()).orElse(null);
    }

    public TradeIn createTradeIn(Long userId, Long carId, String rego, String make, String model,
                                 Integer year, Integer mileage, String condition, String notes) {
        if (regoExists(rego)) {
            throw new RuntimeException("A trade-in request for this vehicle (Rego: " + rego + ") already exists.");
        }

        TradeIn tradeIn = new TradeIn();
        tradeIn.setUserId(userId);
        tradeIn.setCarId(carId);
        tradeIn.setRego(rego.toUpperCase());
        tradeIn.setTradeMake(make);
        tradeIn.setTradeModel(model);
        tradeIn.setTradeYear(year);
        tradeIn.setTradeMileage(mileage);
        tradeIn.setTradeCondition(condition);
        tradeIn.setNotes(notes);
        tradeIn.setEstimatedValue(calculateEstimatedValue(make, model, year, mileage, condition));
        tradeIn.setStatus("PENDING");
        tradeIn.setCreatedAt(LocalDateTime.now());
        tradeIn.setUpdatedAt(LocalDateTime.now());

        return tradeInRepository.save(tradeIn);
    }

    public TradeIn approveTradeIn(Long tradeInId, BigDecimal finalValue) {
        TradeIn tradeIn = tradeInRepository.findById(tradeInId).orElse(null);
        if (tradeIn != null) {
            tradeIn.setFinalValue(finalValue);
            tradeIn.setStatus("APPROVED");
            tradeIn.setUpdatedAt(LocalDateTime.now());
            return tradeInRepository.save(tradeIn);
        }
        return null;
    }

    public TradeIn rejectTradeIn(Long tradeInId, String reason) {
        TradeIn tradeIn = tradeInRepository.findById(tradeInId).orElse(null);
        if (tradeIn != null) {
            tradeIn.setStatus("REJECTED");
            tradeIn.setNotes(reason);
            tradeIn.setUpdatedAt(LocalDateTime.now());
            return tradeInRepository.save(tradeIn);
        }
        return null;
    }

    public List<TradeIn> getUserTradeIns(Long userId) {
        return tradeInRepository.findByUserId(userId);
    }

    public List<TradeIn> getPendingTradeIns() {
        return tradeInRepository.findByStatus("PENDING");
    }

    public TradeIn completeTradeIn(Long tradeInId) {
        TradeIn tradeIn = tradeInRepository.findById(tradeInId).orElse(null);
        if (tradeIn != null) {
            tradeIn.setStatus("COMPLETED");
            tradeIn.setUpdatedAt(LocalDateTime.now());
            return tradeInRepository.save(tradeIn);
        }
        return null;
    }
}