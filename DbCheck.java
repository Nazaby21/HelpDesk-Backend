import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/helpdesk_db";
        try (Connection conn = DriverManager.getConnection(url, "postgres", "123");
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories");
            if (rs.next()) {
                System.out.println("CATEGORY_COUNT: " + rs.getInt(1));
            }
            rs.close();
            
            ResultSet rs2 = stmt.executeQuery("SELECT * FROM categories");
            while(rs2.next()){
                System.out.println("CAT: " + rs2.getString("category_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
