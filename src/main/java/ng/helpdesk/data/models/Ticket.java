package ng.helpdesk.data.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

// A Ticket is the main unit of work in the support system.
// It stores both the original request and the current agent ownership/status.
@Document(collection = "tickets")
@Data
public class Ticket {
    @Id
    private String id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private List<Comment> comments;
    private LocalDateTime createdAt;
    private String customerId;
    private String agentId;
}
