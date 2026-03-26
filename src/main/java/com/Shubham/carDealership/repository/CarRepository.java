// src/main/java/com/Shubham/carDealership/repository/CarRepository.java
package com.Shubham.carDealership.repository;

import com.Shubham.carDealership.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findBySellerId(Long sellerId);
    List<Car> findByStatus(String status);
    List<Car> findByCarSource(String carSource);
    List<Car> findByCarSourceAndStatus(String carSource, String status);

    @Query("SELECT c FROM Car c WHERE " +
            "(:make IS NULL OR c.make = :make) AND " +
            "(:bodyType IS NULL OR c.bodyType = :bodyType) AND " +
            "(:fuel IS NULL OR c.fuel = :fuel) AND " +
            "(:maxPrice IS NULL OR c.price <= :maxPrice) AND " +
            "(:carSource IS NULL OR c.carSource = :carSource) AND " +
            "c.status = 'AVAILABLE'")
    List<Car> searchCars(@Param("make") String make,
                         @Param("bodyType") String bodyType,
                         @Param("fuel") String fuel,
                         @Param("maxPrice") Double maxPrice,
                         @Param("carSource") String carSource);
}