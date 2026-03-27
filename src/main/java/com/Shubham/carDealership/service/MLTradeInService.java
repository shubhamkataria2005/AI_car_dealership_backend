// src/main/java/com/Shubham/carDealership/service/MLTradeInService.java
package com.Shubham.carDealership.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class MLTradeInService {

    @Value("${ml.models.base:src/main/resources/models/}")
    private String modelsBasePath;

    @Value("${ml.tradein.model:${ml.models.base}tradein_model_package/}")
    private String modelPath;

    private boolean modelAvailable = false;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        // Check if model files exist
        File modelFile = new File(modelPath + "tradein_model.pkl");
        File scriptFile = new File(modelPath + "predict_tradein.py");

        if (modelFile.exists() && scriptFile.exists()) {
            modelAvailable = true;
            System.out.println("✅ Trade-In ML Model loaded successfully!");
            System.out.println("   📁 Path: " + modelPath);
            System.out.println("   📄 Model: " + modelFile.getName());
            System.out.println("   🐍 Script: " + scriptFile.getName());
        } else {
            System.out.println("⚠️ Trade-In ML Model not found at: " + modelPath);
            System.out.println("   Using fallback calculation instead.");
            modelAvailable = false;
        }
    }

    /**
     * Predict trade-in value using ML model
     */
    public BigDecimal predictValue(String make, String model, int year, int mileage,
                                   String condition, String bodyType, String fuelType,
                                   String transmission, int owners, double engineSize) {

        if (!modelAvailable) {
            System.out.println("⚠️ ML model unavailable, using fallback calculation");
            return calculateFallbackValue(make, model, year, mileage, condition);
        }

        try {
            System.out.println("🤖 Calling ML model for trade-in prediction...");
            String result = callPythonPredictor(make, model, year, mileage, condition,
                    bodyType, fuelType, transmission, owners, engineSize);
            System.out.println("✅ ML prediction successful: $" + result);
            return new BigDecimal(result);
        } catch (Exception e) {
            System.err.println("❌ ML prediction failed: " + e.getMessage());
            return calculateFallbackValue(make, model, year, mileage, condition);
        }
    }

    /**
     * Call Python script for prediction
     */
    private String callPythonPredictor(String make, String model, int year, int mileage,
                                       String condition, String bodyType, String fuelType,
                                       String transmission, int owners, double engineSize) throws Exception {

        // Prepare input JSON
        Map<String, Object> input = new HashMap<>();
        input.put("make", make);
        input.put("model", model);
        input.put("year", year);
        input.put("mileage", mileage);
        input.put("condition", condition);
        input.put("body_type", bodyType);
        input.put("fuel_type", fuelType);
        input.put("transmission", transmission);
        input.put("owners", owners);
        input.put("engine_size", engineSize);

        String inputJson = objectMapper.writeValueAsString(input);

        // Execute Python script from the model folder
        String[] cmd = {
                "python3",
                modelPath + "predict_tradein.py",
                inputJson
        };

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(modelPath));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Read output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Python script failed with exit code: " + exitCode +
                    ", output: " + output);
        }

        // Parse JSON response
        String jsonOutput = output.toString();
        Map<String, Object> result = objectMapper.readValue(jsonOutput, Map.class);

        if (result.containsKey("success") && (boolean) result.get("success")) {
            return String.valueOf(((Number) result.get("predicted_value")).doubleValue());
        } else {
            String error = result.containsKey("message") ? (String) result.get("message") : "Unknown error";
            throw new RuntimeException(error);
        }
    }

    /**
     * Fallback calculation if ML model is unavailable
     */
    private BigDecimal calculateFallbackValue(String make, String model, int year, int mileage, String condition) {
        System.out.println("📊 Using fallback calculation for: " + make + " " + model);

        // Base values by brand (NZD)
        Map<String, Double> brandValues = new HashMap<>();
        brandValues.put("Toyota", 28000.0);
        brandValues.put("Honda", 26000.0);
        brandValues.put("Mazda", 27000.0);
        brandValues.put("Subaru", 29000.0);
        brandValues.put("Nissan", 25000.0);
        brandValues.put("Ford", 30000.0);
        brandValues.put("BMW", 55000.0);
        brandValues.put("Mercedes", 58000.0);
        brandValues.put("Audi", 52000.0);
        brandValues.put("Hyundai", 24000.0);
        brandValues.put("Kia", 23500.0);
        brandValues.put("Volkswagen", 31000.0);
        brandValues.put("Tesla", 65000.0);
        brandValues.put("Lamborghini", 350000.0);
        brandValues.put("Ferrari", 400000.0);
        brandValues.put("Porsche", 120000.0);

        double baseValue = brandValues.getOrDefault(make, 25000.0);

        // Age depreciation (10% per year, max 70%)
        int age = 2025 - year;
        double ageFactor = Math.max(0.3, 1.0 - (age * 0.10));

        // Mileage adjustment (reduce by up to 40%)
        double mileageFactor = Math.max(0.6, 1.0 - (mileage / 200000.0));

        // Condition factor
        Map<String, Double> conditionFactors = new HashMap<>();
        conditionFactors.put("Excellent", 1.0);
        conditionFactors.put("Very Good", 0.9);
        conditionFactors.put("Good", 0.8);
        conditionFactors.put("Fair", 0.65);
        conditionFactors.put("Poor", 0.5);
        double conditionFactor = conditionFactors.getOrDefault(condition, 0.7);

        double estimatedValue = baseValue * ageFactor * mileageFactor * conditionFactor;
        estimatedValue = Math.max(1000, Math.min(150000, estimatedValue));

        // Round to nearest $100
        estimatedValue = Math.round(estimatedValue / 100) * 100;

        System.out.println("   Fallback value: $" + estimatedValue);
        return BigDecimal.valueOf(estimatedValue);
    }

    public boolean isModelAvailable() {
        return modelAvailable;
    }
}