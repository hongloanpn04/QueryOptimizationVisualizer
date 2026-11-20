package service;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryExecutionService {

    // Số bản ghi trả về
    private int recordCount;

    // Thời gian đo bằng Java (ms)
    private double executionTime;

    // Số lượt đọc trang dữ liệu từ bộ nhớ đệm
    private long logicalReads = -1;

    // Số trang đọc trực tiếp từ đĩa
    private long physicalReads = -1;

    // Số trang được đọc trước
    private long readAheadReads = -1;

    // Thông báo thống kê do SQL Server trả về
    private String statisticsMessage = "";

    /**
     * Thực thi truy vấn và đo chi phí.
     */
    public void executeQuery(String sql) throws SQLException {

        recordCount = 0;
        executionTime = 0;

        logicalReads = -1;
        physicalReads = -1;
        readAheadReads = -1;

        statisticsMessage = "";

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setReadOnly(true);

            // Bật thống kê I/O cho kết nối này
            try (Statement setting = conn.createStatement()) {
                setting.execute("SET STATISTICS IO ON");
            }

            try {
                try (Statement stmt = conn.createStatement()) {

                    stmt.setQueryTimeout(30);

                    long startTime = System.nanoTime();

                    try (ResultSet rs = stmt.executeQuery(sql)) {

                        while (rs.next()) {
                            recordCount++;
                        }
                    }

                    long endTime = System.nanoTime();

                    executionTime =
                            (endTime - startTime) / 1_000_000.0;

                    // Nhận thông báo thống kê từ SQL Server
                    SQLWarning warning = stmt.getWarnings();

                    StringBuilder messages = new StringBuilder();

                    while (warning != null) {

                        messages.append(warning.getMessage())
                                .append("\n");

                        warning = warning.getNextWarning();
                    }

                    statisticsMessage = messages.toString();

                    // Phân tích các chỉ số I/O
                    parseStatistics(statisticsMessage);
                }

            } finally {

                // Tắt thống kê khi đã đo xong
                try (Statement setting = conn.createStatement()) {
                    setting.execute("SET STATISTICS IO OFF");
                }
            }
        }
    }

    /**
     * Đọc logical reads, physical reads và read-ahead reads
     * từ thông báo STATISTICS IO.
     */
    private void parseStatistics(String message) {

        Pattern pattern = Pattern.compile(
                "Table\\s+'[^']+'\\s*\\.\\s*"
                + "Scan count\\s+\\d+\\s*,\\s*"
                + "logical reads\\s+(\\d+)\\s*,\\s*"
                + "physical reads\\s+(\\d+)\\s*,"
                + ".*?read-ahead reads\\s+(\\d+)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(message);

        long totalLogical = 0;
        long totalPhysical = 0;
        long totalReadAhead = 0;

        boolean found = false;

        while (matcher.find()) {

            found = true;

            totalLogical += Long.parseLong(matcher.group(1));

            totalPhysical += Long.parseLong(matcher.group(2));

            totalReadAhead += Long.parseLong(matcher.group(3));
        }

        if (found) {
            logicalReads = totalLogical;
            physicalReads = totalPhysical;
            readAheadReads = totalReadAhead;
        }
    }

    public int getRecordCount() {
        return recordCount;
    }

    public double getExecutionTime() {
        return executionTime;
    }

    public long getLogicalReads() {
        return logicalReads;
    }

    public long getPhysicalReads() {
        return physicalReads;
    }

    public long getReadAheadReads() {
        return readAheadReads;
    }

    public String getStatisticsMessage() {
        return statisticsMessage;
    }

    /**
     * Lưu kết quả một lần thực nghiệm.
     */
    public static class ExecutionResult {

        private final String methodName;
        private final int recordCount;
        private final double executionTime;

        private final long logicalReads;
        private final long physicalReads;
        private final long readAheadReads;

        public ExecutionResult(
                String methodName,
                int recordCount,
                double executionTime,
                long logicalReads,
                long physicalReads,
                long readAheadReads) {

            this.methodName = methodName;
            this.recordCount = recordCount;
            this.executionTime = executionTime;

            this.logicalReads = logicalReads;
            this.physicalReads = physicalReads;
            this.readAheadReads = readAheadReads;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getRecordCount() {
            return recordCount;
        }

        public double getExecutionTime() {
            return executionTime;
        }

        public long getLogicalReads() {
            return logicalReads;
        }

        public long getPhysicalReads() {
            return physicalReads;
        }

        public long getReadAheadReads() {
            return readAheadReads;
        }
    }

    /**
     * Thực thi và lấy kết quả đo.
     */
    public ExecutionResult measureQuery(
            String methodName,
            String sql) throws SQLException {

        executeQuery(sql);

        return new ExecutionResult(
                methodName,
                getRecordCount(),
                getExecutionTime(),
                getLogicalReads(),
                getPhysicalReads(),
                getReadAheadReads()
        );
    }

    /**
     * SQL Server tự chọn phương pháp truy xuất.
     */
    public ExecutionResult testIndexSeek() throws SQLException {

        String sql = """
            SELECT *
            FROM EMPLOYEE
            WHERE Ssn = 10001
            """;

        return measureQuery(
                "SQL Server tu chon",
                sql
        );
    }

    /**
     * Buộc SQL Server quét clustered index.
     */
    public ExecutionResult testIndexScan() throws SQLException {

        String sql = """
            SELECT *
            FROM EMPLOYEE WITH (INDEX(0))
            WHERE Ssn = 10001
            """;

        return measureQuery(
                "Clustered Index Scan",
                sql
        );
    }
}