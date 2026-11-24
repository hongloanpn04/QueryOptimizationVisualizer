CREATE DATABASE QueryOptimizationDB;
GO

USE QueryOptimizationDB;
GO

CREATE TABLE DEPARTMENT (
    Dnumber INT PRIMARY KEY,
    Dname NVARCHAR(100) NOT NULL
);

INSERT INTO DEPARTMENT (Dnumber, Dname)
VALUES
(1, N'Nhân sự'),
(2, N'Kế toán'),
(3, N'Kỹ thuật'),
(4, N'Kinh doanh'),
(5, N'Nghiên cứu');

CREATE TABLE EMPLOYEE (
    Ssn INT PRIMARY KEY,
    Fname NVARCHAR(100) NOT NULL,
    Salary INT NOT NULL,
    Dno INT NOT NULL,
    Sex CHAR(1) NOT NULL,

    FOREIGN KEY (Dno)
        REFERENCES DEPARTMENT(Dnumber)
);

DECLARE @i INT = 1;

WHILE @i <= 100000
BEGIN
    INSERT INTO EMPLOYEE (Ssn, Fname, Salary, Dno, Sex)
    VALUES (
        @i,
        CONCAT(N'Nhân viên ', @i),
        20000 + (@i % 80000),
        (@i % 5) + 1,
        CASE
            WHEN @i % 2 = 0 THEN 'F'
            ELSE 'M'
        END
    );

    SET @i = @i + 1;
END;

SELECT COUNT(*) AS TotalEmployees
FROM EMPLOYEE;

SELECT TOP 10 *
FROM EMPLOYEE;

SET STATISTICS IO ON;
SET STATISTICS TIME ON;

SELECT *
FROM EMPLOYEE
WHERE Ssn = 10001;

SELECT *
FROM EMPLOYEE WITH (INDEX(0))
WHERE Ssn = 10001;