package service;

import model.CostResult;
import model.QueryStatistics;

public class CostCalculator {

    /**
     * S1a - Linear Search.
     *
     * Quét toàn bộ các block dữ liệu.
     */
    public CostResult calculateS1(QueryStatistics stats) {

        double b = stats.getBlockCount();

        return new CostResult(
                "S1a",
                "Linear Search",
                b,
                "b",
                "Quet toan bo cac block du lieu"
        );
    }

    /**
     * S1b - Linear Search trung bình.
     *
     * Áp dụng khi tìm một bản ghi theo khóa duy nhất
     * và bản ghi đó tồn tại.
     */
    public CostResult calculateS1Average(QueryStatistics stats) {

        double b = stats.getBlockCount();

        double cost = b / 2.0;

        return new CostResult(
                "S1b",
                "Linear Search - Average",
                cost,
                "b / 2",
                "Trung binh doc mot nua so block"
        );
    }

    /**
     * S2 - Binary Search.
     *
     * Áp dụng khi file được sắp xếp
     * theo thuộc tính tìm kiếm.
     */
    public CostResult calculateS2(QueryStatistics stats) {

        double b = stats.getBlockCount();
        double r = stats.getRecordCount();
        double s = stats.getMatchingRecords();

        // Hệ số chặn trung bình
        double bfr = r / b;

        // Chi phí tìm kiếm nhị phân
        double searchCost =
                Math.ceil(Math.log(b) / Math.log(2));

        // Số block chứa kết quả
        double resultBlocks =
                Math.ceil(s / bfr);

        double cost =
                searchCost + resultBlocks - 1;

        return new CostResult(
                "S2",
                "Binary Search",
                cost,
                "ceil(log2(b)) + ceil(s/bfr) - 1",
                "Tim kiem nhi phan tren file co thu tu"
        );
    }

    /**
     * S3a - Primary Index.
     *
     * Tìm một bản ghi duy nhất bằng primary index.
     */
    public CostResult calculateS3(QueryStatistics stats) {

        double x = stats.getIndexHeight();

        double cost = x + 1;

        return new CostResult(
                "S3a",
                "Primary Index",
                cost,
                "x + 1",
                "Doc x muc index va mot block du lieu"
        );
    }

    /**
     * S3b - Hash Key.
     *
     * Giả sử sử dụng static hashing
     * và không có overflow.
     */
    public CostResult calculateS3Hash(QueryStatistics stats) {

        double cost = 1;

        return new CostResult(
                "S3b",
                "Hash Key",
                cost,
                "1",
                "Static hashing: uoc tinh mot block I/O"
        );
    }

    /**
     * S4 - Ordering Index.
     *
     * Truy vấn khoảng trên thuộc tính khóa
     * có ordering index.
     *
     * Giả sử khoảng một nửa file thỏa điều kiện.
     */
    public CostResult calculateS4(QueryStatistics stats) {

        double b = stats.getBlockCount();
        double x = stats.getIndexHeight();

        double cost = x + b / 2.0;

        return new CostResult(
                "S4",
                "Ordering Index",
                cost,
                "x + b/2",
                "Tim kiem theo khoang bang ordering index"
        );
    }

    /**
     * S5 - Clustering Index.
     *
     * Truy xuất nhiều bản ghi theo điều kiện
     * bằng trên thuộc tính có clustering index.
     */
    public CostResult calculateS5(QueryStatistics stats) {

        double r = stats.getRecordCount();
        double b = stats.getBlockCount();
        double x = stats.getIndexHeight();
        double s = stats.getMatchingRecords();

        // Hệ số chặn trung bình
        double bfr = r / b;

        // Số block dữ liệu cần đọc
        double resultBlocks =
                Math.ceil(s / bfr);

        double cost = x + resultBlocks;

        return new CostResult(
                "S5",
                "Clustering Index",
                cost,
                "x + ceil(s/bfr)",
                "Doc index va cac block trong cung cluster"
        );
    }

    /**
     * S6a - Secondary Index không duy nhất.
     *
     * Ước tính trường hợp xấu nhất:
     * mỗi bản ghi phù hợp nằm ở một block khác nhau.
     */
    public CostResult calculateS6(QueryStatistics stats) {

        double x = stats.getIndexHeight();
        double s = stats.getMatchingRecords();

        double cost = x + 1 + s;

        return new CostResult(
                "S6a",
                "Secondary Index - Nonunique",
                cost,
                "x + 1 + s",
                "Truong hop xau nhat cua secondary index"
        );
    }

    /**
     * S6 - Secondary Index duy nhất.
     *
     * Tìm một bản ghi theo thuộc tính khóa.
     */
    public CostResult calculateS6Unique(QueryStatistics stats) {

        double x = stats.getIndexHeight();

        double cost = x + 1;

        return new CostResult(
                "S6",
                "Secondary Index - Unique",
                cost,
                "x + 1",
                "Truy xuat mot ban ghi bang secondary index"
        );
    }
}