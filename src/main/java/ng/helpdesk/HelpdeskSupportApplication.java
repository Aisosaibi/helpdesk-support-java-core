package ng.helpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;

// This is the application entry point for the whole Spring Boot project.
// It boots the REST API that the browser-based Help Desk frontend is calling.
// The @CrossOrigin annotation is intentionally broad so the static HTML front end
// running from file:// or localhost can access the backend on port 8080.
@SpringBootApplication
@CrossOrigin(origins = "*")
public class HelpdeskSupportApplication {
    public static void main(String[] args) {
        SpringApplication.run(HelpdeskSupportApplication.class, args);
    }
}
