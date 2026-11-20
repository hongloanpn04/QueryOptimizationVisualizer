package model;

/**
 * Lưu các thông số thống kê của bảng dữ liệu
 * phục vụ việc tính chi phí truy vấn.
 */
public class QueryStatistics {

    // Tổng số bản ghi trong bảng
    private long recordCount;

    // Tổng số block lưu trữ dữ liệu
    private long blockCount;

    // Chiều cao của cây Index
    private int indexHeight;

    // Số bản ghi dự kiến thỏa điều kiện
    private long matchingRecords;

    public QueryStatistics(
            long recordCount,
            long blockCount,
            int indexHeight,
            long matchingRecords) {

        if (recordCount <= 0 || blockCount <= 0
                || indexHeight <= 0 || matchingRecords < 0
                || matchingRecords > recordCount) {
            throw new IllegalArgumentException(
                    "Thong so thong ke khong hop le"
            );
        }

        this.recordCount = recordCount;
        this.blockCount = blockCount;
        this.indexHeight = indexHeight;
        this.matchingRecords = matchingRecords;
    }

    public long getRecordCount() {
        return recordCount;
    }

    public long getBlockCount() {
        return blockCount;
    }

    public int getIndexHeight() {
        return indexHeight;
    }

    public long getMatchingRecords() {
        return matchingRecords;
    }
}