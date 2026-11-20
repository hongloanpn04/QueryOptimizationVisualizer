package service;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class IndexMetadataService {

    public static class IndexInfo {

        private final String indexName;
        private final String indexType;
        private final boolean unique;
        private final String columnName;
        private final int keyPosition;

        public IndexInfo(
                String indexName,
                String indexType,
                boolean unique,
                String columnName,
                int keyPosition) {

            this.indexName = indexName;
            this.indexType = indexType;
            this.unique = unique;
            this.columnName = columnName;
            this.keyPosition = keyPosition;
        }

        public String getIndexName() {
            return indexName;
        }

        public String getIndexType() {
            return indexType;
        }

        public boolean isUnique() {
            return unique;
        }

        public String getColumnName() {
            return columnName;
        }

        public int getKeyPosition() {
            return keyPosition;
        }
    }

    /**
     * Lấy danh sách Index của một bảng.
     */
    public List<IndexInfo> getIndexes(String tableName)
            throws SQLException {

        List<IndexInfo> indexes = new ArrayList<>();

        String sql = """
            SELECT
                i.name AS IndexName,
                i.type_desc AS IndexType,
                i.is_unique AS IsUnique,
                c.name AS ColumnName,
                ic.key_ordinal AS KeyPosition
            FROM sys.indexes i
            JOIN sys.index_columns ic
                ON i.object_id = ic.object_id
                AND i.index_id = ic.index_id
            JOIN sys.columns c
                ON ic.object_id = c.object_id
                AND ic.column_id = c.column_id
            WHERE i.object_id = OBJECT_ID(?)
              AND i.index_id > 0
              AND ic.key_ordinal > 0
            ORDER BY i.name, ic.key_ordinal
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "dbo." + tableName);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    indexes.add(new IndexInfo(
                            rs.getString("IndexName"),
                            rs.getString("IndexType"),
                            rs.getBoolean("IsUnique"),
                            rs.getString("ColumnName"),
                            rs.getInt("KeyPosition")
                    ));
                }
            }
        }

        return indexes;
    }
}