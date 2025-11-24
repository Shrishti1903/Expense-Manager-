import java.time.LocalDate;

public class Expense {
    private String name;
    private String category;
    private double amount;
    private LocalDate date;
    private String notes;

    public Expense(String name, String category, double amount, LocalDate date, String notes) {
        this.name = name;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.notes = notes == null ? "" : notes;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNotes() { return notes; }

    @Override
    public String toString() {
        return name + "," + category + "," + amount + "," + date + "," + notes;
    }
}