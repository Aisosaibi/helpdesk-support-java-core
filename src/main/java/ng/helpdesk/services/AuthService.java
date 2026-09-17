package ng.helpdesk.services;

import ng.helpdesk.data.models.Role;
import ng.helpdesk.data.models.User;
import ng.helpdesk.data.repositories.UserRepository;
import ng.helpdesk.dtos.requests.CreateUserRequest;
import ng.helpdesk.dtos.requests.LoginUserRequest;
import ng.helpdesk.dtos.requests.LogoutUserRequest;
import ng.helpdesk.dtos.responses.UserResponse;
import ng.helpdesk.exceptions.InvalidCredentialsException;
import ng.helpdesk.exceptions.UserAlreadyExistsException;
import ng.helpdesk.exceptions.UserNotFoundException;
import ng.helpdesk.utils.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

// Authentication logic is centralized here so the controllers stay thin.
@Service
public class AuthService {

    private final UserRepository userRepository;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse register(CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request is required");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        Optional<User> existingByEmail = userRepository.findByEmail(request.getEmail());
        if (existingByEmail.isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        Optional<User> existingByUsername = userRepository.findByUsername(request.getUsername());
        if (existingByUsername.isPresent()) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole() == null ? Role.CUSTOMER : request.getRole());
        user.setLoggedIn(false);
        userRepository.save(user);
        return Mapper.mapToUser(user);
    }

    public UserResponse login(LoginUserRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        Optional<User> found = userRepository.findByUsername(request.getUsername());
        if (found.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }

        User user = found.get();
        if (!user.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException("Incorrect password");
        }

        user.setLoggedIn(true);
        userRepository.save(user);
        return Mapper.mapToUser(user);
    }

    public UserResponse logout(LogoutUserRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        Optional<User> found = userRepository.findByUsername(request.getUsername());
        if (found.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }

        User user = found.get();
        user.setLoggedIn(false);
        userRepository.save(user);
        return Mapper.mapToUser(user);
    }
}
