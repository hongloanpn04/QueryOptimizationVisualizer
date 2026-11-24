package service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Điều phối các kịch bản thực nghiệm SQL Server.
 * So sánh kế hoạch SQL Server tự chọn với các phương án dùng hint.
 */
public class ExperimentComparisonService {

    private final QueryExecutionService queryService;
    private final ActualPlanService planService;

    public ExperimentComparisonService() {
        this.queryService = new QueryExecutionService();
        this.planService = new ActualPlanService();
    }

    /** 0=Ssn, 1=Dno, 2=Sex, 3=AND, 4=JOIN. */
    public String runExperiment(int selectedIndex) throws Exception {
        return switch (selectedIndex) {
            case 0 -> runSsn();
            case 1 -> runDno();
            case 2 -> runSex();
            case 3 -> runAnd();
            case 4 -> runJoin();
            default -> throw new IllegalArgumentException("Loai truy van khong hop le.");
        };
    }

    private String runSsn() throws Exception {
        String normalSql = """
            SELECT *
            FROM EMPLOYEE
            WHERE Ssn = 10001
            """;
        String scanSql = """
            SELECT *
            FROM EMPLOYEE WITH (INDEX(0))
            WHERE Ssn = 10001
            """;
        List<CaseResult> results = new ArrayList<>();
        results.add(measure("SQL Server tu chon", normalSql, 0));
        results.add(measure("Ep Scan bang INDEX(0)", scanSql, 0));
        return buildReport("SSN - INDEX SEEK / SCAN", results);
    }

    /** Dno: thêm kế hoạch mặc định, giữ hai phương án so sánh trước đây. */
    private String runDno() throws Exception {
        String normalSql = """
            SELECT *
            FROM EMPLOYEE
            WHERE Dno = 5
            """;
        String indexedSql = """
            SELECT *
            FROM EMPLOYEE WITH (INDEX(IX_QOV_EMPLOYEE_Dno))
            WHERE Dno = 5
            """;
        String scanSql = """
            SELECT *
            FROM EMPLOYEE WITH (INDEX(0))
            WHERE Dno = 5
            """;
        List<CaseResult> results = new ArrayList<>();
        results.add(measure("SQL Server tu chon (khong hint)", normalSql, 1));
        results.add(measure("Ep dung Index IX_QOV_EMPLOYEE_Dno", indexedSql, 1));
        results.add(measure("Ep Scan bang INDEX(0)", scanSql, 1));
        return buildReport("DNO - TU CHON / EP INDEX / EP SCAN", results);
    }

    /** Sex: thêm kế hoạch mặc định, giữ hai phương án so sánh trước đây. */
    private String runSex() throws Exception {
        String normalSql = """
            SELECT *
            FROM EMPLOYEE
            WHERE Sex = 'F'
            """;
        String indexedSql = """
            SELECT *
            FROM EMPLOYEE WITH (INDEX(IX_QOV_EMPLOYEE_Sex))
            WHERE Sex = 'F'
            """;
        String scanSql = """
            SELECT *
            FROM EMPLOYEE WITH (INDEX(0))
            WHERE Sex = 'F'
            """;
        List<CaseResult> results = new ArrayList<>();
        results.add(measure("SQL Server tu chon (khong hint)", normalSql, 2));
        results.add(measure("Ep dung Index IX_QOV_EMPLOYEE_Sex", indexedSql, 2));
        results.add(measure("Ep Scan bang INDEX(0)", scanSql, 2));
        return buildReport("SEX - TU CHON / EP INDEX / EP SCAN", results);
    }

