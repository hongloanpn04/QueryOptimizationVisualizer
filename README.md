# Query Optimization Visualizer — Mô phỏng tối ưu hóa truy vấn

Ứng dụng desktop **Java Swing** phục vụ báo cáo và demo môn **Hệ cơ sở dữ liệu**, tập trung vào **Chương 19: Query Optimization** (đặc biệt là mục **19.3 – tối ưu hóa dựa trên chi phí** và **19.4 – hàm chi phí của phép SELECT**).

Ứng dụng có hai phần độc lập nhưng bổ trợ cho nhau:

1. **Mô phỏng lý thuyết:** tính chi phí truy xuất dữ liệu bằng số lần truy cập block (I/O) theo các công thức đơn giản hóa của Chương 19, xét loại truy vấn và cấu trúc truy xuất *giả định*.
2. **Thực nghiệm SQL Server:** kết nối cơ sở dữ liệu thật bằng JDBC, chạy SELECT, đo Logical/Physical Reads, thời gian phía Java, phân tích Actual Execution Plan và liệt kê Index hiện có.

> **Phạm vi:** Đây là công cụ *học tập và minh họa*, **không phải** một Query Optimizer thay thế SQL Server. Chi phí lý thuyết và số lượt đọc trang SQL Server là hai số đo khác nhau; không nên coi chúng hoàn toàn tương đương.

## 1. Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java, sử dụng text blocks (`"""`) |
| Giao diện | Java Swing (`JFrame`, `JPanel`, `JTable`, `SwingWorker`) |
| IDE / kiểu dự án đang dùng | Apache NetBeans 31, Java Application (Ant) |
| JDK đã chạy thử | JDK 26 |
| Hệ quản trị CSDL | Microsoft SQL Server, thực nghiệm trên `QueryOptimizationDB` |
| Kết nối | Microsoft JDBC Driver for SQL Server (`mssql-jdbc-13.6.0.jre11.jar`) |
| Xác thực | Windows Authentication (Integrated Security) |

JDK 26 và tên máy SQL Server là cấu hình **máy demo**, không phải điều kiện bắt buộc của thuật toán. Để sử dụng text blocks, cần Java 15 trở lên; khi triển khai nên dùng bản JDK phù hợp với JDBC driver và NetBeans đang cài.

## 2. Chức năng

### Tab 1 — Mô phỏng lý thuyết

- Nhập `r` (số bản ghi), `b` (số block dữ liệu), `x` (số mức Index), `s` (số bản ghi phù hợp).
- Chọn một trong bốn tình huống: tìm bản ghi theo khóa duy nhất; tìm nhiều bản ghi theo thuộc tính không duy nhất; truy vấn khoảng theo khóa; tìm trên file đã sắp xếp.
- Đánh dấu cấu trúc truy xuất được **giả định** tồn tại: Primary, Hash, Clustering, Secondary, Ordered.
- Lọc thuật toán theo tình huống và checkbox, tính chi phí I/O, hiển thị công thức và đề xuất phương án có chi phí **ước tính** thấp nhất trong các ứng viên được xét.

#### Các công thức hiện có

| Mã | Phương pháp / trường hợp | Chi phí ước tính (block I/O) | Điều kiện quan trọng |
|---|---|---|---|
| S1a | Linear Search toàn bộ file | `b` | Quét để lấy tất cả bản ghi thỏa điều kiện |
| S1b | Linear Search trung bình | `b / 2` | Tìm một khóa duy nhất, bản ghi **có tồn tại** |
| S2 | Binary Search | `ceil(log2(b)) + ceil(s / bfr) - 1` | File sắp xếp phù hợp theo thuộc tính tìm kiếm |
| S3a | Primary Index | `x + 1` | Truy xuất một bản ghi qua primary index |
| S3b | Hash Key | Xấp xỉ `1` | Trường hợp static/linear hashing đơn giản, không có overflow |
| S4 | Ordering Index | `x + b / 2` | Truy vấn khoảng theo khóa, giả định gần một nửa file phù hợp |
| S5 | Clustering Index | `x + ceil(s / bfr)` | Điều kiện bằng trên thuộc tính có clustering index |
| S6a | Secondary Index, không duy nhất | `x + 1 + s` | Ước tính trường hợp xấu: nhiều bản ghi nằm ở các block khác nhau |
| S6 (Unique) | Secondary Index, khóa duy nhất | `x + 1` | Điều kiện bằng trên khóa có secondary index |

