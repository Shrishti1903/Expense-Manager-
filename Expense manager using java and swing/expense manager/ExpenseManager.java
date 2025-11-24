import java.io.*;
import java.util.*;
import java.time.LocalDate;
import java.util.stream.Collectors;

public class ExpenseManager {
    private List<Expense> expenses = new ArrayList<>();
    private final String fileName = "expenses.csv";

    // Budgets
    private final Map<String, Double> budgets = new HashMap<>();
    private final String budgetsFile = "budgets.csv";

    // ANSI colors (used in console methods)
    private final String RESET = "\u001B[0m";
    private final String RED = "\u001B[31m";
    private final String GREEN = "\u001B[32m";
    private final String YELLOW = "\u001B[33m";
    private final String CYAN = "\u001B[36m";

    private final String[] categories = {"Food", "Transport", "Bills", "Entertainment", "Misc"};

    public ExpenseManager() {
        loadExpenses();
        loadBudgets();
        // default budgets if file empty
        for (String c : categories) budgets.putIfAbsent(c, 0.0);
    }

    public String[] getCategories() { return categories; }

    /* ------------ Added getters for GUI ------------ */
    public List<Expense> getAllExpenses() {
        return Collections.unmodifiableList(expenses);
    }
    public Map<String, Double> getBudgetsCopy() {
        return new HashMap<>(budgets);
    }

    /* ------------ Core Ops ------------ */

    public void addExpense(String name, String category, double amount, LocalDate date, String notes) {
        Expense e = new Expense(name, category, amount, date, notes);
        expenses.add(e);
        saveExpenses();
        budgetAlertFor(category); // alert (if any)
    }

    public void showExpenses() {
        System.out.println(CYAN + "\n--- All Expenses ---" + RESET);
        printHeader();
        for (Expense e : expenses) printRow(e);
        printTotal(expenses);
    }

    public void showCategorySummary() {
        System.out.println(CYAN + "\n--- Category-wise Summary ---" + RESET);
        Map<String, Double> summary = categoryTotals(expenses);
        double total = summary.values().stream().mapToDouble(Double::doubleValue).sum();
        double maxAmount = summary.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);

