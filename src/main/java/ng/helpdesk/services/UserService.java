package ng.helpdesk.services;

import ng.helpdesk.data.models.Role;
import ng.helpdesk.data.models.User;
import ng.helpdesk.data.repositories.UserRepository;
import ng.helpdesk.dtos.responses.UserResponse;
import ng.helpdesk.exceptions.UserNotFoundException;
import ng.helpdesk.utils.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// This service is responsible for non-auth user lookups.
@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getUserById(String id) {
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }
        return Mapper.mapToUser(found.get());
    }

    public List<UserResponse> getAllAgents() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == Role.AGENT || user.getRole() == Role.ADMIN)
                .map(Mapper::mapToUser)
                .collect(Collectors.toList());
    }
}
