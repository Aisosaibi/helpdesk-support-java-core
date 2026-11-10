package ng.helpdesk.controllers;

import ng.helpdesk.dtos.responses.UserResponse;
import ng.helpdesk.exceptions.UserNotFoundException;
import ng.helpdesk.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        try {
            UserResponse response = userService.getUserById(id);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
