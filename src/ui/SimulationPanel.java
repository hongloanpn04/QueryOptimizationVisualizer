package ui;

import model.QueryStatistics;
import model.CostResult;
import service.CostCalculator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SimulationPanel extends JPanel {

    // =====================================
    // 1. KHAI BÁO THÀNH PHẦN
    // =====================================

    private JTextField txtRecords;
    private JTextField txtBlocks;
    private JTextField txtIndexHeight;
    private JTextField txtMatchingRecords;

    private JComboBox<String> cboQueryType;

    private JTextArea txtSQLExample;

    private JTable resultTable;
    private DefaultTableModel tableModel;

    private JLabel lblRecommendation;

    private CostCalculator calculator;

    // =====================================
    // 2. CONSTRUCTOR
    // =====================================

    public SimulationPanel() {

        calculator = new CostCalculator();

        setLayout(new BorderLayout(10, 10));

        initComponents();
    }

    // =====================================
    // 3. KHỞI TẠO GIAO DIỆN
    // =====================================

    private void initComponents() {

        // =========================
        // PANEL NHẬP THÔNG SỐ
        // =========================

        JPanel inputPanel = new JPanel(
                new GridLayout(6, 2, 10, 10)
        );

        inputPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Thong so du lieu"
                )
        );

        txtRecords = new JTextField("10000");
        txtBlocks = new JTextField("2000");
        txtIndexHeight = new JTextField("3");

        // s sẽ được tự động ước lượng
        txtMatchingRecords = new JTextField("1");

        txtMatchingRecords.setEditable(false);

        // =========================
        // CHỌN LOẠI TRUY VẤN
        // =========================

        String[] queryTypes = {

            "Tim mot ban ghi theo khoa duy nhat",

            "Tim nhieu ban ghi theo thuoc tinh khong duy nhat",

            "Truy van khoang tren thuoc tinh khoa",

            "Tim kiem tren file duoc sap xep"
        };

        cboQueryType = new JComboBox<>(queryTypes);

        // =========================
        // THÊM THÀNH PHẦN NHẬP
        // =========================

        inputPanel.add(new JLabel("So ban ghi (r):"));
        inputPanel.add(txtRecords);

        inputPanel.add(new JLabel("So block (b):"));
        inputPanel.add(txtBlocks);

        inputPanel.add(new JLabel("Chieu cao Index (x):"));
        inputPanel.add(txtIndexHeight);

        inputPanel.add(new JLabel("So ban ghi phu hop (s):"));
        inputPanel.add(txtMatchingRecords);

        inputPanel.add(new JLabel("Loai truy van:"));
        inputPanel.add(cboQueryType);

        // =========================
        // NÚT TÍNH CHI PHÍ
        // =========================

        JButton btnCalculate =
                new JButton("Tinh chi phi");

        inputPanel.add(new JLabel(""));
        inputPanel.add(btnCalculate);

        // =========================
        // HIỂN THỊ SQL MINH HỌA
        // =========================

        txtSQLExample = new JTextArea(3, 50);

        txtSQLExample.setEditable(false);

        txtSQLExample.setFont(
                new Font("Consolas", Font.PLAIN, 13)
        );

        txtSQLExample.setBorder(
                BorderFactory.createTitledBorder(
                        "Truy van SQL minh hoa"
                )
        );

        JScrollPane sqlScroll =
                new JScrollPane(txtSQLExample);

        sqlScroll.setPreferredSize(
                new Dimension(0, 95)
        );

        // =========================
        // PANEL PHÍA TRÊN
        // =========================

        JPanel topPanel = new JPanel(
                new BorderLayout(5, 5)
        );

        topPanel.add(
                inputPanel,
                BorderLayout.NORTH
        );

        topPanel.add(
                sqlScroll,
                BorderLayout.SOUTH
        );

        add(topPanel, BorderLayout.NORTH);

        // =========================
        // BẢNG KẾT QUẢ
        // =========================

        tableModel = new DefaultTableModel(
                new Object[]{
                    "Phuong phap",
                    "Ten thuat toan",
                    "Cong thuc",
                    "Chi phi (I/O)"
                },
                0
        ) {

            @Override
            public boolean isCellEditable(
                    int row,
                    int column) {

                return false;
            }
        };

        resultTable = new JTable(tableModel);

        resultTable.setRowHeight(25);

        resultTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        add(
                new JScrollPane(resultTable),
                BorderLayout.CENTER
        );

        // =========================
        // HIỂN THỊ ĐỀ XUẤT
        // =========================

        lblRecommendation = new JLabel(
                "Chon loai truy van va nhan Tinh chi phi."
        );

        lblRecommendation.setFont(
                new Font("Arial", Font.BOLD, 13)
        );

        lblRecommendation.setBorder(
                BorderFactory.createEmptyBorder(
                        10, 15, 10, 15
                )
        );

        add(
                lblRecommendation,
                BorderLayout.SOUTH
        );

        // =========================
        // XỬ LÝ SỰ KIỆN
        // =========================

        cboQueryType.addActionListener(
                e -> updateQueryExample()
        );

        btnCalculate.addActionListener(
                e -> calculateCosts()
        );

        // Khi nhấn Enter ở ô r
        txtRecords.addActionListener(
                e -> updateQueryExample()
        );

        // Khởi tạo truy vấn ban đầu
        updateQueryExample();
    }

    // =====================================
    // 4. CẬP NHẬT CÂU SQL MINH HỌA
    // =====================================

    private void updateQueryExample() {

        int type = cboQueryType.getSelectedIndex();

        try {

            long r = Long.parseLong(
                    txtRecords.getText().trim()
            );

            if (r <= 0) {

                txtMatchingRecords.setText("");

                txtSQLExample.setText(
                        "So ban ghi r phai lon hon 0."
                );

                tableModel.setRowCount(0);

                return;
            }

            long s = 1;

            switch (type) {

                // =========================
                // TRUY VẤN KHÓA DUY NHẤT
                // =========================

                case 0:

                    txtSQLExample.setText("""
                        SELECT *
                        FROM EMPLOYEE
                        WHERE Ssn = 10001;
                        """);

                    s = 1;

                    break;

                // =========================
                // TRUY VẤN NHIỀU BẢN GHI
                // =========================

                case 1:

                    txtSQLExample.setText("""
                        SELECT *
                        FROM EMPLOYEE
                        WHERE Dno = 5;
                        """);

                    // Giả định có 5 phòng ban
                    // và các bản ghi phân bố đều

                    s = Math.max(1, r / 5);

                    break;

                // =========================
                // TRUY VẤN KHOẢNG
                // =========================

                case 2:

                    txtSQLExample.setText("""
                        SELECT *
                        FROM EMPLOYEE
                        WHERE Ssn > 5000;
                        """);

                    // Giả định khoảng 50% bản ghi
                    // đáp ứng điều kiện

                    s = Math.max(1, r / 2);

                    break;

                // =========================
                // FILE ĐƯỢC SẮP XẾP
                // =========================

                case 3:

                    txtSQLExample.setText("""
                        SELECT *
                        FROM EMPLOYEE
                        WHERE Ssn = 10001;
                        """);

                    s = 1;

                    break;
            }

            txtMatchingRecords.setText(
                    Long.toString(s)
            );

        } catch (NumberFormatException ex) {

            txtMatchingRecords.setText("");

            txtSQLExample.setText(
                    "Vui long nhap so ban ghi r hop le."
            );
        }

        tableModel.setRowCount(0);

        lblRecommendation.setText(
                "Nhan Tinh chi phi de xem ket qua."
        );
    }

    // =====================================
    // 5. TÍNH CHI PHÍ
    // =====================================

    private void calculateCosts() {

        try {

            long r = Long.parseLong(
                    txtRecords.getText().trim()
            );

            long b = Long.parseLong(
                    txtBlocks.getText().trim()
            );

            int x = Integer.parseInt(
                    txtIndexHeight.getText().trim()
            );

            // Cập nhật s dựa trên r mới nhất
            updateQueryExample();

            long s = Long.parseLong(
                    txtMatchingRecords.getText().trim()
            );

            // =========================
            // KIỂM TRA THÔNG SỐ
            // =========================

            if (r <= 0 || b <= 0 || x <= 0
                    || s <= 0 || s > r) {

                throw new IllegalArgumentException(
                        "Can r > 0, b > 0, x > 0 va 1 <= s <= r."
                );
            }

            QueryStatistics stats =
                    new QueryStatistics(r, b, x, s);

            List<CostResult> candidates =
                    new ArrayList<>();

            int queryType =
                    cboQueryType.getSelectedIndex();

            // =========================
            // LỌC THUẬT TOÁN
            // =========================

            switch (queryType) {

                // =========================
                // KHÓA DUY NHẤT
                // =========================

                case 0:

                    candidates.add(
                            calculator.calculateS1(stats)
                    );

                    candidates.add(
                            calculator.calculateS1Average(stats)
                    );

                    candidates.add(
                            calculator.calculateS3(stats)
                    );

                    candidates.add(
                            calculator.calculateS3Hash(stats)
                    );

                    candidates.add(
                            calculator.calculateS6Unique(stats)
                    );

                    break;

                // =========================
                // NHIỀU BẢN GHI
                // =========================

                case 1:

                    candidates.add(
                            calculator.calculateS1(stats)
                    );

                    candidates.add(
                            calculator.calculateS5(stats)
                    );

                    // Đã bỏ S6a theo phạm vi báo cáo

                    break;

                // =========================
                // TRUY VẤN KHOẢNG
                // =========================

                case 2:

                    candidates.add(
                            calculator.calculateS1(stats)
                    );

                    candidates.add(
                            calculator.calculateS4(stats)
                    );

                    break;

                // =========================
                // FILE CÓ THỨ TỰ
                // =========================

                case 3:

                    candidates.add(
                            calculator.calculateS1(stats)
                    );

                    candidates.add(
                            calculator.calculateS2(stats)
                    );

                    break;
            }

            // =========================
            // HIỂN THỊ BẢNG
            // =========================

            tableModel.setRowCount(0);

            CostResult best = null;

            int bestRow = -1;

            for (CostResult result : candidates) {

                addResult(result);

                if (best == null
                        || result.getCost() < best.getCost()) {

                    best = result;

                    bestRow =
                            tableModel.getRowCount() - 1;
                }
            }

            // =========================
            // HIỂN THỊ ĐỀ XUẤT
            // =========================

            if (best != null) {

                String recommendation = String.format(
                        "Chi phi thap nhat trong cac phuong an dang xet: "
                        + "%s - %s (%.2f I/O)",
                        best.getMethodCode(),
                        best.getMethodName(),
                        best.getCost()
                );

                lblRecommendation.setText(
                        recommendation
                );

                if (bestRow >= 0) {

                    resultTable.setRowSelectionInterval(
                            bestRow,
                            bestRow
                    );
                }
            }

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Vui long nhap so hop le!",
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

    // =====================================
    // 6. THÊM KẾT QUẢ VÀO BẢNG
    // =====================================

    private void addResult(CostResult result) {

        tableModel.addRow(new Object[]{
            result.getMethodCode(),
            result.getMethodName(),
            result.getFormula(),
            result.getCost()
        });
    }
}