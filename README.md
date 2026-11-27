## Phần thực hiện: Mô phỏng chi phí truy vấn và thực nghiệm SQL Server

### 1. Mô phỏng lý thuyết

Xây dựng giao diện Java Swing cho phép mô phỏng chi phí truy xuất dữ liệu theo các phương pháp tìm kiếm trong chương 19.

Các chức năng:
- Nhập số bản ghi (r), số block (b), chiều cao Index (x).
- Tự động ước lượng số bản ghi phù hợp (s) theo giả định của từng loại truy vấn.
- Lựa chọn loại truy vấn và hiển thị câu lệnh SQL minh họa.
- Tính toán chi phí I/O theo công thức lý thuyết.
- So sánh các phương pháp và tô màu phương pháp có chi phí thấp nhất.

Các phương pháp được sử dụng:
- S1a: Linear Search — b.
- S1b: Linear Search trung bình — b/2.
- S2: Binary Search.
- S3a: Primary Index — x + 1.
- S3b: Hash Key — 1.
- S4: Ordering Index — x + b/2.
- S5: Clustering Index — x + ceil(s/bfr).
- S6: Secondary Index trên khóa duy nhất — x + 1.

Lưu ý: Chi phí trong phần mô phỏng là ước lượng lý thuyết,
không phải chi phí thực tế do SQL Server đo được.

### 2. Thực nghiệm SQL Server

Xây dựng chức năng kết nối SQL Server bằng JDBC để thực hiện
truy vấn và quan sát phương pháp truy xuất thực tế.

Các chức năng đã thực hiện:
- Hiển thị câu lệnh SQL của truy vấn được chọn.
- Thực thi truy vấn trên cơ sở dữ liệu.
- Hiển thị số bản ghi trả về.
- Đo Logical Reads và Physical Reads.
- Đo thời gian thực thi qua JDBC.
- Đọc Actual Execution Plan để xác định toán tử thực tế.
- Kiểm tra danh sách Index hiện có của bảng EMPLOYEE.

### 3. So sánh Index Seek và Index Scan

Đã xây dựng trường hợp thực nghiệm:

SELECT *
FROM EMPLOYEE
WHERE Ssn = 10001;

Chương trình so sánh:
- Phương pháp SQL Server tự lựa chọn.
- Phương pháp quét bảng sử dụng INDEX(0).

Kết quả hiển thị:
- Actual Operator.
- Logical Reads.
- Physical Reads.
- Thời gian thực thi.
- Nhận xét về chi phí đọc trang.

Lưu ý: Kết quả phụ thuộc vào dữ liệu, Index, bộ nhớ đệm
và môi trường thực thi của SQL Server.

### 4. Công nghệ sử dụng

- Java Swing: Xây dựng giao diện.
- JDBC: Kết nối cơ sở dữ liệu.
- Microsoft SQL Server: Thực nghiệm truy vấn.
- Apache NetBeans: Phát triển chương trình.

### 5. Phạm vi hoàn thành

- Hoàn thành mô phỏng lý thuyết và so sánh chi phí I/O.
- Hoàn thành chức năng kiểm tra Index.
- Hoàn thành thực nghiệm Seek và Scan theo thuộc tính Ssn.
- Các trường hợp thực nghiệm còn lại được phân chia cho thành viên khác.
  
## Video Demo

[Xem hoặc tải video demo chương trình](demo/demo.mp4)
