

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.EMPLOYEE')
      AND name = N'IX_QOV_EMPLOYEE_Dno'
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_QOV_EMPLOYEE_Dno
    ON dbo.EMPLOYEE(Dno);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.EMPLOYEE')
      AND name = N'IX_QOV_EMPLOYEE_Sex'
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_QOV_EMPLOYEE_Sex
    ON dbo.EMPLOYEE(Sex);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.EMPLOYEE')
      AND name = N'IX_QOV_EMPLOYEE_Dno_Sex_Salary'
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_QOV_EMPLOYEE_Dno_Sex_Salary
    ON dbo.EMPLOYEE(Dno, Sex, Salary);
END;
GO

-- Kiem tra cac Index vua tao
SELECT
    i.name AS IndexName,
    i.type_desc AS IndexType,
    c.name AS ColumnName,
    ic.key_ordinal AS KeyPosition
FROM sys.indexes i
JOIN sys.index_columns ic
    ON i.object_id = ic.object_id
   AND i.index_id = ic.index_id
JOIN sys.columns c
    ON ic.object_id = c.object_id
   AND ic.column_id = c.column_id
WHERE i.object_id = OBJECT_ID(N'dbo.EMPLOYEE')
  AND i.name LIKE N'IX_QOV_%'
ORDER BY i.name, ic.key_ordinal;
GO