    /** AND (S7): so sanh SQL Server tu chon, mot Index Dno va ep Scan. */
    private String runAnd() throws Exception {
        String baseSql = """
            SELECT *
            FROM EMPLOYEE
            WHERE Dno = 5
              AND Salary > 30000
              AND Sex = 'F'
            """;
        String scanSql = """
            SELECT *
            FROM EMPLOYEE WITH (INDEX(0))
            WHERE Dno = 5
              AND Salary > 30000
              AND Sex = 'F'
            """;
        String singleIndexSql = """
            SELECT *
            FROM EMPLOYEE WITH (INDEX(IX_QOV_EMPLOYEE_Dno))
            WHERE Dno = 5
              AND Salary > 30000
              AND Sex = 'F'
            """;
        List<CaseResult> results = new ArrayList<>();
        results.add(measure("SQL Server tu chon", baseSql, 3));
        results.add(measure("Ep dung Index Dno", singleIndexSql, 3));
        results.add(measure("Ep Scan INDEX(0)", scanSql, 3));
        return buildReport("AND - TRUY VAN NHIEU DIEU KIEN", results);
    }

    /** JOIN: thêm phương án không có join hint trước ba thuật toán ép buộc. */
    private String runJoin() throws Exception {
        String normalSql = """
            SELECT E.Fname, D.Dname
            FROM EMPLOYEE E
            INNER JOIN DEPARTMENT D
                ON E.Dno = D.Dnumber
            """;
        String loopSql = joinSql("LOOP JOIN");
        String hashSql = joinSql("HASH JOIN");
        String mergeSql = joinSql("MERGE JOIN");
        List<CaseResult> results = new ArrayList<>();
        results.add(measure("SQL Server tu chon (khong hint)", normalSql, 4));
        results.add(measure("Nested Loops (LOOP JOIN)", loopSql, 4));
        results.add(measure("Hash Match (HASH JOIN)", hashSql, 4));
        results.add(measure("Merge Join (MERGE JOIN)", mergeSql, 4));
        return buildReport("JOIN - TU CHON / CAC JOIN ALGORITHMS", results);
    }

    private String joinSql(String joinHint) {
        return """
            SELECT E.Fname, D.Dname
            FROM EMPLOYEE E
            INNER JOIN DEPARTMENT D
                ON E.Dno = D.Dnumber
            OPTION (""" + joinHint + ")";
    }

    private CaseResult measure(String methodName, String sql, int queryType) throws Exception {
        QueryExecutionService.ExecutionResult stats =
                queryService.measureQuery(methodName, sql);

        String operatorText = "Khong lay duoc Actual Execution Plan";
        try {
            String xml = planService.getActualPlan(sql);
            if (xml != null && !xml.isBlank()) {
                List<String> operators = planService.getPhysicalOperators(xml);
                if (!operators.isEmpty()) {
                    operatorText = String.join(", ", operators);
                } else {
                    operatorText = "Khong tim thay Physical Operator trong Plan";
                }
            }
        } catch (Exception ex) {
            operatorText = "Loi lay Plan: " + ex.getMessage();
        }
        return new CaseResult(methodName, stats, operatorText, theoryFor(queryType, operatorText));
    }

    private String buildReport(String title, List<CaseResult> results) {
        StringBuilder out = new StringBuilder();
        out.append("KET QUA THUC NGHIEM: ").append(title).append("\n");
        out.append("============================================================\n\n");
        for (int i = 0; i < results.size(); i++) {
            CaseResult item = results.get(i);
            QueryExecutionService.ExecutionResult s = item.stats;
            out.append("PHUONG PHAP ").append(i + 1).append(": ")
                    .append(item.methodName).append("\n");
            out.append("Thuc te SQL Server su dung : ").append(item.operators).append("\n");
            out.append("Lien he ly thuyet        : ").append(item.theory).append("\n");
            out.append("So ban ghi: ").append(s.getRecordCount()).append("\n");
            out.append("Logical Reads: ").append(formatReads(s.getLogicalReads())).append("\n");
            out.append("Physical Reads: ").append(formatReads(s.getPhysicalReads())).append("\n");
            out.append("Read-ahead Reads: ").append(formatReads(s.getReadAheadReads())).append("\n");
            out.append(String.format(Locale.US,
                    "Thoi gian Java/JDBC: %.3f ms\n", s.getExecutionTime()));
            out.append("------------------------------------------------------------\n\n");
        }
        out.append("NHAN XET\n").append(buildComment(results));
        out.append("\n\nLUU Y\n");
        out.append("- Actual Plan duoc lay o lan thuc thi rieng bang SET STATISTICS XML ON.\n");
        out.append("- Thoi gian la thoi gian do tu Java/JDBC, khong phai CPU time cua SQL Server.\n");
        out.append("- Physical Reads co the bang 0 khi du lieu da nam trong buffer cache.\n");
        out.append("- INDEX(0) ep SQL Server scan heap/clustered index; khong co nghia la xoa moi Index vat ly.\n");
        out.append("- Cac Index IX_QOV_* can duoc tao bang script SQL truoc khi chay Dno/Sex/AND.\n");
        out.append("- JOIN hints chi dung de thu nghiem, khong ket luan mot join algorithm luon tot hon.\n");
        out.append("- S1-S7 la mo hinh ly thuyet; khong dong nhat hoan toan voi operator cua SQL Server.\n");
        out.append("- Lien he ly thuyet dua tren operator; neu khong lay duoc Plan thi khong suy dien.\n");
        return out.toString();
    }

