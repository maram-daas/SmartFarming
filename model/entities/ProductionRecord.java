package model.entities;

import java.util.ArrayList;
import java.util.List;

public class ProductionRecord {
    private List<Double> productions;
    private String unit;

    public ProductionRecord(String unit) {
        this.productions = new ArrayList<>();
        this.unit = unit;
    }

    public void addProduction(double amount) { productions.add(amount); }
    public double getTotalProduction() { return productions.stream().mapToDouble(Double::doubleValue).sum(); }
    public double getAverageProduction() { return productions.isEmpty() ? 0 : getTotalProduction() / productions.size(); }
    public List<Double> getProductions() { return productions; }
    public String getUnit() { return unit; }
}
