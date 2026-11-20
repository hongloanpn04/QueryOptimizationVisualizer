package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:sqlserver://DESKTOP-R9QAAPQ;"
            + "databaseName=QueryOptimizationDB;"
            + "integratedSecurity=true;"
            + "encrypt=true;"
            + "trustServerCertificate=true;";

    public static Connection getConnection() throws SQLException {

        return DriverManager.getConnection(URL);
    }

    public static void main(String[] args) {

        try (Connection conn = getConnection()) {

            System.out.println("Ket noi SQL Server thanh cong!");

            System.out.println(
                    "Database: " + conn.getCatalog()
            );

        } catch (SQLException e) {

            System.out.println("Ket noi that bai!");

            System.out.println(e.getMessage());
        }
    }
}