    private String buildComment(List<CaseResult> results) {
        long minReads = Long.MAX_VALUE;
        String bestMethod = null;
        boolean allAvailable = true;
        for (CaseResult item : results) {
            long reads = item.stats.getLogicalReads();
            if (reads < 0) {
                allAvailable = false;
            } else if (reads < minReads) {
                minReads = reads;
                bestMethod = item.methodName;
            }
        }
        if (!allAvailable || bestMethod == null) {
            return "Chua du thong ke Logical Reads de xac dinh phuong an co so lan doc trang thap nhat. Hay kiem tra STATISTICS IO va quyen ket noi.";
        }
        return "Trong lan chay nay, phuong phap co Logical Reads thap nhat la: "
                + bestMethod + " (" + minReads + " reads). Hay doi chieu them Actual Plan va thoi gian; ket qua co the thay doi theo du lieu va cache.";
    }

    /**
     * Lien he ly thuyet mot cach than trong voi operator trong Actual Plan.
     * Nhieu operator co the cung xuat hien (vi du Nested Loops + Key Lookup),
     * nen khong mac dinh 'ep index' dong nghia voi S3 hoac S6.
     */
    private String theoryFor(int queryType, String operators) {
        if (queryType == 4) {
            return "Thuat toan JOIN (ngoai pham vi S1-S7)";
        }
        String op = operators.toLowerCase(Locale.ROOT);
        if (op.contains("khong lay duoc") || op.contains("khong tim thay")
                || op.contains("loi lay plan")) {
            return queryType == 3
                    ? "S7 - Dieu kien AND (chua xac dinh cach truy xuat tu Plan)"
                    : "Chua xac dinh - Can xem Actual Execution Plan";
        }
        boolean scan = op.contains("index scan") || op.contains("table scan");
        boolean seek = op.contains("index seek");
        if (queryType == 3) {
            if (scan && !seek) {
                return "S7 - Dieu kien AND; truy xuat tuong tu S1 (Scan)";
            }
            if (seek) {
                return "S7 - Dieu kien AND; su dung Index roi kiem tra dieu kien con lai";
            }
            return "S7 - Dieu kien AND; xem chi tiet Actual Plan";
        }
        if (scan && !seek) {
            return "S1 - Linear Search (lien he voi Scan)";
        }
        if (queryType == 0 && seek) {
            return "S3 - Tim theo khoa bang Index (lien he voi Seek tren Ssn)";
        }
        if ((queryType == 1 || queryType == 2) && seek) {
            return "S6 - Secondary Index (lien he voi Index Seek tren Dno/Sex)";
        }
        return "Chua phan loai ro - Xem Actual Execution Plan";
    }

    private String formatReads(long value) {
        return value >= 0 ? Long.toString(value) : "Chua lay duoc";
    }

    private static class CaseResult {
        private final String methodName;
        private final QueryExecutionService.ExecutionResult stats;
        private final String operators;
        private final String theory;

        private CaseResult(String methodName,
                           QueryExecutionService.ExecutionResult stats,
                           String operators, String theory) {
            this.methodName = methodName;
            this.stats = stats;
            this.operators = operators;
            this.theory = theory;
        }
    }
}