Với `bfr = r / b` là **số bản ghi trung bình/block**. Trong ví dụ Chương 19, mỗi thuộc tính/Index có thể có `x` và `s` riêng; UI hiện sử dụng **một cặp `x`, `s` chung** cho từng lần mô phỏng.

Ví dụ `r = 10000`, `b = 2000`, `x = 3`, `s = 80` cho ra S1a = 2000; S2 = 26; S3a = 4; S4 = 1003; S5 = 19; S6a = 84. **Không được chọn giá trị nhỏ nhất của cả bảng** nếu cấu trúc Index/loại truy vấn không phù hợp.

### Tab 2 — Thực nghiệm SQL Server

- Chọn các truy vấn mẫu: tìm theo `Ssn`, `Dno`, `Sex`, nhiều điều kiện `AND`, hoặc `JOIN`.
- Nút **Thực thi truy vấn**: chạy SELECT và hiển thị số bản ghi, thời gian qua JDBC cùng thống kê I/O.
- Nút **So sánh Seek và Scan**: chạy hai truy vấn tìm `Ssn = 10001`:
  - SQL Server **tự chọn** phương án: `SELECT * FROM EMPLOYEE WHERE Ssn = 10001`.
  - Ép quét clustered index để đối chứng: `SELECT * FROM EMPLOYEE WITH (INDEX(0)) WHERE Ssn = 10001`.
- Thu thập `Logical Reads`, `Physical Reads`, `Read-ahead Reads` bằng `SET STATISTICS IO ON` và xử lý thông báo trả về qua JDBC.
- Nhận **Actual Execution Plan XML** bằng `SET STATISTICS XML ON`, đọc các toán tử `PhysicalOp` như `Clustered Index Seek`, `Clustered Index Scan`.
- Nút **Kiểm tra Index**: truy vấn metadata (`sys.indexes`, `sys.index_columns`, `sys.columns`) để liệt kê tên, loại, tính duy nhất và cột khóa Index của bảng `EMPLOYEE`.

**Lưu ý đo lường:** Thời gian hiển thị là thời gian phía **Java/JDBC thực thi và đọc kết quả**, không phải riêng CPU time của SQL Server. Trong phiên bản demo hiện tại, thao tác lấy Actual Plan chạy lại truy vấn **một lần riêng**, nên Plan và thống kê I/O/thời gian không nhất thiết đến từ cùng một lần thực thi. `Physical Reads` có thể biến động theo bộ nhớ đệm. `Logical Reads` là số lượt đọc page từ cache, **không phải số block đọc từ đĩa** trong mô hình lý thuyết.

## 3. Cấu trúc mã nguồn

```text
queryoptimizationvisualizer/
├── src/
│   ├── app/
│   │   └── Main.java
│   ├── database/
│   │   └── DatabaseConnection.java
│   ├── model/
│   │   ├── QueryStatistics.java
│   │   └── CostResult.java
│   ├── service/
│   │   ├── CostCalculator.java
│   │   ├── QueryExecutionService.java
│   │   ├── ActualPlanService.java
│   │   └── IndexMetadataService.java
│   ├── ui/
│   │   ├── MainFrame.java
│   │   ├── SimulationPanel.java
│   │   └── ExperimentPanel.java
│   └── util/
│       └── FormatUtils.java
├── nbproject/                 # Cấu hình dự án NetBeans/Ant (nếu lưu trong repo)
├── build.xml                  # Tác vụ build Ant (nếu có)
└── README.md
```

Các đường dẫn ở trên phản ánh cấu trúc **Source Packages** đang dùng trong NetBeans; đối chiếu với thư mục thật trước khi commit.

### Vai trò từng file

| File | Vai trò |
|---|---|
| `app/Main.java` | Điểm khởi chạy ứng dụng Swing |
| `ui/MainFrame.java` | Cửa sổ chính, chứa hai tab |
| `ui/SimulationPanel.java` | Nhập thông số, chọn trường hợp, hiển thị bảng chi phí và đề xuất |
| `ui/ExperimentPanel.java` | Giao diện truy vấn, so sánh Seek/Scan, kiểm tra Index |
| `model/QueryStatistics.java` | Lưu tham số `r`, `b`, `x`, `s` |
| `model/CostResult.java` | Lưu mã phương pháp, tên, chi phí, công thức, giải thích |
| `service/CostCalculator.java` | Triển khai các công thức S1–S6 |
| `service/QueryExecutionService.java` | Chạy SQL, đếm bản ghi và đo I/O/thời gian |
| `service/ActualPlanService.java` | Lấy XML Actual Execution Plan và phân tích toán tử vật lý |
| `service/IndexMetadataService.java` | Đọc danh sách Index thật từ metadata SQL Server |
| `database/DatabaseConnection.java` | Tạo kết nối JDBC đến SQL Server |
| `util/FormatUtils.java` | Tiện ích định dạng; hiện không phải phần cốt lõi của luồng demo |

