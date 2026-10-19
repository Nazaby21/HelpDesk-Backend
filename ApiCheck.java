import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiCheck {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest loginReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"admin1@helpdesk.com\",\"password\":\"admin123\"}"))
            .build();
            
        HttpResponse<String> loginRes = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("LOGIN STATUS: " + loginRes.statusCode());
        String body = loginRes.body();
        if (loginRes.statusCode() != 200) {
            System.out.println("LOGIN FAILED: " + body);
            return;
        }
        
        String token = body.split("accessToken\":\"")[1].split("\\""")[0];
        System.out.println("TOKEN: " + token.substring(0, 10) + "...\n");
        
        HttpRequest catReq = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/api/v1/categories"))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
            
        HttpResponse<String> catRes = client.send(catReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("CAT STATUS: " + catRes.statusCode());
        System.out.println("CAT BODY: " + catRes.body());
    }
}
