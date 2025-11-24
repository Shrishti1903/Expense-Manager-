import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ExpenseManagerGUI extends JFrame {

    private final ExpenseManager manager = new ExpenseManager();
    private final ExpenseTableModel tableModel = new ExpenseTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextArea reportArea = new JTextArea(12, 60);

    public ExpenseManagerGUI() {
        super("Expense Manager (Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        // Style
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setFillsViewportHeight(true);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Add Expense", buildAddPanel());
        tabs.addTab("All Expenses", buildListPanel());
        tabs.addTab("Budgets", buildBudgetPanel());
        tabs.addTab("Reports & Export", buildReportPanel());

        add(tabs);
        refreshTable();
    }

    /* ---------------- Panels ---------------- */

    private JPanel buildAddPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField name = new JTextField(20);
        JComboBox<String> category = new JComboBox<>(manager.getCategories());
        JTextField amount = new JTextField(10);
        JTextField date = new JTextField(10);
        date.setToolTipText("YYYY-MM-DD (blank = today)");
        JTextArea notes = new JTextArea(4, 20);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);

        int r=0;
        gc.gridy=r++; gc.gridx=0; form.add(new JLabel("Name:"), gc);     gc.gridx=1; form.add(name, gc);
        gc.gridy=r++; gc.gridx=0; form.add(new JLabel("Category:"), gc); gc.gridx=1; form.add(category, gc);
        gc.gridy=r++; gc.gridx=0; form.add(new JLabel("Amount:"), gc);   gc.gridx=1; form.add(amount, gc);
        gc.gridy=r++; gc.gridx=0; form.add(new JLabel("Date:"), gc);     gc.gridx=1; form.add(date, gc);
        gc.gridy=r++; gc.gridx=0; form.add(new JLabel("Notes:"), gc);    gc.gridx=1; form.add(new JScrollPane(notes), gc);

        JButton addBtn = new JButton("Add Expense");
        addBtn.addActionListener((ActionEvent e) -> {
            try {
                String n = name.getText().trim();
                String c = (String) category.getSelectedItem();
                double a = Double.parseDouble(amount.getText().trim());
                LocalDate d;
                String di = date.getText().trim();
                if (di.isEmpty()) d = LocalDate.now();
                else d = LocalDate.parse(di);
                String note = notes.getText().trim();

                manager.addExpense(n, c, a, d, note);
                JOptionPane.showMessageDialog(this, "Expense added!");
                name.setText(""); amount.setText(""); date.setText(""); notes.setText("");
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(addBtn);

        p.add(form, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildListPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshTable());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(refresh);
        p.add(top, BorderLayout.NORTH);
        return p;
    }

    private JPanel buildBudgetPanel() {
        JPanel p = new JPanel(new BorderLayout(10,10));

        JTextArea budgetsView = new JTextArea(15, 50);
        budgetsView.setEditable(false);
        budgetsView.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel setPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> cat = new JComboBox<>(manager.getCategories());
        JTextField amt = new JTextField(8);
        JButton setBtn = new JButton("Set Budget");

        setBtn.addActionListener(e -> {
            try {
                String c = (String)cat.getSelectedItem();
                double a = Double.parseDouble(amt.getText().trim());
                manager.setBudget(c, a);
                JOptionPane.showMessageDialog(this, "Budget set for " + c);
                amt.setText("");
                updateBudgetsView(budgetsView);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        setPanel.add(new JLabel("Category:")); setPanel.add(cat);
        setPanel.add(new JLabel("Budget:")); setPanel.add(amt);
        setPanel.add(setBtn);

        p.add(setPanel, BorderLayout.NORTH);
        p.add(new JScrollPane(budgetsView), BorderLayout.CENTER);

        updateBudgetsView(budgetsView);
        return p;
    }

    private void updateBudgetsView(JTextArea ta) {
        Map<String, Double> budgets = manager.getBudgetsCopy();
        // compute spent
        Map<String, Double> spent = new HashMap<>();
        for (Expense e : manager.getAllExpenses()) {
            spent.put(e.getCategory(), spent.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s %-12s %-12s %-10s%n", "Category", "Budget", "Spent", "Status"));
        sb.append("--------------------------------------------------------\n");
        for (String c : manager.getCategories()) {
            double b = budgets.getOrDefault(c, 0.0);
            double s = spent.getOrDefault(c, 0.0);
            String status;
            if (b <= 0) status = "No Budget";
            else if (s > b) status = "RED (Exceeded)";
            else if (s > 0.8 * b) status = "YELLOW (80%+)";
            else status = "GREEN (Safe)";
            sb.append(String.format("%-15s %-12.2f %-12.2f %-10s%n", c, b, s, status));
        }
        ta.setText(sb.toString());
    }

    private JPanel buildReportPanel() {
        JPanel p = new JPanel(new BorderLayout(8,8));

        // Top controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField month = new JTextField(2);
        JTextField year = new JTextField(4);
        JButton showMonthly = new JButton("Show Monthly Report");
        JButton exportAll = new JButton("Export ALL to CSV");
        JButton exportMonthly = new JButton("Export Monthly to CSV");

        controls.add(new JLabel("Month:"));
        controls.add(month);
        controls.add(new JLabel("Year:"));
        controls.add(year);
        controls.add(showMonthly);
        controls.add(exportMonthly);
        controls.add(exportAll);

        showMonthly.addActionListener(e -> {
            try {
                int m = Integer.parseInt(month.getText().trim());
                int y = Integer.parseInt(year.getText().trim());
                showMonthlyReport(m, y);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Enter valid month & year!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        exportAll.addActionListener(e -> {
            String name = promptFileName("export_all.csv");
            if (name != null) manager.exportAllToCsv(name);
        });

        exportMonthly.addActionListener(e -> {
            try {
                int m = Integer.parseInt(month.getText().trim());
                int y = Integer.parseInt(year.getText().trim());
                String def = "export_" + y + "_" + String.format("%02d", m) + ".csv";
                String name = promptFileName(def);
                if (name != null) manager.exportMonthlyToCsv(m, y, name);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Enter valid month & year!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        p.add(controls, BorderLayout.NORTH);
        p.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        return p;
    }

    private String promptFileName(String def) {
        String name = JOptionPane.showInputDialog(this, "Output filename:", def);
        if (name == null) return null;
        name = name.trim();
        if (name.isEmpty()) name = def;
        return name;
    }

    private void showMonthlyReport(int month, int year) {
        List<Expense> data = manager.getAllExpenses();
        List<Expense> list = new ArrayList<>();
        for (Expense e : data) {
            if (e.getDate().getMonthValue() == month && e.getDate().getYear() == year) list.add(e);
        }
        String[] cats = manager.getCategories();
        Map<String, Double> totals = new HashMap<>();
        double total = 0.0;
        for (String c : cats) totals.put(c, 0.0);
        for (Expense e : list) {
            totals.put(e.getCategory(), totals.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
            total += e.getAmount();
        }
        double max = 1.0;
        for (double v : totals.values()) if (v > max) max = v;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("---- Monthly Report: %02d/%d ----%n", month, year));
        sb.append(String.format("%-15s %-15s %-10s %-12s %-20s%n", "Name", "Category", "Amount", "Date", "Notes"));
        sb.append("----------------------------------------------------------------------------\n");
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
        for (Expense e : list) {
            sb.append(String.format("%-15s %-15s %10.2f %12s %-20s%n",
                    e.getName(), e.getCategory(), e.getAmount(), df.format(e.getDate()), e.getNotes()));
        }
        sb.append(String.format("%nCategory Breakdown:%n"));
        for (String c : cats) {
            double amt = totals.get(c);
            int bars = (int)((amt / max) * 30);
            sb.append(String.format("%-15s | %s %.2f%n", c, "#".repeat(Math.max(0, bars)), amt));
        }
        sb.append(String.format("%nTOTAL: %.2f%n", total));
        reportArea.setText(sb.toString());
    }

    /* ---------------- Helpers ---------------- */
    private void refreshTable() {
        tableModel.setData(new ArrayList<>(manager.getAllExpenses()));
    }

    /* ---------------- Table Model ---------------- */
    static class ExpenseTableModel extends AbstractTableModel {
        private final String[] cols = {"Name","Category","Amount","Date","Notes"};
        private List<Expense> data = new ArrayList<>();

        public void setData(List<Expense> d) {
            this.data = d;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            Expense e = data.get(r);
            switch (c) {
                case 0: return e.getName();
                case 1: return e.getCategory();
                case 2: return String.format("%.2f", e.getAmount());
                case 3: return e.getDate().toString();
                case 4: return e.getNotes();
            }
            return "";
        }
    }

    /* ---------------- Main ---------------- */
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new ExpenseManagerGUI().setVisible(true));
    }
}