## 4. Luồng xử lý

**Mô phỏng:**

`SimulationPanel` → tạo `QueryStatistics` → lọc thuật toán hợp lệ theo tình huống/checkbox → `CostCalculator` → các `CostResult` → hiển thị `JTable` và phương án có chi phí nhỏ nhất trong tập được xét.

**Thực nghiệm:**

`ExperimentPanel` → `QueryExecutionService` / `ActualPlanService` / `IndexMetadataService` → `DatabaseConnection` → SQL Server → hiển thị kết quả. Các truy vấn chạy trong `SwingWorker` để tránh khóa luồng giao diện.

## 5. Chuẩn bị môi trường để chạy

### Yêu cầu

1. Cài NetBeans và JDK phù hợp.
2. Có SQL Server đang chạy, truy cập được từ máy chạy ứng dụng.
3. Có database `QueryOptimizationDB`, tối thiểu bảng `EMPLOYEE` với các cột `Ssn`, `Fname`, `Salary`, `Dno`, `Sex` để chạy các truy vấn mẫu; truy vấn JOIN cần thêm bảng `DEPARTMENT` (`Dnumber`, `Dname`).
4. Thêm thư viện `mssql-jdbc-13.6.0.jre11.jar` vào **Project → Libraries → Add JAR/Folder** (hoặc dùng phiên bản driver tương thích khác).
5. Nếu dùng **Windows Authentication**, cần chuẩn bị thành phần native authentication của Microsoft JDBC Driver phù hợp với kiến trúc máy và cấu hình đường dẫn native library.

### Cấu hình kết nối

Trong `database/DatabaseConnection.java`, chỉnh tên server và database theo môi trường. Ví dụ:

```java
private static final String URL =
        "jdbc:sqlserver://TEN_MAY_HOAC_INSTANCE;"
        + "databaseName=QueryOptimizationDB;"
        + "integratedSecurity=true;"
        + "encrypt=true;"
        + "trustServerCertificate=true;";
```

> `trustServerCertificate=true` chỉ là cấu hình thuận tiện cho môi trường lab nội bộ. Khi triển khai thật, cần xác thực chứng chỉ SQL Server thích hợp. **Không commit mật khẩu, token, file cấu hình bí mật hoặc đường dẫn cá nhân** lên Git.

Với Windows Authentication, có thể cần đặt **VM Options** trong NetBeans, ví dụ:

```text
-Djava.library.path=C:\duong-dan-den-thu-muc-dll
```

Đường dẫn trên chỉ là ví dụ; hãy thay bằng nơi chứa DLL xác thực đúng với máy Windows của người chạy. Nếu kết nối JDBC báo không tải được thư viện native, kiểm tra lại tùy chọn này trước.

### Chạy

1. Mở dự án trong NetBeans.
2. Xác nhận `Main Class` là `app.Main`.
3. Chọn **Clean and Build**.
4. Nhấn **F6 / Run Project**.
5. Mở các tab và thực hiện các ca demo ở mục tiếp theo.

Nếu `Main.java` còn code `System.out.println` dùng trong giai đoạn kiểm tra, nên xóa các đoạn test tạm đó và chỉ giữ đoạn khởi chạy `MainFrame` trước khi nộp repo.

## 6. Kịch bản demo gợi ý (5–7 phút)

### Demo A — Chi phí lý thuyết (1–2 phút)

1. Mở **Mô phỏng lý thuyết**.
2. Nhập `r=10000`, `b=2000`, `x=3` và `s=1` khi tìm khóa duy nhất.
3. Chọn cấu trúc truy xuất đang được *giả định* tồn tại, ví dụ Primary và Secondary; bỏ Hash.
4. Nhấn **Tính chi phí và đề xuất**. Giải thích vì sao Scan phải đọc nhiều block hơn truy xuất qua Index.
5. Đổi sang tìm nhiều bản ghi, đặt `s=80`, bật Clustering và Secondary; so sánh S1a, S5, S6a.

### Demo B — Seek so với Scan (2–3 phút)

