package ng.helpdesk.dtos.responses;

import lombok.Data;
import ng.helpdesk.data.models.Role;

// UserResponse is the public-facing shape sent back to the browser after login or registration.
// We include username because the front-end stores it directly in localStorage for dashboard state.
@Data
public class UserResponse {
    private String id;
    private String name;
    private String username;
    private String email;
    private Role role;
    private boolean loggedIn;
}
