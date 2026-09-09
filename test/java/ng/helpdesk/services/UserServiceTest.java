package ng.helpdesk.services;

import ng.helpdesk.data.models.Role;
import ng.helpdesk.data.repositories.UserRepository;
import ng.helpdesk.dtos.requests.CreateUserRequest;
import ng.helpdesk.dtos.requests.LoginUserRequest;
import ng.helpdesk.dtos.requests.LogoutUserRequest;
import ng.helpdesk.dtos.responses.UserResponse;
import ng.helpdesk.exceptions.InvalidCredentialsException;
import ng.helpdesk.exceptions.UserAlreadyExistsException;
import ng.helpdesk.exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private CreateUserRequest registerUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Ryan Ariyo");
        request.setUsername("ryannn");
        request.setEmail("ryan@gmail.com");
        request.setPassword("1234");
        request.setRole(Role.CUSTOMER);
        return request;
    }

    @Test
    void registerSavesUserAndReturnsResponse() {
        UserResponse response = authService.register(registerUser());
        assertNotNull(response);
        assertEquals("Ryan Ariyo", response.getName());
        assertEquals("ryan@gmail.com", response.getEmail());
        assertEquals(Role.CUSTOMER, response.getRole());
        assertFalse(response.isLoggedIn());
        assertEquals(1, userRepository.count());
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        authService.register(registerUser());
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerUser()));
        assertEquals(1, userRepository.count());
    }

    @Test
    void loginSetsUserAsLoggedIn() {
        authService.register(registerUser());
        LoginUserRequest login = new LoginUserRequest();
        login.setUsername("ryannn");
        login.setPassword("1234");
        UserResponse response = authService.login(login);
        assertNotNull(response);
        assertTrue(response.isLoggedIn());
    }

    @Test
    void loginThrowsForUnknownUsername() {
        LoginUserRequest login = new LoginUserRequest();
        login.setUsername("nobody");
        login.setPassword("1234");

        assertThrows(UserNotFoundException.class, () -> authService.login(login));
    }

    @Test
    void loginThrowsForWrongPassword() {
        authService.register(registerUser());
        LoginUserRequest login = new LoginUserRequest();
        login.setUsername("ryannn");
        login.setPassword("0000");
        assertThrows(InvalidCredentialsException.class, () -> authService.login(login));
    }

    @Test
    void logoutSetsUserAsLoggedOut() {
        authService.register(registerUser());
        LoginUserRequest login = new LoginUserRequest();
        login.setUsername("ryannn");
        login.setPassword("1234");
        authService.login(login);

        LogoutUserRequest logout = new LogoutUserRequest();
        logout.setUsername("ryannn");
        UserResponse response = authService.logout(logout);
        assertNotNull(response);
        assertFalse(response.isLoggedIn());
    }

    @Test
    void logoutThrowsForUnknownUsername() {
        LogoutUserRequest logout = new LogoutUserRequest();
        logout.setUsername("nobody");
        assertThrows(UserNotFoundException.class, () -> authService.logout(logout));
    }


    @Test
    void getUserByIdReturnsCorrectUser() {
        authService.register(registerUser());
        String id = userRepository.findByUsername("ryannn").get().getId();
        UserResponse response = userService.getUserById(id);
        assertNotNull(response);
        assertEquals("ryan@gmail.com", response.getEmail());
    }

    @Test
    void getUserByIdThrowsWhenNotFound() {
        assertThrows(UserNotFoundException.class, () -> userService.getUserById("nonexistent-id"));
    }
}
