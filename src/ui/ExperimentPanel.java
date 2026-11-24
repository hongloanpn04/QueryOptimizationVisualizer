package ui;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

import service.QueryExecutionService;
import service.ActualPlanService;
import service.IndexMetadataService;
import service.ExperimentComparisonService;

public class ExperimentPanel extends JPanel {

    private JComboBox<String> cboQuery;
    private JTextArea txtSQL;
    private JTextArea txtResult;

    private JButton btnExecute;
    private JButton btnCompare;
    private JButton btnCheckIndex;

    private QueryExecutionService queryService;
    private IndexMetadataService indexMetadataService;
    private ActualPlanService actualPlanService;
    private ExperimentComparisonService experimentComparisonService;

    public ExperimentPanel() {
    experimentComparisonService =new ExperimentComparisonService();

    queryService = new QueryExecutionService();
    indexMetadataService = new IndexMetadataService();

    actualPlanService = new ActualPlanService();

    setLayout(new BorderLayout(10, 10));
        

    initComponents();
        
    }

    private void initComponents() {

        // =========================
        // 1. KHU VỰC CHỌN TRUY VẤN
        // =========================

        JPanel topPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        String[] queryOptions = {
            "Tim nhan vien theo Ssn",
            "Tim nhan vien theo phong ban",
            "Tim nhan vien theo gioi tinh",
            "Truy van nhieu dieu kien AND",
            "Truy van JOIN"
        };

        cboQuery = new JComboBox<>(queryOptions);

        topPanel.add(
                new JLabel("Chon truy van:"),
                BorderLayout.WEST
        );

        topPanel.add(
                cboQuery,
                BorderLayout.CENTER
        );
        
        // Nút kiểm tra Index
        btnCheckIndex = new JButton("Kiem tra Index");

        // Nút so sánh Seek và Scan
        btnCompare = new JButton("So sanh Seek va Scan");

        // Panel chứa hai nút
        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 5, 0)
        );

        buttonPanel.add(btnCheckIndex);
        buttonPanel.add(btnCompare);

        // Đưa hai nút lên bên phải
        topPanel.add(
                buttonPanel,
                BorderLayout.EAST
        );

        add(topPanel, BorderLayout.NORTH);

        // =========================
        // 2. KHU VỰC HIỂN THỊ SQL
        // =========================

        txtSQL = new JTextArea(4, 50);

        txtSQL.setEditable(false);

        txtSQL.setFont(
                new Font("Consolas", Font.PLAIN, 14)
        );

        txtSQL.setBorder(
                BorderFactory.createTitledBorder(
                        "Cau truy van SQL"
                )
        );

        // =========================
        // 3. KHU VỰC KẾT QUẢ
        // =========================

        txtResult = new JTextArea();

        txtResult.setEditable(false);

        txtResult.setFont(
                new Font("Consolas", Font.PLAIN, 14)
        );

        txtResult.setBorder(
                BorderFactory.createTitledBorder(
                        "Ket qua thuc nghiem"
                )
        );

        // =========================
        // 4. NÚT THỰC THI
        // =========================

        btnExecute = new JButton("Thuc thi truy van");

        JPanel centerPanel = new JPanel(
                new BorderLayout(10, 10)
        );

        // Ô hiển thị SQL có chiều cao nhỏ
        JScrollPane sqlScroll = new JScrollPane(txtSQL);

        sqlScroll.setPreferredSize(
                new Dimension(0, 130)
        );

        // Ô kết quả có thanh cuộn
        JScrollPane resultScroll = new JScrollPane(txtResult);

        resultScroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        );

        // Panel chứa ô SQL và nút thực thi
        JPanel sqlPanel = new JPanel(
                new BorderLayout(5, 5)
        );

        sqlPanel.add(
                sqlScroll,
                BorderLayout.CENTER
        );

        sqlPanel.add(
                btnExecute,
                BorderLayout.SOUTH
        );

        sqlPanel.setPreferredSize(
                new Dimension(0, 170)
        );

        // Sắp xếp lại bố cục
        centerPanel.add(
                sqlPanel,
                BorderLayout.NORTH
        );

        centerPanel.add(
                resultScroll,
                BorderLayout.CENTER
        );

        add(centerPanel, BorderLayout.CENTER);

        // =========================
        // 5. XỬ LÝ SỰ KIỆN
        // =========================

        cboQuery.addActionListener(
                e -> updateSQL()
        );

        btnExecute.addActionListener(
                e -> executeQuery()
        );
        
        btnCompare.addActionListener(
        e -> compareIndexMethods()
        );
        btnCheckIndex.addActionListener(
            e -> checkIndexes()
        );

        updateSQL();
    }

    // ===================================
    // HIỂN THỊ CÂU TRUY VẤN THEO LỰA CHỌN
    // ===================================

    private void updateSQL() {

        int index = cboQuery.getSelectedIndex();

        String sql = getSelectedSQL(index);

        txtSQL.setText(sql);

        txtResult.setText("");
    }

    // ===================================
    // LẤY CÂU TRUY VẤN
    // ===================================

    private String getSelectedSQL(int index) {

        switch (index) {

            case 0:
                return """
                    SELECT *
                    FROM EMPLOYEE
                    WHERE Ssn = 10001
                    """;

            case 1:
                return """
                    SELECT *
                    FROM EMPLOYEE
                    WHERE Dno = 5
                    """;

            case 2:
                return """
                    SELECT *
                    FROM EMPLOYEE
                    WHERE Sex = 'F'
                    """;

            case 3:
                return """
                    SELECT *
                    FROM EMPLOYEE
                    WHERE Dno = 5
                      AND Salary > 30000
                      AND Sex = 'F'
                    """;

            case 4:
                return """
                    SELECT E.Fname, D.Dname
                    FROM EMPLOYEE E
                    JOIN DEPARTMENT D
                    ON E.Dno = D.Dnumber
                    """;

            default:
                return "";
        }
    }

    // ===================================
    // THỰC THI TRUY VẤN
    // ===================================

    private void executeQuery() {

        int index = cboQuery.getSelectedIndex();

        String sql = getSelectedSQL(index);

        btnExecute.setEnabled(false);
        cboQuery.setEnabled(false);

        txtResult.setText(
                "Dang thuc thi truy van..."
        );

        // Chạy truy vấn ở luồng nền
        // để giao diện không bị treo

        SwingWorker<String, Void> worker =
                new SwingWorker<>() {

            @Override
            protected String doInBackground() {

                try {

                    queryService.executeQuery(sql);

                    int recordCount =
                            queryService.getRecordCount();

                    double executionTime =
                            queryService.getExecutionTime();
                    long logicalReads = queryService.getLogicalReads();
                    long physicalReads = queryService.getPhysicalReads();

                    System.out.println("STATISTICS IO:");
                    System.out.println(queryService.getStatisticsMessage());

                    return String.format(
                        """
                        THUC THI THANH CONG

                        So ban ghi tra ve: %d

                        Logical Reads: %s
                        Physical Reads: %s

                        Thoi gian Java: %.3f ms

                        GHI CHU
                        Logical Reads va Physical Reads
                        duoc lay tu SQL Server.

                        Thoi gian Java bao gom thuc thi
                        va doc ket qua JDBC.
                        """,
                        recordCount,
                        logicalReads >= 0
                                ? Long.toString(logicalReads)
                                : "Chua lay duoc",
                        physicalReads >= 0
                                ? Long.toString(physicalReads)
                                : "Chua lay duoc",
                        executionTime
                );

                } catch (SQLException ex) {

                    return "LOI SQL SERVER:\n"
                            + ex.getMessage();
                }
            }

            @Override
            protected void done() {

                try {

                    txtResult.setText(get());

                } catch (Exception ex) {

                    txtResult.setText(
                            "Loi: " + ex.getMessage()
                    );

                } finally {

                    btnExecute.setEnabled(true);
                    cboQuery.setEnabled(true);
                }
            }
        };

        worker.execute();
    }
    
    private void compareIndexMethods() {

        btnCompare.setEnabled(false);
        btnExecute.setEnabled(false);
        cboQuery.setEnabled(false);

        txtResult.setText("Dang phan tich truy van...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {

            @Override
            protected String doInBackground() {

                try {

                    // =====================================
                    // 1. THỰC NGHIỆM INDEX SEEK
                    // =====================================

                    QueryExecutionService.ExecutionResult seek =
                            queryService.testIndexSeek();

                    // =====================================
                    // 2. THỰC NGHIỆM INDEX SCAN
                    // =====================================

                    QueryExecutionService.ExecutionResult scan =
                            queryService.testIndexScan();

                    // =====================================
                    // 3. LẤY ACTUAL EXECUTION PLAN
                    // =====================================

                    String seekSQL = """
                        SELECT *
                        FROM EMPLOYEE
                        WHERE Ssn = 10001
                        """;

                    String scanSQL = """
                        SELECT *
                        FROM EMPLOYEE WITH (INDEX(0))
                        WHERE Ssn = 10001
                        """;

                    // Giá trị mặc định nếu không lấy được Plan
                    String seekOperator = "Chua xac dinh";
                    String scanOperator = "Chua xac dinh";

                    try {

                        // Lấy Actual Plan của truy vấn 1
                        String seekXML =
                                actualPlanService.getActualPlan(seekSQL);

                        if (seekXML != null) {

                            var operators =
                                    actualPlanService.getAccessOperators(seekXML);

                            if (!operators.isEmpty()) {

                                seekOperator =
                                        String.join(", ", operators);
                            }
                        }

                        // Lấy Actual Plan của truy vấn 2
                        String scanXML =
                                actualPlanService.getActualPlan(scanSQL);

                        if (scanXML != null) {

                            var operators =
                                    actualPlanService.getAccessOperators(scanXML);

                            if (!operators.isEmpty()) {

                                scanOperator =
                                        String.join(", ", operators);
                            }
                        }

                    } catch (Exception ex) {

                        System.out.println(
                                "Khong lay duoc Actual Plan: "
                                + ex.getMessage()
                        );
                    }

                    // =====================================
                    // 4. LẤY KẾT QUẢ I/O
                    // =====================================

                    long seekLogical = seek.getLogicalReads();
                    long scanLogical = scan.getLogicalReads();

                    long seekPhysical = seek.getPhysicalReads();
                    long scanPhysical = scan.getPhysicalReads();

                    double seekTime = seek.getExecutionTime();
                    double scanTime = scan.getExecutionTime();

                    // =====================================
                    // 5. TẠO NHẬN XÉT
                    // =====================================

                    String comment;

                    if (seekLogical < 0 || scanLogical < 0) {

                        comment =
                                "Chua lay duoc day du thong ke I/O.";

                    } else if (seekLogical < scanLogical) {

                        comment =
                                "Phuong phap 1 co chi phi doc trang thap hon.";

                    } else if (seekLogical > scanLogical) {

                        comment =
                                "Phuong phap 2 co chi phi doc trang thap hon.";

                    } else {

                        comment =
                                "Hai phuong phap co chi phi doc trang bang nhau.";
                    }

                    // =====================================
                    // 6. HIỂN THỊ KẾT QUẢ
                    // =====================================

                    return String.format(
                            """
                            KET QUA SO SANH SEEK VA SCAN

                            =========================================

                            PHUONG PHAP 1: SQL SERVER TU CHON

                            Actual Operator: %s

                            So ban ghi: %d

                            Logical Reads: %s
                            Physical Reads: %s

                            Thoi gian Java: %.3f ms

                            =========================================

                            PHUONG PHAP 2: CLUSTERED INDEX SCAN

                            Actual Operator: %s

                            So ban ghi: %d

                            Logical Reads: %s
                            Physical Reads: %s

                            Thoi gian Java: %.3f ms

                            =========================================

                            NHAN XET

                            %s

                            =========================================

                            LUU Y

                            Logical Reads: So luot doc trang tu cache.
                            Physical Reads: So trang doc tu dia.

                            Thoi gian Java la thoi gian JDBC.

                            Actual Operator duoc lay tu
                            Actual Execution Plan cua SQL Server.

                            Phan lay Plan chay truy van rieng,
                            khong phai cung lan do I/O va thoi gian.
                            """,

                            // Phương pháp 1
                            seekOperator,
                            seek.getRecordCount(),
                            seekLogical >= 0
                                    ? Long.toString(seekLogical)
                                    : "Chua lay duoc",
                            seekPhysical >= 0
                                    ? Long.toString(seekPhysical)
                                    : "Chua lay duoc",
                            seekTime,

                            // Phương pháp 2
                            scanOperator,
                            scan.getRecordCount(),
                            scanLogical >= 0
                                    ? Long.toString(scanLogical)
                                    : "Chua lay duoc",
                            scanPhysical >= 0
                                    ? Long.toString(scanPhysical)
                                    : "Chua lay duoc",
                            scanTime,

                            // Nhận xét
                            comment
                    );

                } catch (Exception ex) {

                    return "LOI THUC NGHIEM:\n"
                            + ex.getMessage();
                }
            }

            @Override
            protected void done() {

                try {

                    txtResult.setText(get());
                    txtResult.setCaretPosition(0);

                } catch (Exception ex) {

                    txtResult.setText(
                            "Loi: " + ex.getMessage()
                    );

                } finally {

                    btnCompare.setEnabled(true);
                    btnExecute.setEnabled(true);
                    cboQuery.setEnabled(true);
                }
            }
        };

        worker.execute();
    }
        /**
     * Kiểm tra danh sách Index hiện có
     * trong bảng EMPLOYEE của SQL Server.
     */
    private void checkIndexes() {

        btnCheckIndex.setEnabled(false);
        btnCompare.setEnabled(false);
        btnExecute.setEnabled(false);

        txtResult.setText("Dang kiem tra Index trong SQL Server...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {

            @Override
            protected String doInBackground() {

                try {

                    var indexes =
                            indexMetadataService.getIndexes("EMPLOYEE");

                    StringBuilder result = new StringBuilder();

                    result.append("DANH SACH INDEX CUA BANG EMPLOYEE\n");
                    result.append("====================================\n\n");

                    if (indexes.isEmpty()) {

                        result.append(
                                "Khong tim thay Index hoac khong co quyen xem metadata."
                        );

                        return result.toString();
                    }

                    for (var index : indexes) {

                        result.append("Ten Index: ")
                                .append(index.getIndexName())
                                .append("\n");

                        result.append("Loai: ")
                                .append(index.getIndexType())
                                .append("\n");

                        result.append("Unique: ")
                                .append(index.isUnique() ? "Co" : "Khong")
                                .append("\n");

                        result.append("Cot: ")
                                .append(index.getColumnName())
                                .append("\n");

                        result.append("Vi tri khoa: ")
                                .append(index.getKeyPosition())
                                .append("\n");

                        result.append("------------------------------------\n\n");
                    }

                    result.append("GHI CHU\n\n");

                    result.append(
                            "CLUSTERED: Du lieu duoc to chuc theo clustered index.\n"
                    );

                    result.append(
                            "NONCLUSTERED: Index rieng de tim kiem du lieu.\n"
                    );

                    result.append(
                            "UNIQUE: Khoa Index khong cho phep trung lap.\n\n"
                    );

                    result.append(
                            "Index ton tai khong co nghia SQL Server se su dung.\n"
                    );

                    result.append(
                            "Can xem Actual Execution Plan de biet Index thuc su duoc chon."
                    );

                    return result.toString();

                } catch (Exception ex) {

                    return "LOI KIEM TRA INDEX:\n"
                            + ex.getMessage();
                }
            }

            @Override
            protected void done() {

                try {

                    txtResult.setText(get());

                    // Cuộn lên đầu để xem kết quả
                    txtResult.setCaretPosition(0);

                } catch (Exception ex) {

                    txtResult.setText(
                            "Loi: " + ex.getMessage()
                    );

                } finally {

                    btnCheckIndex.setEnabled(true);
                    btnCompare.setEnabled(true);
                    btnExecute.setEnabled(true);
                }
            }
        };

        worker.execute();
    }
}
