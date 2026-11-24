package service;

import java.util.ArrayList;
import java.util.List;

/**
 * Dieu phoi cac kich ban thuc nghiem SQL Server trong tab Experiment.
 * Su dung QueryExecutionService va ActualPlanService da co trong project.
 */
public class ExperimentComparisonService {

    private final QueryExecutionService queryService;
    private final ActualPlanService planService;

    public ExperimentComparisonService() {
        this.queryService = new QueryExecutionService();
        this.planService = new ActualPlanService();
    }

    /**
     * selectedIndex tuong ung ComboBox trong ExperimentPanel:
     * 0=Ssn, 1=Dno, 2=Sex, 3=AND, 4=JOIN.
     */
    public String runExperiment(int selectedIndex) throws Exception {
        switch (selectedIndex) {
            case 0:
                return runSsn();
            case 1:
                return runDno();
            case 2:
                return runSex();
            case 3:
                return runAnd();
            case 4:
                return runJoin();
            default:
                throw new IllegalArgumentException("Loai truy van khong hop le.");
        }
    }

    private String runSsn() throws Exception {
        String seekSql = """
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
        results.add(measure("SQL Server tu chon", seekSql));
        results.add(measure("Ep Scan bang INDEX(0)", scanSql));
        return buildReport("SSN - INDEX SEEK / SCAN", results);
    }

    /** Dno: so sanh Index duoc tao rieng voi phuong an ep Scan. */
    private String runDno() throws Exception {
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
        results.add(measure("Dung Index IX_QOV_EMPLOYEE_Dno", indexedSql));
        results.add(measure("Khong dung Nonclustered Index (INDEX(0))", scanSql));
        return buildReport("DNO - CO INDEX / EP SCAN", results);
    }

    /** Sex: danh gia Index tren cot co do phan biet thap. */
    private String runSex() throws Exception {
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
        results.add(measure("Dung Index IX_QOV_EMPLOYEE_Sex", indexedSql));
        results.add(measure("Khong dung Nonclustered Index (INDEX(0))", scanSql));
        return buildReport("SEX - HIEU QUA SU DUNG INDEX", results);
    }

    /** AND: so sanh scan, Index don Dno va Composite Index. */
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

        String compositeSql = """
            SELECT *
            FROM EMPLOYEE WITH (INDEX(IX_QOV_EMPLOYEE_Dno_Sex_Salary))
            WHERE Dno = 5
              AND Salary > 30000
              AND Sex = 'F'
            """;

        List<CaseResult> results = new ArrayList<>();
        results.add(measure("SQL Server tu chon", baseSql));
        results.add(measure("Ep Scan INDEX(0)", scanSql));
        results.add(measure("Index don tren Dno", singleIndexSql));
        results.add(measure("Composite Index (Dno, Sex, Salary)", compositeSql));
        return buildReport("AND - TRUY VAN NHIEU DIEU KIEN", results);
    }

    /** JOIN: yeu cau SQL Server thu lan luot cac join algorithm. */
    private String runJoin() throws Exception {
        String loopSql = joinSql("LOOP JOIN");
        String hashSql = joinSql("HASH JOIN");
        String mergeSql = joinSql("MERGE JOIN");

        List<CaseResult> results = new ArrayList<>();
        results.add(measure("Nested Loops (LOOP JOIN)", loopSql));
        results.add(measure("Hash Match (HASH JOIN)", hashSql));
        results.add(measure("Merge Join (MERGE JOIN)", mergeSql));
        return buildReport("JOIN - SO SANH JOIN ALGORITHMS", results);
    }

    private String joinSql(String joinHint) {
        return """
            SELECT E.Fname, D.Dname
            FROM EMPLOYEE E
            INNER JOIN DEPARTMENT D
                ON E.Dno = D.Dnumber
            OPTION (""" + joinHint + ")";
    }

    private CaseResult measure(String methodName, String sql) throws Exception {
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

        return new CaseResult(methodName, stats, operatorText);
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
            out.append("Actual Physical Operators: ")
                    .append(item.operators).append("\n");
            out.append("So ban ghi: ").append(s.getRecordCount()).append("\n");
            out.append("Logical Reads: ").append(formatReads(s.getLogicalReads())).append("\n");
            out.append("Physical Reads: ").append(formatReads(s.getPhysicalReads())).append("\n");
            out.append("Read-ahead Reads: ").append(formatReads(s.getReadAheadReads())).append("\n");
            out.append(String.format(java.util.Locale.US,
                    "Thoi gian Java/JDBC: %.3f ms\n", s.getExecutionTime()));
            out.append("------------------------------------------------------------\n\n");
        }

        out.append("NHAN XET\n");
        out.append(buildComment(results));
        out.append("\n\nLUU Y\n");
        out.append("- Actual Plan duoc lay o lan thuc thi rieng bang SET STATISTICS XML ON.\n");
        out.append("- Thoi gian la thoi gian do tu Java/JDBC, khong phai CPU time cua SQL Server.\n");
        out.append("- Physical Reads co the bang 0 khi du lieu da nam trong buffer cache.\n");
        out.append("- INDEX(0) ep SQL Server scan heap/clustered index; khong co nghia la xoa moi Index vat ly.\n");
        out.append("- Cac Index IX_QOV_* can duoc tao bang script SQL di kem truoc khi chay Dno/Sex/AND.\n");
        out.append("- JOIN hints chi dung de thu nghiem, khong ket luan mot join algorithm luon tot hon.\n");
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

    private String formatReads(long value) {
        return value >= 0 ? Long.toString(value) : "Chua lay duoc";
    }

    private static class CaseResult {
        private final String methodName;
        private final QueryExecutionService.ExecutionResult stats;
        private final String operators;

        private CaseResult(String methodName,
                           QueryExecutionService.ExecutionResult stats,
                           String operators) {
            this.methodName = methodName;
            this.stats = stats;
            this.operators = operators;
        }
    }
}
