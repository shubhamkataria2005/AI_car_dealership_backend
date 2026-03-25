package com.Shubham.carDealership.controller;

import com.Shubham.carDealership.config.JwtUtil;
import com.Shubham.carDealership.dto.CarRequest;
import com.Shubham.carDealership.dto.CarResponse;
import com.Shubham.carDealership.model.User;
import com.Shubham.carDealership.repository.UserRepository;
import com.Shubham.carDealership.service.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cars")
@CrossOrigin(origins = "http://localhost:5173")
public class CarController {

    @Autowired
    private CarService carService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User getAuthenticatedUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            return userRepository.findById(userId).orElse(null);
        }
        return null;
    }

    @PostMapping("/list")
    public ResponseEntity<?> listCar(@RequestBody CarRequest request, HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser(httpRequest);
        if (user == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Please login first");
            return ResponseEntity.ok(response);
        }

        CarResponse car = carService.listCar(request, user);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("car", car);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllCars() {
        List<CarResponse> cars = carService.getAllAvailableCars();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cars", cars);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-cars")
    public ResponseEntity<?> getMyCars(HttpServletRequest request) {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Please login first");
            return ResponseEntity.ok(response);
        }

        List<CarResponse> cars = carService.getUserCars(user.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cars", cars);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCarById(@PathVariable Long id) {
        try {
            CarResponse car = carService.getCarById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("car", car);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Car not found");
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCars(
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String bodyType,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) Double maxPrice) {

        List<CarResponse> cars = carService.searchCars(make, bodyType, fuel, maxPrice);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cars", cars);
        return ResponseEntity.ok(response);
    }
}