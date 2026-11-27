# Query Optimization Visualizer
Ứng dụng desktop (Java Swing) giúp **trực quan hóa và so sánh chi phí truy vấn cơ sở dữ liệu**: vừa **mô phỏng lý thuyết** theo các công thức I/O trong giáo trình, vừa **thực nghiệm trên Microsoft SQL Server** để đối chiếu với những gì hệ quản trị CSDL thực sự làm (Actual Execution Plan, Logical Reads, Physical Reads, thời gian thực thi).
## Video Demo Chương Trình
[Video demo Query Optimization Visualizer trên YouTube](https://youtu.be/6lhsLiYfTIk)

---
## 📑 Mục lục
1. [Giới thiệu](#1-giới-thiệu)
2. [Mục tiêu và ý nghĩa](#2-mục-tiêu-và-ý-nghĩa)
3. [Tính năng chính](#3-tính-năng-chính)
4. [Tab 1 – Mô phỏng lý thuyết](#4-tab-1--mô-phỏng-lý-thuyết)
5. [Tab 2 – Thực nghiệm SQL Server](#5-tab-2--thực-nghiệm-sql-server)
6. [Cơ sở dữ liệu thực nghiệm](#6-cơ-sở-dữ-liệu-thực-nghiệm)
7. [Cách hoạt động](#7-cách-hoạt-động)
8. [Kiến trúc và cấu trúc thư mục](#8-kiến-trúc-và-cấu-trúc-thư-mục)
9. [Công nghệ sử dụng](#9-công-nghệ-sử-dụng)
10. [Hướng dẫn cài đặt và chạy](#10-hướng-dẫn-cài-đặt-và-chạy)
11. [Hạn chế và lưu ý](#11-hạn-chế-và-lưu-ý)

---
## 1. Giới thiệu

Trong môn học về cơ sở dữ liệu, phần *tối ưu hóa truy vấn* (chương 19 của giáo trình) trình bày nhiều phương pháp truy xuất dữ liệu (S1 – S7) cùng các công thức ước lượng chi phí theo **số lần đọc block (I/O)**. 
**Query Optimization Visualizer** được xây dựng để trả lời câu hỏi đó bằng hai hướng song song:

- **Mô phỏng lý thuyết:** người dùng nhập các thông số của bảng (số bản ghi, số block, chiều cao Index), chương trình tính chi phí của từng phương pháp theo công thức và chỉ ra phương pháp rẻ nhất.
- **Thực nghiệm trên SQL Server:** chương trình kết nối trực tiếp bằng JDBC, chạy các truy vấn thật trên bảng 100.000 bản ghi, đo số trang đọc và đọc **Actual Execution Plan** để biết SQL Server *thực sự* dùng toán tử nào (Seek, Scan, Nested Loops, Hash Match, Merge Join…).

Từ đó người học có thể đặt lý thuyết và thực tế cạnh nhau để so sánh, nhận xét.

## 2. Mục tiêu và ý nghĩa

- Giúp **hiểu bản chất** các phương pháp truy xuất S1 – S7 thay vì chỉ học thuộc công thức.
- Thể hiện **sự khác biệt giữa Index Seek và Index Scan** bằng con số cụ thể (Logical Reads).
- Cho thấy **bộ tối ưu của SQL Server tự chọn kế hoạch** như thế nào, và điều gì xảy ra khi bị ép dùng kế hoạch khác bằng *hint*.
- Rèn kỹ năng làm việc với **JDBC, `SET STATISTICS IO`, Execution Plan XML** và xây dựng giao diện desktop bằng **Java Swing**.

## 3. Tính năng chính

| Nhóm | Tính năng |
| --- | --- |
| Mô phỏng lý thuyết | Nhập `r`, `b`, `x`; tự ước lượng `s`; hiển thị SQL minh họa; tính chi phí I/O theo công thức; so sánh các phương pháp và **tô sáng phương pháp chi phí thấp nhất** |
| Thực thi truy vấn | Chạy truy vấn đã chọn trên SQL Server, trả về số bản ghi, Logical Reads, Physical Reads, thời gian thực thi qua JDBC |
| Actual Execution Plan | Lấy plan XML (`SET STATISTICS XML ON`), phân tích thẻ `RelOp` để liệt kê các **toán tử vật lý** thực tế |
| So sánh phương pháp | Chạy cùng một truy vấn theo nhiều kế hoạch (SQL Server tự chọn / ép dùng Index / ép Scan / các thuật toán JOIN) và tổng hợp thành báo cáo |
| Kiểm tra Index | Liệt kê các Index liên quan đến truy vấn đang chọn (tên, loại, unique, cột khóa) kèm giải thích vai trò |
| Liên hệ lý thuyết | Tự gắn toán tử thực tế với mô hình lý thuyết (ví dụ Scan ↔ S1, Index Seek trên Ssn ↔ S3, Index Seek trên Dno/Sex ↔ S6) |
| Trải nghiệm | Mọi thao tác truy vấn chạy bằng `SwingWorker` ở luồng nền nên giao diện **không bị treo** khi đang thực nghiệm |

## 4. Tab 1 – Mô phỏng lý thuyết

Tab này **không truy vấn SQL Server**, chỉ tính toán theo công thức.

### Thông số đầu vào

| Ký hiệu | Ý nghĩa | Giá trị mặc định |
| --- | --- | --- |
| `r` | Số bản ghi của bảng | 10.000 |
| `b` | Số block chứa dữ liệu | 2.000 |
| `x` | Chiều cao của cây Index | 3 |
| `s` | Số bản ghi thỏa điều kiện (**tự ước lượng**, không cần nhập) | tùy loại truy vấn |
| `bfr` | Hệ số chặn trung bình = `r / b` | tự tính |

### Các loại truy vấn và cách ước lượng `s`

| Loại truy vấn | SQL minh họa | Cách tính `s` | Các phương pháp được so sánh |
| --- | --- | --- | --- |
| Tìm một bản ghi theo khóa duy nhất | `WHERE Ssn = 10001` | `s = 1` | S1a, S1b, S3a, S3b, S6 (unique) |
| Tìm nhiều bản ghi theo thuộc tính không duy nhất | `WHERE Dno = 5` | `s = r / 5` (giả định 5 phòng ban, phân bố đều) | S1a, S5 |
| Truy vấn khoảng trên thuộc tính khóa | `WHERE Ssn > 5000` | `s = r / 2` (giả định 50% bản ghi phù hợp) | S1a, S4 |
| Tìm kiếm trên file được sắp xếp | `WHERE Ssn = 10001` | `s = 1` | S1a, S2 |
| Truy vấn nhiều điều kiện AND (S7) | `WHERE Dno = 5 AND Salary > 30000 AND Sex = 'F'` | không áp dụng | Mô phỏng từng bước lọc (không có công thức I/O riêng) |

### Công thức chi phí được cài đặt (`CostCalculator`)

| Mã | Phương pháp | Công thức (số block đọc) |
| --- | --- | --- |
| **S1a** | Linear Search | `b` |
| **S1b** | Linear Search – trung bình (tìm theo khóa duy nhất) | `b / 2` |
| **S2** | Binary Search (file có thứ tự) | `⌈log₂(b)⌉ + ⌈s / bfr⌉ − 1` |
| **S3a** | Primary Index (một bản ghi) | `x + 1` |
| **S3b** | Hash Key (static hashing, không overflow) | `1` |
| **S4** | Ordering Index (truy vấn khoảng) | `x + b / 2` |
| **S5** | Clustering Index (nhiều bản ghi cùng giá trị) | `x + ⌈s / bfr⌉` |
| **S6** | Secondary Index – thuộc tính khóa (unique) | `x + 1` |
| **S6a** | Secondary Index – không duy nhất (trường hợp xấu nhất) | `x + 1 + s` |

> Chi phí ở tab này là **ước lượng lý thuyết**, không phải chi phí thực tế do SQL Server đo được.

### Mô phỏng S7 – điều kiện AND (Conjunctive Selection)

Với truy vấn `Dno = 5 AND Salary > 30000 AND Sex = 'F'`, chương trình trình bày các bước xử lý:

1. Dùng Index trên `Dno` để lấy các bản ghi ứng viên có `Dno = 5`.
2. Kiểm tra điều kiện `Salary > 30000` trên các ứng viên.
3. Kiểm tra tiếp `Sex = 'F'`.
4. Chỉ giữ lại bản ghi thỏa **cả ba** điều kiện.

S7 không có công thức chi phí I/O cố định, vì chi phí phụ thuộc vào Index được chọn và số bản ghi ứng viên.

## 5. Tab 2 – Thực nghiệm SQL Server

Tab này kết nối SQL Server qua JDBC và có **3 nút chức năng**:

| Nút | Chức năng |
| --- | --- |
| **Thực thi truy vấn** | Chạy truy vấn đã chọn, hiển thị số bản ghi, Logical Reads, Physical Reads và thời gian thực thi (Java/JDBC) |
| **Kiểm tra Index** | Liệt kê các Index liên quan đến truy vấn (tên, loại CLUSTERED/NONCLUSTERED, unique, cột khóa) và giải thích vai trò của chúng |
| **So sánh phương pháp** | Chạy cùng truy vấn theo nhiều kế hoạch khác nhau, lấy Actual Plan cho từng kế hoạch rồi lập báo cáo so sánh |

### 5 kịch bản thực nghiệm

| STT | Truy vấn | Các phương án được so sánh |
| --- | --- | --- |
| 1 | `WHERE Ssn = 10001` | SQL Server tự chọn (Clustered Index Seek) **vs** ép quét bảng bằng `INDEX(0)` |
| 2 | `WHERE Dno = 5` | Tự chọn **vs** ép dùng `IX_QOV_EMPLOYEE_Dno` **vs** ép Scan `INDEX(0)` |
| 3 | `WHERE Sex = 'F'` | Tự chọn **vs** ép dùng `IX_QOV_EMPLOYEE_Sex` **vs** ép Scan `INDEX(0)` |
| 4 | `WHERE Dno = 5 AND Salary > 30000 AND Sex = 'F'` (S7) | Tự chọn **vs** ép dùng Index `Dno` **vs** ép Scan `INDEX(0)` |
| 5 | `EMPLOYEE JOIN DEPARTMENT ON Dno = Dnumber` | Tự chọn **vs** `LOOP JOIN` **vs** `HASH JOIN` **vs** `MERGE JOIN` |

### Báo cáo cho mỗi phương án gồm

- **Toán tử thực tế** mà SQL Server sử dụng (đọc từ Actual Execution Plan).
- **Liên hệ lý thuyết** (S1, S3, S6, S7…) dựa trên toán tử thực tế.
- **Số bản ghi** trả về.
- **Logical Reads**, **Physical Reads**, **Read-ahead Reads**.
- **Thời gian** thực thi đo bằng Java/JDBC (ms).
- **Nhận xét** tự động: phương án có Logical Reads thấp nhất trong lần chạy đó.

## 6. Cơ sở dữ liệu thực nghiệm

Script tạo dữ liệu nằm trong thư mục [`sql/`](sql/).

**Database:** `QueryOptimizationDB`

```
DEPARTMENT (Dnumber PK, Dname)                      -- 5 phòng ban
EMPLOYEE   (Ssn PK, Fname, Salary, Dno FK, Sex)     -- 100.000 nhân viên
```

Cách sinh dữ liệu cho bảng `EMPLOYEE` (vòng lặp `i = 1 … 100000`):

| Cột | Quy tắc sinh |
| --- | --- |
| `Ssn` | `i` (khóa chính → Clustered Index) |
| `Fname` | `N'Nhân viên ' + i` |
| `Salary` | `20000 + (i % 80000)` |
| `Dno` | `(i % 5) + 1` → 5 phòng ban phân bố đều |
| `Sex` | `'F'` nếu `i` chẵn, ngược lại `'M'` → 50/50 |

**Các Index phục vụ thực nghiệm** (`sql/create_experiment_indexes.sql`):

| Index | Loại | Cột | Mục đích |
| --- | --- | --- | --- |
| `PK` trên `Ssn` | Clustered | `Ssn` | Tìm theo khóa chính (Seek) |
| `IX_QOV_EMPLOYEE_Dno` | Nonclustered | `Dno` | Thực nghiệm truy vấn theo phòng ban và điều kiện AND |
| `IX_QOV_EMPLOYEE_Sex` | Nonclustered | `Sex` | Thực nghiệm truy vấn theo giới tính (độ chọn lọc thấp) |

Script cũng xóa `IX_QOV_EMPLOYEE_Dno_Sex_Salary` (Composite Index) nếu đã tồn tại, để demo chỉ dùng Index đơn.

## 7. Cách hoạt động 

```
┌──────────────┐   chọn truy vấn   ┌─────────────────────────┐
│ ExperimentPanel │ ─────────────► │ ExperimentComparisonService │
└──────────────┘                   └────────────┬────────────┘
                                                │ với mỗi phương án
                      ┌─────────────────────────┴───────────────────────┐
                      ▼                                                 ▼
        QueryExecutionService                               ActualPlanService
   SET STATISTICS IO ON → chạy truy vấn              SET STATISTICS XML ON → chạy truy vấn
   → đọc SQLWarning → regex tách                     → lấy ResultSet "showplan" → parse XML
   logical/physical/read-ahead reads                 → đọc thuộc tính PhysicalOp của <RelOp>
   → đo thời gian bằng System.nanoTime()
                      └─────────────────────────┬───────────────────────┘
                                                ▼
                                  Báo cáo: toán tử thực tế + lượt đọc + thời gian
                                  + liên hệ lý thuyết + nhận xét
```

Chú ý:

- **Đo I/O:** bật `SET STATISTICS IO ON`, SQL Server trả thống kê qua `SQLWarning`; chương trình dùng **regex** để tách `logical reads`, `physical reads`, `read-ahead reads` và cộng dồn cho nhiều bảng.
- **Lấy Actual Plan:** bật `SET STATISTICS XML ON`, duyệt các `ResultSet` trả về, nhận diện cột có tên chứa `showplan`, rồi phân tích XML bằng DOM.
- **An toàn khi parse XML:** vô hiệu hóa DOCTYPE và external entity (chống XXE).
- **An toàn khi truy vấn:** kết nối đặt `setReadOnly(true)`, đặt `setQueryTimeout(30)` giây và luôn tắt `STATISTICS` sau khi đo.
- **Giao diện mượt:** các tác vụ nặng chạy trong `SwingWorker`, nút bấm bị khóa trong lúc chờ.
- **Plan lấy ở lần chạy riêng:** Actual Plan được lấy ở một lần thực thi khác với lần đo I/O để không làm lệch kết quả đo.

## 8. Kiến trúc và cấu trúc thư mục

```
QueryOptimizationVisualizer/
├── demo/
│   └── demo.mp4                       # Video demo chương trình
├── sql/
│   ├── queryoptimization.sql          # Tạo DB, bảng, 100.000 bản ghi mẫu
│   └── create_experiment_indexes.sql  # Tạo/kiểm tra các Index phục vụ thực nghiệm
├── src/
│   ├── app/
│   │   └── Main.java                  # Điểm khởi chạy
│   ├── database/
│   │   └── DatabaseConnection.java    # Chuỗi kết nối JDBC tới SQL Server
│   ├── model/
│   │   ├── QueryStatistics.java       # Thông số r, b, x, s (có kiểm tra hợp lệ)
│   │   └── CostResult.java            # Kết quả chi phí của một phương pháp
│   ├── service/
│   │   ├── CostCalculator.java              # Công thức chi phí S1 – S6
│   │   ├── QueryExecutionService.java       # Chạy truy vấn, đo Logical/Physical Reads, thời gian
│   │   ├── ActualPlanService.java           # Lấy và phân tích Actual Execution Plan (XML)
│   │   ├── IndexMetadataService.java        # Đọc danh sách Index từ sys.indexes
│   │   └── ExperimentComparisonService.java # Điều phối 5 kịch bản thực nghiệm và lập báo cáo
│   ├── ui/
│   │   ├── MainFrame.java             # Cửa sổ chính (2 tab)
│   │   ├── SimulationPanel.java       # Tab mô phỏng lý thuyết
│   │   └── ExperimentPanel.java       # Tab thực nghiệm SQL Server
│   └── util/
│       └── FormatUils.java
├── nbproject/                         # Cấu hình dự án Apache NetBeans
├── build.xml
└── manifest.mf
```

Dự án chia theo các lớp rõ ràng: **UI** (Swing) → **Service** (logic tính toán, truy vấn, phân tích plan) → **Database/Model** (kết nối và dữ liệu), giúp tách biệt phần giao diện khỏi phần nghiệp vụ.

## 9. Công nghệ sử dụng

| Công nghệ | Vai trò |
| --- | --- |
| **Java** (Swing, AWT) | Xây dựng giao diện desktop |
| **JDBC** + `mssql-jdbc` | Kết nối và truy vấn SQL Server |
| **Microsoft SQL Server** | Cơ sở dữ liệu thực nghiệm; cung cấp `STATISTICS IO`, `STATISTICS XML` |
| **T-SQL** | Tạo dữ liệu, tạo Index, hint truy vấn (`INDEX(...)`, `OPTION (LOOP/HASH/MERGE JOIN)`) |
| **DOM XML Parser** | Phân tích Actual Execution Plan |
| **Apache NetBeans** | IDE phát triển, build bằng Ant |

## 10. Hướng dẫn cài đặt và chạy

### Yêu cầu

- **JDK 17 trở lên** (mã nguồn dùng text block và switch expression; cấu hình NetBeans hiện đặt `javac.source=26`, bạn có thể chỉnh lại cho khớp JDK đang dùng).
- **Microsoft SQL Server** (bản Express/Developer đều được) và **SQL Server Management Studio (SSMS)**.
- **Microsoft JDBC Driver for SQL Server** (`mssql-jdbc-*.jar`).
- **Apache NetBeans** (khuyến nghị, vì dự án dùng cấu trúc NetBeans/Ant).

### Bước 1 – Tải mã nguồn

```bash
git clone https://github.com/<ten-tai-khoan>/QueryOptimizationVisualizer.git
cd QueryOptimizationVisualizer
```

### Bước 2 – Tạo cơ sở dữ liệu

Mở SSMS và chạy lần lượt:

1. `sql/queryoptimization.sql` – tạo database `QueryOptimizationDB`, hai bảng và nạp 100.000 nhân viên (vòng lặp nên có thể mất một lúc).
2. `sql/create_experiment_indexes.sql` – tạo hai Index `IX_QOV_EMPLOYEE_Dno`, `IX_QOV_EMPLOYEE_Sex` (**bắt buộc** nếu muốn chạy thực nghiệm Dno / Sex / AND).

### Bước 3 – Cấu hình kết nối

Mở `src/database/DatabaseConnection.java` và sửa tên máy chủ cho phù hợp:

```java
private static final String URL =
        "jdbc:sqlserver://<TEN-MAY-CHU-CUA-BAN>;"
        + "databaseName=QueryOptimizationDB;"
        + "integratedSecurity=true;"
        + "encrypt=true;"
        + "trustServerCertificate=true;";
```

> Mặc định chương trình dùng **Windows Authentication** (`integratedSecurity=true`), nên cần thêm file `mssql-jdbc_auth-*.dll` (đi kèm bộ driver, thư mục `auth/x64`) vào `java.library.path`.
> Nếu dùng **SQL Server Authentication**, hãy bỏ `integratedSecurity=true` và thêm `user=...;password=...;` vào chuỗi kết nối.

Có thể kiểm tra nhanh kết nối bằng cách chạy `DatabaseConnection.main()` – chương trình sẽ in ra kết quả kết nối.

### Bước 4 – Thêm driver và chạy

1. Mở project bằng NetBeans, thêm `mssql-jdbc-*.jar` vào **Libraries** (đường dẫn hiện trong `nbproject/project.properties` là đường dẫn máy tác giả, cần chỉnh lại).
2. Nếu dùng Windows Authentication, thêm vào *VM Options*:
   `-Djava.library.path="<đường-dẫn-tới>\auth\x64" --enable-native-access=ALL-UNNAMED`
3. Chạy lớp `app.Main` (**Run Project**).


## 11. Hạn chế và lưu ý

- Chi phí ở tab mô phỏng là **ước lượng lý thuyết**, không đồng nhất hoàn toàn với toán tử của SQL Server.
- Giá trị `s` được **giả định** (ví dụ phân bố đều, 50% bản ghi) chứ không đọc từ thống kê thật.
- Thời gian hiển thị là thời gian đo **từ Java/JDBC**, bao gồm cả việc đọc kết quả, không phải CPU time của SQL Server.
- **Physical Reads có thể bằng 0** khi dữ liệu đã nằm trong buffer cache; kết quả phụ thuộc dữ liệu, Index, bộ nhớ đệm và môi trường chạy.
- `INDEX(0)` ép SQL Server quét bảng/clustered index, **không** có nghĩa là xóa các Index vật lý.
- Các **hint** (`INDEX`, `LOOP/HASH/MERGE JOIN`) chỉ dùng để thử nghiệm; không nên áp dụng bừa bãi trong hệ thống thực.
- Chuỗi kết nối hiện được cố định trong mã nguồn (tên máy chủ, Windows Authentication) nên cần chỉnh khi chạy trên máy khác.
- Hàm `calculateS6` (Secondary Index không duy nhất, `x + 1 + s`) đã được cài đặt trong `CostCalculator` nhưng chưa được đưa lên giao diện mô phỏng.






