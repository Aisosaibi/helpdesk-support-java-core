package ng.helpdesk.data.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// Comments are attached to a ticket so the customer and support team can discuss progress.
@Document(collection = "comments")
@Data
public class Comment {
    @Id
    private String id;
    private String body;
    private LocalDateTime createdAt;
    private String ticketId;
    private String userId;
}
