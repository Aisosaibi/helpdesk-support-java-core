package ng.helpdesk.dtos.requests;

import lombok.Data;
import ng.helpdesk.data.models.Role;

// This request is used when a new customer or agent account is created.
@Data
public class CreateUserRequest {
    private String name;
    private String username;
    private String password;
    private String email;
    private Role role;
}
