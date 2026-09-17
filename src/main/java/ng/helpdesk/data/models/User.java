package ng.helpdesk.data.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

// Every account in the system is stored as a User document in MongoDB.
// This object is the main identity for both customers and agents.
@Document(collection = "users")
@Data
public class User {
    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String username;
    @Indexed(unique = true)
    private String email;
    private String password;
    private Role role;
    private boolean loggedIn;
}
