package ui;

import model.QueryStatistics;
import model.CostResult;
import service.CostCalculator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SimulationPanel extends JPanel {

    private JTextField txtRecords;
    private JTextField txtBlocks;
    private JTextField txtIndexHeight;
    private JTextField txtMatchingRecords;
    private JComboBox<String> cboQueryType;

    private JTable resultTable;
    private DefaultTableModel tableModel;

    private CostCalculator calculator;

    public SimulationPanel() {

        calculator = new CostCalculator();

        setLayout(new BorderLayout(10, 10));

        initComponents();
    }

    private void initComponents() {

        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 10, 10));

        inputPanel.setBorder(
                BorderFactory.createTitledBorder("Thong so du lieu")
        );

        txtRecords = new JTextField("10000");
        txtBlocks = new JTextField("2000");
        txtIndexHeight = new JTextField("3");
        txtMatchingRecords = new JTextField("80");
        
        String[] queryTypes = {
            "Tim mot ban ghi theo khoa duy nhat",
            "Tim nhieu ban ghi theo thuoc tinh khong duy nhat",
            "Truy van khoang tren thuoc tinh khoa",
            "Tim kiem tren file duoc sap xep"
        };

        cboQueryType = new JComboBox<>(queryTypes);

        inputPanel.add(new JLabel("So ban ghi (r):"));
        inputPanel.add(txtRecords);

        inputPanel.add(new JLabel("So block (b):"));
        inputPanel.add(txtBlocks);

        inputPanel.add(new JLabel("Chieu cao Index (x):"));
        inputPanel.add(txtIndexHeight);

        inputPanel.add(new JLabel("So ban ghi phu hop (s):"));
        inputPanel.add(txtMatchingRecords);

        JButton btnCalculate = new JButton("Tinh chi phi");
        inputPanel.add(new JLabel("Loai truy van:"));
        inputPanel.add(cboQueryType);

        inputPanel.add(new JLabel(""));
        inputPanel.add(btnCalculate);

        add(inputPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{
                    "Phuong phap",
                    "Ten thuat toan",
                    "Cong thuc",
                    "Chi phi (I/O)"
                },
                0
        );

        resultTable = new JTable(tableModel);

        add(new JScrollPane(resultTable), BorderLayout.CENTER);

        btnCalculate.addActionListener(e -> calculateCosts());
    }

    private void calculateCosts() {

        try {

            long r = Long.parseLong(txtRecords.getText().trim());
            long b = Long.parseLong(txtBlocks.getText().trim());
            int x = Integer.parseInt(txtIndexHeight.getText().trim());
            long s = Long.parseLong(txtMatchingRecords.getText().trim());

            // Kiểm tra tham số
            if (r <= 0 || b <= 0 || x <= 0 || s <= 0 || s > r) {
                throw new IllegalArgumentException(
                        "Can r > 0, b > 0, x > 0 va 1 <= s <= r."
                );
            }

            QueryStatistics stats =
                    new QueryStatistics(r, b, x, s);

            tableModel.setRowCount(0);

            int queryType = cboQueryType.getSelectedIndex();

            switch (queryType) {

                case 0:
                    // Tìm một bản ghi theo khóa duy nhất
                    if (s != 1) {
                        throw new IllegalArgumentException(
                                "Truy van khoa duy nhat phai co s = 1."
                        );
                    }

                    addResult(calculator.calculateS1(stats));
                    addResult(calculator.calculateS1Average(stats));
                    addResult(calculator.calculateS3(stats));
                    addResult(calculator.calculateS3Hash(stats));
                    addResult(calculator.calculateS6Unique(stats));

                    break;

                case 1:
                    // Tìm nhiều bản ghi theo thuộc tính không duy nhất
                    addResult(calculator.calculateS1(stats));
                    addResult(calculator.calculateS5(stats));
                    addResult(calculator.calculateS6(stats));

                    break;

                case 2:
                    // Truy vấn khoảng trên thuộc tính khóa
                    addResult(calculator.calculateS1(stats));
                    addResult(calculator.calculateS4(stats));

                    break;

                case 3:
                    // Tìm kiếm trên file có thứ tự
                    addResult(calculator.calculateS1(stats));
                    addResult(calculator.calculateS2(stats));

                    break;
            }

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Vui long nhap cac gia tri so hop le!",
                    "Loi du lieu",
                    JOptionPane.ERROR_MESSAGE
            );

        } catch (IllegalArgumentException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Loi thong so",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
    // Thêm kết quả tính toán vào bảng
    private void addResult(CostResult result) {

        tableModel.addRow(new Object[]{
            result.getMethodCode(),
            result.getMethodName(),
            result.getFormula(),
            result.getCost()
        });
    }
}