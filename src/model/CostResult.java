package model;

/**
 * Lưu kết quả tính chi phí của một phương pháp truy xuất.
 */
public class CostResult {

    // Mã phương pháp, ví dụ S1, S3, S4, S6
    private String methodCode;

    // Tên phương pháp truy xuất
    private String methodName;

    // Chi phí ước lượng, tính bằng số lần đọc block
    private double cost;

    // Công thức tính chi phí
    private String formula;

    // Giải thích kết quả
    private String explanation;

    public CostResult(
            String methodCode,
            String methodName,
            double cost,
            String formula,
            String explanation) {

        this.methodCode = methodCode;
        this.methodName = methodName;
        this.cost = cost;
        this.formula = formula;
        this.explanation = explanation;
    }

    public String getMethodCode() {
        return methodCode;
    }

    public String getMethodName() {
        return methodName;
    }

    public double getCost() {
        return cost;
    }

    public String getFormula() {
        return formula;
    }

    public String getExplanation() {
        return explanation;
    }
}