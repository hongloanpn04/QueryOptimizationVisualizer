package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import service.ExperimentComparisonService;
import service.IndexMetadataService;
import service.QueryExecutionService;

/** Tab thuc nghiem SQL Server. */
public class ExperimentPanel extends JPanel {

    private final QueryExecutionService queryService = new QueryExecutionService();
    private final IndexMetadataService indexMetadataService = new IndexMetadataService();
    private final ExperimentComparisonService experimentComparisonService =
            new ExperimentComparisonService();

    private JComboBox<String> cboQuery;
    private JTextArea txtSQL;
    private JTextArea txtResult;
    private JButton btnExecute;
    private JButton btnCompare;
    private JButton btnCheckIndex;

    public ExperimentPanel() {
        setLayout(new BorderLayout(10, 10));
        initComponents();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        cboQuery = new JComboBox<>(new String[]{
            "Tim nhan vien theo Ssn",
            "Tim nhan vien theo phong ban",
            "Tim nhan vien theo gioi tinh",
            "Truy van nhieu dieu kien AND",
            "Truy van JOIN"
        });
        topPanel.add(new JLabel("Chon truy van:"), BorderLayout.WEST);
        topPanel.add(cboQuery, BorderLayout.CENTER);

        btnCheckIndex = new JButton("Kiem tra Index");
        btnCompare = new JButton("So sanh phuong phap");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttons.add(btnCheckIndex);
        buttons.add(btnCompare);
        topPanel.add(buttons, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        txtSQL = new JTextArea(4, 50);
        txtSQL.setEditable(false);
        txtSQL.setFont(new Font("Consolas", Font.PLAIN, 14));
        txtSQL.setBorder(BorderFactory.createTitledBorder("Cau truy van SQL"));

        txtResult = new JTextArea();
        txtResult.setEditable(false);
        txtResult.setFont(new Font("Consolas", Font.PLAIN, 14));
        txtResult.setBorder(BorderFactory.createTitledBorder("Ket qua thuc nghiem"));

        JScrollPane sqlScroll = new JScrollPane(txtSQL);
        sqlScroll.setPreferredSize(new Dimension(0, 130));
        JScrollPane resultScroll = new JScrollPane(txtResult);
        resultScroll.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        btnExecute = new JButton("Thuc thi truy van");
        JPanel sqlPanel = new JPanel(new BorderLayout(5, 5));
        sqlPanel.add(sqlScroll, BorderLayout.CENTER);
        sqlPanel.add(btnExecute, BorderLayout.SOUTH);
        sqlPanel.setPreferredSize(new Dimension(0, 170));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(sqlPanel, BorderLayout.NORTH);
        centerPanel.add(resultScroll, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        cboQuery.addActionListener(e -> updateSQL());
        btnExecute.addActionListener(e -> executeQuery());
        btnCompare.addActionListener(e -> compareIndexMethods());
        btnCheckIndex.addActionListener(e -> checkIndexes());
        updateSQL();
    }

    private void updateSQL() {
        txtSQL.setText(getSelectedSQL(cboQuery.getSelectedIndex()));
        txtResult.setText("");
    }

    private String getSelectedSQL(int index) {
        switch (index) {
            case 0: return "SELECT *\nFROM EMPLOYEE\nWHERE Ssn = 10001";
            case 1: return "SELECT *\nFROM EMPLOYEE\nWHERE Dno = 5";
            case 2: return "SELECT *\nFROM EMPLOYEE\nWHERE Sex = 'F'";
            case 3: return "SELECT *\nFROM EMPLOYEE\nWHERE Dno = 5\n  AND Salary > 30000\n  AND Sex = 'F'";
            case 4: return "SELECT E.Fname, D.Dname\nFROM EMPLOYEE E\nJOIN DEPARTMENT D\nON E.Dno = D.Dnumber";
            default: return "";
        }
    }

    private void setBusy(boolean busy) {
        btnExecute.setEnabled(!busy);
        btnCompare.setEnabled(!busy);
        btnCheckIndex.setEnabled(!busy);
        cboQuery.setEnabled(!busy);
    }

    @FunctionalInterface
    private interface Task {
        String run() throws Exception;
    }

    private void runTask(String waitingMessage, Task task) {
        setBusy(true);
        txtResult.setText(waitingMessage);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return task.run();
                } catch (Exception ex) {
                    return "LOI:\n" + ex.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    txtResult.setText(get());
                    txtResult.setCaretPosition(0);
                } catch (Exception ex) {
                    txtResult.setText("LOI:\n" + ex.getMessage());
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void executeQuery() {
        final String sql = getSelectedSQL(cboQuery.getSelectedIndex());
        runTask("Dang thuc thi truy van...", () -> {
            queryService.executeQuery(sql);
            return String.format(Locale.US,
                "THUC THI THANH CONG\n\nSo ban ghi: %d\nLogical Reads: %s\nPhysical Reads: %s\nThoi gian Java/JDBC: %.3f ms\n\n"
                + "GHI CHU\nThoi gian Java bao gom thuc thi va doc ket qua JDBC.\n",
                queryService.getRecordCount(),
                readsText(queryService.getLogicalReads()),
                readsText(queryService.getPhysicalReads()),
                queryService.getExecutionTime());
        });
    }

    private static String readsText(long reads) {
        return reads >= 0 ? Long.toString(reads) : "Chua lay duoc";
    }

    private void compareIndexMethods() {
        final int selectedIndex = cboQuery.getSelectedIndex();
        runTask("Dang chay thuc nghiem...",
                () -> experimentComparisonService.runExperiment(selectedIndex));
    }

    private void checkIndexes() {
        final int selectedIndex = cboQuery.getSelectedIndex();
        runTask("Dang kiem tra Index lien quan...",
                () -> buildIndexReport(selectedIndex));
    }

    private String buildIndexReport(int selectedIndex) throws SQLException {
        StringBuilder out = new StringBuilder();
        out.append("KIEM TRA INDEX THEO TRUY VAN\n");
        out.append("============================================================\n");
        out.append("Truy van: ").append(cboQuery.getItemAt(selectedIndex))
           .append("\n\n");

        String employeeColumn;
        switch (selectedIndex) {
            case 0: employeeColumn = "Ssn"; break;
            case 1: employeeColumn = "Dno"; break;
            case 2: employeeColumn = "Sex"; break;
            case 3: employeeColumn = "Dno"; break;
            case 4: employeeColumn = "Dno"; break;
            default: throw new IllegalArgumentException("Loai truy van khong hop le.");
        }

        appendIndexes(out, "EMPLOYEE", employeeColumn, selectedIndex);
        if (selectedIndex == 4) {
            appendIndexes(out, "DEPARTMENT", "Dnumber", selectedIndex);
        }

        out.append("\nGHI CHU\n");
        out.append("- CLUSTERED: Du lieu goc duoc to chuc theo khoa Index.\n");
        out.append("  Vi du: tim theo Ssn co the lay truc tiep dong nhan vien.\n");
        out.append("- NONCLUSTERED: Giong muc luc rieng, giup tim khoa\n");
        out.append("  va vi tri du lieu. Vi du: Dno dan den cac Ssn tuong ung.\n");
        out.append("- Co Index khong co nghia SQL Server chac chan dung Index do.\n");
        out.append("- Index Seek: Tim den vung khoa can truy xuat.\n");
        out.append("- Index Scan: Quet cac muc cua Index.\n");
        out.append("- Nhan 'So sanh phuong phap' de xem Actual Plan va chi phi.\n");
        out.append("- Chi hien thi Index lien quan den truy van dang chon.\n");
        return out.toString();
    }

    /** Chi lay Index co cot khoa dau tien la cot can xem; gop cac cot theo ten Index. */
    private void appendIndexes(StringBuilder out, String tableName,
                               String keyColumn, int selectedIndex) throws SQLException {
        List<IndexMetadataService.IndexInfo> rows =
                indexMetadataService.getIndexes(tableName);
        Map<String, List<IndexMetadataService.IndexInfo>> grouped =
                new LinkedHashMap<>();

        for (IndexMetadataService.IndexInfo row : rows) {
            if (row.getKeyPosition() == 1
                    && row.getColumnName().equalsIgnoreCase(keyColumn)) {
                grouped.putIfAbsent(row.getIndexName(), new ArrayList<>());
            }
        }
        for (IndexMetadataService.IndexInfo row : rows) {
            List<IndexMetadataService.IndexInfo> cols = grouped.get(row.getIndexName());
            if (cols != null) cols.add(row);
        }
        // Demo chi dung Index don tren Dno (khong liet ke composite).
        if ("EMPLOYEE".equals(tableName) && "Dno".equalsIgnoreCase(keyColumn)) {
            grouped.entrySet().removeIf(e -> e.getValue().size() != 1);
        }

        out.append("BANG ").append(tableName).append(" - COT ")
           .append(keyColumn).append("\n");
        out.append("------------------------------------------------------------\n");
        if (grouped.isEmpty()) {
            out.append("Khong tim thay Index phu hop voi cot khoa dau tien ")
               .append(keyColumn).append(".\n\n");
            return;
        }

        for (Map.Entry<String, List<IndexMetadataService.IndexInfo>> e : grouped.entrySet()) {
            List<IndexMetadataService.IndexInfo> cols = e.getValue();
            cols.sort(Comparator.comparingInt(IndexMetadataService.IndexInfo::getKeyPosition));
            IndexMetadataService.IndexInfo first = cols.get(0);
            List<String> names = new ArrayList<>();
            for (IndexMetadataService.IndexInfo col : cols) {
                names.add(col.getColumnName());
            }

            out.append("Ten Index : ").append(first.getIndexName()).append('\n');
            out.append("Loai      : ").append(first.getIndexType()).append('\n');
            out.append("Unique    : ").append(first.isUnique() ? "Co" : "Khong").append('\n');
            out.append("Cot khoa  : ").append(String.join(", ", names)).append('\n');
            out.append("\nGIAI THICH\n");
            appendIndexExplanation(out, tableName, keyColumn, first.getIndexType(),
                    selectedIndex);
            out.append("------------------------------------------------------------\n\n");
        }
    }

    /** Chi giai thich loai Index va vai tro trong truy van, khong giai thich UNIQUE. */
    private void appendIndexExplanation(StringBuilder out, String tableName,
                                        String keyColumn, String indexType,
                                        int queryIndex) {
        if ("CLUSTERED".equalsIgnoreCase(indexType)) {
            out.append("- CLUSTERED: Du lieu goc duoc to chuc theo khoa ")
               .append(keyColumn).append(".\n");
        } else if ("NONCLUSTERED".equalsIgnoreCase(indexType)) {
            out.append("- NONCLUSTERED: Giong muc luc rieng, giup tim\n")
               .append("  gia tri khoa va xac dinh noi chua du lieu goc.\n");
        }

        if ("DEPARTMENT".equals(tableName)) {
            out.append("- Vai tro: Tim phong ban theo Dnumber de ghep voi\n")
               .append("  EMPLOYEE.Dno trong truy van JOIN.\n");
            return;
        }

        switch (queryIndex) {
            case 0:
                out.append("- Vai tro: Tim nhan vien co Ssn = 10001.\n")
                   .append("  Neu dung Clustered Index Seek, SQL Server co the\n")
                   .append("  den ngay dong du lieu theo ma Ssn.\n");
                break;
            case 1:
                out.append("- Vai tro: Tim cac nhan vien co Dno = 5.\n")
                   .append("  Index Dno co the giup tim cac ma Ssn tuong ung,\n")
                   .append("  roi tra cuu dong day du khi can SELECT *.\n");
                break;
            case 2:
                out.append("- Vai tro: Tim cac nhan vien co Sex = 'F'.\n")
                   .append("  Neu qua nhieu nhan vien phu hop, quet bang\n")
                   .append("  co the nhanh hon truy cap qua Index.\n");
                break;
            case 3:
                out.append("- Vai tro S7: Dung Index Dno tim nhan vien phong 5,\n")
                   .append("  sau do kiem tra Salary > 30000 va Sex = 'F'.\n")
                   .append("  Khong su dung Composite Index trong demo nay.\n");
                break;
            case 4:
                out.append("- Vai tro: Ho tro tim nhan vien theo Dno de ghep\n")
                   .append("  voi khoa Dnumber cua bang DEPARTMENT.\n");
                break;
            default:
                break;
        }
    }
}
