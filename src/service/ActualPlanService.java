package service;

import database.DatabaseConnection;

import java.io.StringReader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ActualPlanService {

    /**
     * Thực thi truy vấn SQL và lấy Actual Execution Plan dạng XML.
     *
     * @param sql Câu truy vấn cần kiểm tra
     * @return XML Execution Plan hoặc null nếu không lấy được
     */
    public String getActualPlan(String sql) throws Exception {

        String xmlPlan = null;

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Chỉ sử dụng kết nối để đọc dữ liệu
            conn.setReadOnly(true);

            // Bật Actual Execution Plan dạng XML
            try (Statement setting = conn.createStatement()) {

                setting.execute("SET STATISTICS XML ON");
            }

            try {

                try (Statement stmt = conn.createStatement()) {

                    // Giới hạn thời gian thực thi 30 giây
                    stmt.setQueryTimeout(30);

                    // Thực thi truy vấn
                    boolean hasResult = stmt.execute(sql);

                    // SQL Server có thể trả về nhiều ResultSet
                    while (true) {

                        if (hasResult) {

                            try (ResultSet rs = stmt.getResultSet()) {

                                ResultSetMetaData metadata =
                                        rs.getMetaData();

                                boolean isXmlPlan = false;

                                // Execution Plan XML được trả về
                                // dưới dạng ResultSet có một cột
                                if (metadata.getColumnCount() == 1) {

                                    String columnName =
                                            metadata.getColumnLabel(1);

                                    isXmlPlan =
                                            columnName != null
                                            && columnName.toLowerCase()
                                                    .contains("showplan");
                                }

                                // Đọc dữ liệu của ResultSet
                                while (rs.next()) {

                                    if (isXmlPlan) {

                                        String value = rs.getString(1);

                                        if (value != null
                                                && value.contains("ShowPlanXML")) {

                                            xmlPlan = value;
                                        }
                                    }
                                }
                            }
                        }

                        // Chuyển sang kết quả tiếp theo
                        if (!stmt.getMoreResults()) {

                            // -1 nghĩa là không còn kết quả
                            if (stmt.getUpdateCount() == -1) {
                                break;
                            }

                            hasResult = false;

                        } else {

                            hasResult = true;
                        }
                    }
                }

            } finally {

                // Luôn tắt thống kê XML sau khi sử dụng
                try (Statement setting = conn.createStatement()) {

                    setting.execute("SET STATISTICS XML OFF");
                }
            }
        }

        return xmlPlan;
    }

    /**
     * Phân tích XML Execution Plan.
     *
     * Lấy danh sách các toán tử vật lý mà SQL Server
     * thực sự sử dụng để xử lý truy vấn.
     */
    public List<String> getPhysicalOperators(
            String xmlPlan) throws Exception {

        List<String> operators = new ArrayList<>();

        if (xmlPlan == null || xmlPlan.isBlank()) {
            return operators;
        }

        // Khởi tạo bộ phân tích XML
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        // Cấu hình bảo mật khi xử lý XML
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        // Tạo đối tượng phân tích XML
        DocumentBuilder builder =
                factory.newDocumentBuilder();

        Document document = builder.parse(
                new InputSource(new StringReader(xmlPlan))
        );

        // Lấy tất cả thẻ RelOp
        NodeList nodes =
                document.getElementsByTagNameNS("*", "RelOp");

        // Đọc thuộc tính PhysicalOp
        for (int i = 0; i < nodes.getLength(); i++) {

            Element element =
                    (Element) nodes.item(i);

            String physicalOp =
                    element.getAttribute("PhysicalOp");

            if (!physicalOp.isBlank()) {

                operators.add(physicalOp);
            }
        }

        return operators;
    }

    /**
     * Lọc các toán tử truy xuất dữ liệu.
     *
     * Ví dụ:
     * - Clustered Index Seek
     * - Clustered Index Scan
     * - Index Seek
     * - Index Scan
     * - Table Scan
     */
    public List<String> getAccessOperators(
            String xmlPlan) throws Exception {

        List<String> result = new ArrayList<>();

        // Lấy danh sách tất cả toán tử vật lý
        List<String> operators =
                getPhysicalOperators(xmlPlan);

        for (String op : operators) {

            // Chỉ lấy toán tử Seek hoặc Scan
            if (op.contains("Seek")
                    || op.contains("Scan")) {

                result.add(op);
            }
        }

        return result;
    }
}