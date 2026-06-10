package model.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductionRecord {
    private final List<ProductionEntry> entries;
    private final String unit;

    public ProductionRecord(String unit) {
        this.entries = new ArrayList<>();
        this.unit = unit;
    }

    public void addProduction(double amount) {
        addProduction(amount, LocalDateTime.now());
    }

    public void addProduction(double amount, LocalDateTime recordedAt) {
        entries.add(new ProductionEntry(amount, recordedAt));
    }

    public double getTotalProduction() {
        return entries.stream().mapToDouble(ProductionEntry::getAmount).sum();
    }

    public double getAverageProduction() {
        return entries.isEmpty() ? 0 : getTotalProduction() / entries.size();
    }

    public List<ProductionEntry> getEntries() {
        return entries;
    }

    /** Backward-compatible view of amounts only. */
    public List<Double> getProductions() {
        return entries.stream().map(ProductionEntry::getAmount).toList();
    }

    public String getUnit() {
        return unit;
    }
}