        for (String cat : categories) {
            double amt = summary.getOrDefault(cat, 0.0);
            int bars = (int)((amt / (maxAmount == 0 ? 1 : maxAmount)) * 30);
            double percent = total == 0 ? 0 : (amt / total) * 100.0;
            System.out.printf("%-15s | ", cat);
            for (int i = 0; i < bars; i++) System.out.print("#");
            System.out.printf(" %.2f (%.1f%%)\n", amt, percent);
        }
        System.out.printf(GREEN + "Total Expenses: %.2f\n" + RESET, total);
    }

    public void showChart() {
        System.out.println(CYAN + "\n--- Expense Chart (per item) ---" + RESET);
        double maxAmount = expenses.stream().mapToDouble(Expense::getAmount).max().orElse(1.0);
        for (Expense e : expenses) {
            int bars = (int)((e.getAmount() / maxAmount) * 50);
            String color = e.getAmount() > maxAmount * 0.7 ? RED : e.getAmount() > maxAmount * 0.3 ? YELLOW : GREEN;
            System.out.printf("%-15s | ", e.getName());
            System.out.print(color);
            for (int i = 0; i < bars; i++) System.out.print("#");
            System.out.print(RESET);
            System.out.printf(" %.2f\n", e.getAmount());
        }
    }

    public void showTopExpenses(int n) {
        System.out.println(CYAN + "\n--- Top " + n + " Expenses ---" + RESET);
        List<Expense> list = expenses.stream()
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(n)
                .collect(Collectors.toList());
        printHeader();
        for (Expense e : list) printRow(e);
        printTotal(list);
    }

    public void showMonthlyReport(int month, int year) {
        System.out.println(CYAN + "\n--- Monthly Report: " + month + "/" + year + " ---" + RESET);
        List<Expense> list = expenses.stream()
                .filter(e -> e.getDate().getMonthValue() == month && e.getDate().getYear() == year)
                .collect(Collectors.toList());
        if (list.isEmpty()) { System.out.println("No data for this month."); return; }
        printHeader();
        for (Expense e : list) printRow(e);
        printTotal(list);
        System.out.println(CYAN + "\nCategory Breakdown:" + RESET);
        Map<String, Double> sum = categoryTotals(list);
        sum.forEach((k,v) -> System.out.printf("%-15s : %.2f\n", k, v));
    }

    /* ------------ Search ------------ */

    public void searchByKeyword(String keyword) {
        String q = keyword.toLowerCase();
        List<Expense> list = expenses.stream().filter(e ->
                e.getName().toLowerCase().contains(q) ||
                        e.getNotes().toLowerCase().contains(q) ||
                        e.getCategory().toLowerCase().contains(q)
        ).collect(Collectors.toList());
        printSearchResult("Keyword: " + keyword, list);
    }

    public void searchByCategory(String category) {
        List<Expense> list = expenses.stream()
                .filter(e -> e.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
        printSearchResult("Category: " + category, list);
    }

    public void searchByDateRange(LocalDate from, LocalDate to) {
        List<Expense> list = expenses.stream()
                .filter(e -> ( !e.getDate().isBefore(from) && !e.getDate().isAfter(to) ))
                .collect(Collectors.toList());
        printSearchResult("Date range: " + from + " to " + to, list);
    }

    private void printSearchResult(String title, List<Expense> list) {
        System.out.println(CYAN + "\n--- Search (" + title + ") ---" + RESET);
        if (list.isEmpty()) { System.out.println("No matching expenses."); return; }
        printHeader();
        for (Expense e : list) printRow(e);
        printTotal(list);
    }

    /* ------------ Budgets & Alerts ------------ */

    public void setBudget(String category, double amount) {
        budgets.put(category, amount);
        saveBudgets();
        System.out.println(GREEN + "Budget set for " + category + ": " + amount + RESET);
    }

    public void viewBudgets() {
        System.out.println(CYAN + "\n--- Budgets ---" + RESET);
        for (String c : categories) {
            System.out.printf("%-15s : %.2f\n", c, budgets.getOrDefault(c, 0.0));
        }
        System.out.println(CYAN + "\n--- Current Spend vs Budget ---" + RESET);
        Map<String, Double> totals = categoryTotals(expenses);
        for (String c : categories) {
            double spent = totals.getOrDefault(c, 0.0);
            double limit = budgets.getOrDefault(c, 0.0);
            if (limit <= 0) {
                System.out.printf("%-15s : Spent %.2f (no budget set)\n", c, spent);
            } else {
                String color = spent > limit ? RED : (spent > 0.8 * limit ? YELLOW : GREEN);
                System.out.print(color);
                System.out.printf("%-15s : Spent %.2f / Budget %.2f\n", c, spent, limit);
                System.out.print(RESET);
            }
        }
    }

    private void budgetAlertFor(String category) {
        double limit = budgets.getOrDefault(category, 0.0);
        if (limit <= 0) return; // no budget
        double spent = expenses.stream()
                .filter(e -> e.getCategory().equalsIgnoreCase(category))
                .mapToDouble(Expense::getAmount).sum();
        if (spent > limit) {
            System.out.println(RED + "⚠ Budget exceeded for " + category +
                    " (Spent: " + String.format("%.2f", spent) + " / " + String.format("%.2f", limit) + ")" + RESET);
        } else if (spent > 0.8 * limit) {
            System.out.println(YELLOW + "⚠ Nearing budget for " + category +
                    " (Spent: " + String.format("%.2f", spent) + " / " + String.format("%.2f", limit) + ")" + RESET);
        }
    }

    /* ------------ Export ------------ */

    public void exportAllToCsv(String outFile) {
        exportListToCsv(outFile, expenses);
    }

    public void exportMonthlyToCsv(int month, int year, String outFile) {
        List<Expense> list = expenses.stream()
                .filter(e -> e.getDate().getMonthValue() == month && e.getDate().getYear() == year)
                .collect(Collectors.toList());
        exportListToCsv(outFile, list);
    }

    private void exportListToCsv(String outFile, List<Expense> list) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(outFile))) {
            pw.println("Name,Category,Amount,Date,Notes");
            for (Expense e : list) {
                // Escape commas in notes/name if any
                String name = e.getName().replace(",", " ");
                String notes = e.getNotes().replace(",", " ");
                pw.println(name + "," + e.getCategory() + "," + e.getAmount() + "," + e.getDate() + "," + notes);
            }
            System.out.println(GREEN + "Exported " + list.size() + " rows to " + outFile + RESET);
        } catch (IOException ex) {
            System.out.println(RED + "Export failed: " + ex.getMessage() + RESET);
        }
    }

    /* ------------ Persistence ------------ */

    private void saveExpenses() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
            for (Expense e : expenses) pw.println(e.toString());
        } catch (IOException ex) {
            System.out.println(RED + "Error saving expenses." + RESET);
        }
    }

    private void loadExpenses() {
        File file = new File(fileName);
        if (!file.exists()) return;
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String[] parts = sc.nextLine().split(",", -1); // keep empty notes
                if (parts.length >= 5) {
                    String name = parts[0];
                    String category = parts[1];
                    double amount = Double.parseDouble(parts[2]);
                    LocalDate date = LocalDate.parse(parts[3]);
                    String notes = parts[4];
                    expenses.add(new Expense(name, category, amount, date, notes));
                }
            }
        } catch (Exception ex) {
            System.out.println(RED + "Error loading expenses." + RESET);
        }
    }

    private void saveBudgets() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(budgetsFile))) {
            pw.println("Category,Budget");
            for (String c : categories) {
                pw.println(c + "," + budgets.getOrDefault(c, 0.0));
            }
        } catch (IOException ex) {
            System.out.println(RED + "Error saving budgets." + RESET);
        }
    }

    private void loadBudgets() {
        File f = new File(budgetsFile);
        if (!f.exists()) return;
        try (Scanner sc = new Scanner(f)) {
            if (sc.hasNextLine()) sc.nextLine(); // skip header
            while (sc.hasNextLine()) {
                String[] p = sc.nextLine().split(",", -1);
                if (p.length >= 2) {
                    String cat = p[0];
                    double amt = Double.parseDouble(p[1]);
                    budgets.put(cat, amt);
                }
            }
        } catch (Exception ex) {
            System.out.println(YELLOW + "Budgets file invalid, using defaults." + RESET);
        }
    }

    /* ------------ Helpers ------------ */

    private Map<String, Double> categoryTotals(List<Expense> list) {
        Map<String, Double> map = new HashMap<>();
        for (Expense e : list) map.put(e.getCategory(), map.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
        return map;
    }

    private void printHeader() {
        System.out.printf("%-15s %-15s %10s %12s %-20s\n", "Name", "Category", "Amount", "Date", "Notes");
        System.out.println("--------------------------------------------------------------------------------");
    }

    private void printRow(Expense e) {
        System.out.printf("%-15s %-15s %10.2f %12s %-20s\n",
                e.getName(), e.getCategory(), e.getAmount(), e.getDate(), e.getNotes());
    }

    private void printTotal(List<Expense> list) {
        double total = list.stream().mapToDouble(Expense::getAmount).sum();
        System.out.printf(GREEN + "Total: %.2f\n" + RESET, total);
    }
}