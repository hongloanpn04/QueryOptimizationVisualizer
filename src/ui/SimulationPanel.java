package ui;

import model.QueryStatistics;
import model.CostResult;
import service.CostCalculator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Mo phong ly thuyet S1-S7.
 * S1-S6: uoc luong chi phi I/O theo cong thuc.
 * S7: minh hoa quy trinh AND su dung mot Index tren Dno, khong dat cong thuc S7.
 */
public class SimulationPanel extends JPanel {

    private JTextField txtRecords;
    private JTextField txtBlocks;
    private JTextField txtIndexHeight;
    private JTextField txtMatchingRecords;
    private JComboBox<String> cboQueryType;
    private JTextArea txtSQLExample;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel lblRecommendation;
    private JButton btnCalculate;

    private JTextArea txtS7Steps;
    private JPanel resultCards;
    private CardLayout resultCardLayout;

    private final CostCalculator calculator;

    private static final int S7_QUERY = 4;

    public SimulationPanel() {
        calculator = new CostCalculator();
        setLayout(new BorderLayout(10, 10));
        initComponents();
    }

    private void initComponents() {
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Thong so du lieu"));

        txtRecords = new JTextField("10000");
        txtBlocks = new JTextField("2000");
        txtIndexHeight = new JTextField("3");
        txtMatchingRecords = new JTextField("1");
        txtMatchingRecords.setEditable(false);

        cboQueryType = new JComboBox<>(new String[]{
            "Tim mot ban ghi theo khoa duy nhat",
            "Tim nhieu ban ghi theo thuoc tinh khong duy nhat",
            "Truy van khoang tren thuoc tinh khoa",
            "Tim kiem tren file duoc sap xep",
            "Truy van nhieu dieu kien AND (S7)"
        });

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

        btnCalculate = new JButton("Tinh chi phi");
        inputPanel.add(new JLabel(""));
        inputPanel.add(btnCalculate);

        txtSQLExample = new JTextArea(3, 50);
        txtSQLExample.setEditable(false);
        txtSQLExample.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtSQLExample.setBorder(
                BorderFactory.createTitledBorder("Truy van SQL minh hoa"));
        JScrollPane sqlScroll = new JScrollPane(txtSQLExample);
        sqlScroll.setPreferredSize(new Dimension(0, 95));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(inputPanel, BorderLayout.NORTH);
        topPanel.add(sqlScroll, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"Phuong phap", "Ten thuat toan", "Cong thuc", "Chi phi (I/O)"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setRowHeight(25);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        txtS7Steps = new JTextArea();
        txtS7Steps.setEditable(false);
        txtS7Steps.setFont(new Font("Consolas", Font.PLAIN, 14));
        txtS7Steps.setMargin(new Insets(12, 16, 12, 16));
        txtS7Steps.setLineWrap(true);
        txtS7Steps.setWrapStyleWord(true);

        resultCardLayout = new CardLayout();
        resultCards = new JPanel(resultCardLayout);
        resultCards.add(new JScrollPane(resultTable), "COST");
        resultCards.add(new JScrollPane(txtS7Steps), "S7");
        add(resultCards, BorderLayout.CENTER);

        lblRecommendation = new JLabel("Chon loai truy van va nhan Tinh chi phi.");
        lblRecommendation.setFont(new Font("Arial", Font.BOLD, 13));
        lblRecommendation.setBorder(
                BorderFactory.createEmptyBorder(10, 15, 10, 15));
        add(lblRecommendation, BorderLayout.SOUTH);

        cboQueryType.addActionListener(e -> updateQueryExample());
        btnCalculate.addActionListener(e -> calculateCosts());
        txtRecords.addActionListener(e -> updateQueryExample());

        updateQueryExample();
    }

    private void updateQueryExample() {
        int type = cboQueryType.getSelectedIndex();
        tableModel.setRowCount(0);

        if (type == S7_QUERY) {
            txtSQLExample.setText("""
                SELECT *
                FROM EMPLOYEE
                WHERE Dno = 5 AND Salary > 30000 AND Sex = 'F';
                """);
            txtMatchingRecords.setText("Khong ap dung");
            txtRecords.setEnabled(false);
            txtBlocks.setEnabled(false);
            txtIndexHeight.setEnabled(false);
            btnCalculate.setText("Mo phong S7");
            txtS7Steps.setText("Nhan 'Mo phong S7' de xem cac buoc xu ly AND.");
            resultCardLayout.show(resultCards, "S7");
            lblRecommendation.setText("S7 minh hoa quy trinh; khong co cong thuc I/O rieng.");
            return;
        }

        txtRecords.setEnabled(true);
        txtBlocks.setEnabled(true);
        txtIndexHeight.setEnabled(true);
        btnCalculate.setText("Tinh chi phi");
        resultCardLayout.show(resultCards, "COST");

        try {
            long r = Long.parseLong(txtRecords.getText().trim());
            if (r <= 0) {
                txtMatchingRecords.setText("");
                txtSQLExample.setText("So ban ghi r phai lon hon 0.");
                lblRecommendation.setText("Vui long nhap r hop le.");
                return;
            }

            long s;
            switch (type) {
                case 0:
                    txtSQLExample.setText("""
                        SELECT *
                        FROM EMPLOYEE
                        WHERE Ssn = 10001;
                        """);
                    s = 1;
                    break;
                case 1:
                    txtSQLExample.setText("""
                        SELECT *
                        FROM EMPLOYEE
                        WHERE Dno = 5;
                        """);
                    // Gia dinh 5 phong ban va du lieu phan bo deu.
                    s = Math.max(1, r / 5);
                    break;
                case 2:
                    txtSQLExample.setText("""
                        SELECT *
                        FROM EMPLOYEE
                        WHERE Ssn > 5000;
                        """);
                    // Gia dinh 50% ban ghi thoa man.
                    s = Math.max(1, r / 2);
                    break;
                case 3:
                    txtSQLExample.setText("""
                        SELECT *
                        FROM EMPLOYEE
                        WHERE Ssn = 10001;
                        """);
                    s = 1;
                    break;
                default:
                    throw new IllegalStateException("Loai truy van khong hop le.");
            }
            txtMatchingRecords.setText(Long.toString(s));
            lblRecommendation.setText("Nhan Tinh chi phi de xem ket qua.");
        } catch (NumberFormatException ex) {
            txtMatchingRecords.setText("");
            txtSQLExample.setText("Vui long nhap so ban ghi r hop le.");
            lblRecommendation.setText("Vui long nhap r hop le.");
        }
    }

    private void calculateCosts() {
        if (cboQueryType.getSelectedIndex() == S7_QUERY) {
            showS7Simulation();
            return;
        }

        try {
            long r = Long.parseLong(txtRecords.getText().trim());
            long b = Long.parseLong(txtBlocks.getText().trim());
            int x = Integer.parseInt(txtIndexHeight.getText().trim());
            updateQueryExample();
            long s = Long.parseLong(txtMatchingRecords.getText().trim());

            if (r <= 0 || b <= 0 || x <= 0 || s <= 0 || s > r) {
                throw new IllegalArgumentException(
                        "Can r > 0, b > 0, x > 0 va 1 <= s <= r.");
            }

            QueryStatistics stats = new QueryStatistics(r, b, x, s);
            List<CostResult> candidates = new ArrayList<>();

            switch (cboQueryType.getSelectedIndex()) {
                case 0:
                    candidates.add(calculator.calculateS1(stats));
                    candidates.add(calculator.calculateS1Average(stats));
                    candidates.add(calculator.calculateS3(stats));
                    candidates.add(calculator.calculateS3Hash(stats));
                    candidates.add(calculator.calculateS6Unique(stats));
                    break;
                case 1:
                    candidates.add(calculator.calculateS1(stats));
                    candidates.add(calculator.calculateS5(stats));
                    break;
                case 2:
                    candidates.add(calculator.calculateS1(stats));
                    candidates.add(calculator.calculateS4(stats));
                    break;
                case 3:
                    candidates.add(calculator.calculateS1(stats));
                    candidates.add(calculator.calculateS2(stats));
                    break;
                default:
                    throw new IllegalStateException("Loai truy van khong hop le.");
            }

            tableModel.setRowCount(0);
            CostResult best = null;
            int bestRow = -1;
            for (CostResult result : candidates) {
                tableModel.addRow(new Object[]{
                    result.getMethodCode(), result.getMethodName(),
                    result.getFormula(), result.getCost()
                });
                if (best == null || result.getCost() < best.getCost()) {
                    best = result;
                    bestRow = tableModel.getRowCount() - 1;
                }
            }
            if (best != null) {
                lblRecommendation.setText(String.format(
                        "Chi phi thap nhat trong cac phuong an dang xet: %s - %s (%.2f I/O)",
                        best.getMethodCode(), best.getMethodName(), best.getCost()));
                if (bestRow >= 0) {
                    resultTable.setRowSelectionInterval(bestRow, bestRow);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Vui long nhap so hop le!",
                    "Loi du lieu", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Loi thong so", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showS7Simulation() {
        txtS7Steps.setText("""
            MO PHONG S7 - CONJUNCTIVE SELECTION (AND)
            ============================================================

            Truy van can xu ly:
              Dno = 5 AND Salary > 30000 AND Sex = 'F'

            PHUONG AN: SU DUNG MOT INDEX PHU HOP

            Buoc 1: Su dung Index tren cot Dno.
                    Tim cac ban ghi ung vien co Dno = 5.

                            |
                            v

            Buoc 2: Kiem tra dieu kien Salary > 30000
                    tren nhung ban ghi ung vien.

                            |
                            v

            Buoc 3: Kiem tra tiep dieu kien Sex = 'F'.

                            |
                            v

            KET QUA: Chi giu cac ban ghi thoa man CA BA dieu kien.

            GHI CHU
            - S7 khong co mot cong thuc chi phi I/O co dinh rieng.
            - Chi phi phu thuoc vao Index va so ban ghi ung vien.
            - Day la mo phong ly thuyet, khong truy van SQL Server.
            - Muon xem Logical Reads va Actual Plan, vao tab
              'Thuc nghiem SQL Server' va chon truy van AND.
            """);
        txtS7Steps.setCaretPosition(0);
        resultCardLayout.show(resultCards, "S7");
        lblRecommendation.setText(
                "S7: Dung Index Dno, sau do loc Salary va Sex; khong tinh I/O rieng.");
    }
}
