package model.entities;

public class FeedingProgram {
    private String feedType;
    private double quantityPerMeal;
    private int mealsPerDay;

    public FeedingProgram(String feedType, double quantityPerMeal, int mealsPerDay) {
        this.feedType = feedType;
        this.quantityPerMeal = quantityPerMeal;
        this.mealsPerDay = mealsPerDay;
    }

    public String getFeedType() { return feedType; }
    public double getQuantityPerMeal() { return quantityPerMeal; }
    public int getMealsPerDay() { return mealsPerDay; }
    public double getDailyQuantity() { return quantityPerMeal * mealsPerDay; }
}