1. Mở **Thực nghiệm SQL Server**.
2. Chọn tìm nhân viên theo `Ssn`, nhấn **So sánh Seek và Scan**.
3. Giải thích: hai truy vấn cùng trả một bản ghi, nhưng một truy vấn cho SQL Server tự chọn kế hoạch, truy vấn còn lại ép Scan.
4. Quan sát `Actual Operator`, `Logical Reads`, `Physical Reads`, thời gian Java và nhận xét. Trong các lần đo trước trên dữ liệu demo: **Seek khoảng 3 Logical Reads, Scan khoảng 696**; các giá trị thực nghiệm mới có thể thay đổi.

### Demo C — Index thật (1 phút)

1. Nhấn **Kiểm tra Index**.
2. Quan sát Index `CLUSTERED`, `UNIQUE` trên cột `Ssn` (tên Index do SQL Server cấp có thể khác).
3. Giải thích: *Index tồn tại* không đồng nghĩa *SQL Server chắc chắn chọn Index đó*; phải xem Actual Execution Plan.

## 7. Kiểm thử nghiệm thu trước khi gửi Git

- [ ] Project **Clean and Build** thành công.
- [ ] Mở cửa sổ ứng dụng và chuyển qua lại hai tab bình thường.
- [ ] Mô phỏng với `r=10000, b=2000, x=3, s=80` tính đúng các công thức tương ứng.
- [ ] Chọn khóa duy nhất với `s=1`; nếu `s>1` có thông báo hợp lệ.
- [ ] Bỏ chọn một cấu trúc Index thì thuật toán phụ thuộc cấu trúc đó biến mất khỏi bảng.
- [ ] Đề xuất lấy chi phí nhỏ nhất **trong những ứng viên được xét**, không mặc nhiên chọn S3b.
- [ ] Kết nối SQL Server thành công với máy dùng để demo.
- [ ] Nút **Thực thi truy vấn** trả về bản ghi và thống kê hợp lệ.
- [ ] Nút **So sánh Seek và Scan** hiển thị Logical/Physical Reads, thời gian, Actual Operator.
- [ ] Nút **Kiểm tra Index** hiện metadata bảng `EMPLOYEE`.
- [ ] Ô kết quả có thanh cuộn; người xem nhìn được cả phương pháp 1 và 2.

## 8. Giới hạn và hướng mở rộng

- Cấu trúc Index tại tab mô phỏng do người dùng đánh dấu, **không tự đồng bộ** với metadata SQL Server.
- Một chiều cao Index `x` dùng chung cho các thuật toán; thực tế mỗi Index có thể có chiều cao khác nhau.
- Hệ số chặn `bfr ≈ r/b` là giá trị trung bình theo giả định mô phỏng.
- Các công thức S1–S6 là ước tính I/O cho **những trường hợp cụ thể**, chưa xét mọi loại SELECT, JOIN, phân phối dữ liệu, histogram, CPU, bộ nhớ, bộ đệm và mạng.
- S1a và S1b là hai **trường hợp chi phí của cùng cách tìm Linear Search**, không phải hai loại Index.
- Phần thực nghiệm lấy Plan bằng truy vấn riêng; chưa đồng bộ Plan và I/O cùng một lần thực thi.
- Chưa đo chi phí tối ưu hóa nội bộ của SQL Server và chưa dựng sơ đồ kế hoạch thực thi dạng đồ họa ngay trong Java.

Các hướng mở rộng là tùy chọn; **không bắt buộc cho phiên bản demo hiện tại**.

## 9. Lưu ý khi đưa lên GitHub

- Nên commit mã nguồn, `README.md`, `build.xml` và cấu hình dự án cần thiết để mở trong NetBeans.
- Không commit thư mục sinh ra khi build (`build/`, `dist/`), file log và cấu hình cá nhân nhạy cảm.
- Không đưa mật khẩu hay tên đăng nhập thực tế vào README.
- Thành viên khác cần đổi `DatabaseConnection.java` theo SQL Server của máy mình và chuẩn bị dữ liệu mẫu; ứng dụng **không tự tạo database**.
- Nếu nhóm muốn kết quả giống nhau, nên bổ sung một file SQL tạo bảng và sinh dữ liệu mẫu trong một commit riêng, sau khi kiểm tra an toàn.

---

**Tóm tắt:** Demo thể hiện đường đi từ *ước tính chi phí lý thuyết* → *lọc các cách truy xuất khả thi theo giả định* → *đo truy vấn SQL Server thật* → *kiểm tra Actual Execution Plan và Index đang tồn tại*.
