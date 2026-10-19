import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck2 {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/helpdesk_db";
        try (Connection conn = DriverManager.getConnection(url, "postgres", "123");
             Statement stmt = conn.createStatement()) {
            ResultSet rs2 = stmt.executeQuery("SELECT id, category_name, parent_id FROM categories");
            while(rs2.next()){
                System.out.println("CAT: " + rs2.getString("category_name") + " PARENT: " + rs2.getString("parent_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
