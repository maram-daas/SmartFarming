package model.interfaces;

import model.entities.ProductionRecord;

import java.time.LocalDateTime;

public interface Producing {
    double getProduction();
    void recordProduction(double amount);
    void recordProduction(double amount, LocalDateTime recordedAt);
    ProductionRecord getProductionRecord();
}
