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
    void cleanUp() {
        userRepository.deleteAll();
    }

    private CreateUserRequest buildRegisterRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("John Doe");
        request.setUsername("johndoe");
        request.setEmail("john@example.com");
        request.setPassword("pass123");
        request.setRole(Role.CUSTOMER);
        return request;
    }

    // --- register ---

    @Test
    void registerSavesUserAndReturnsResponse() {
        UserResponse response = authService.register(buildRegisterRequest());

        assertNotNull(response);
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(Role.CUSTOMER, response.getRole());
        assertFalse(response.isLoggedIn());
        assertEquals(1, userRepository.count());
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        authService.register(buildRegisterRequest());

        assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(buildRegisterRequest()));
        assertEquals(1, userRepository.count());
    }

    // --- login ---

    @Test
    void loginSetsUserAsLoggedIn() {
        authService.register(buildRegisterRequest());

        LoginUserRequest login = new LoginUserRequest();
        login.setUsername("johndoe");
        login.setPassword("pass123");

        UserResponse response = authService.login(login);

        assertNotNull(response);
        assertTrue(response.isLoggedIn());
    }

    @Test
    void loginThrowsForUnknownUsername() {
        LoginUserRequest login = new LoginUserRequest();
        login.setUsername("nobody");
        login.setPassword("pass123");

        assertThrows(UserNotFoundException.class, () -> authService.login(login));
    }

    @Test
    void loginThrowsForWrongPassword() {
        authService.register(buildRegisterRequest());

        LoginUserRequest login = new LoginUserRequest();
        login.setUsername("johndoe");
        login.setPassword("wrongpass");

        assertThrows(InvalidCredentialsException.class, () -> authService.login(login));
    }

    // --- logout ---

    @Test
    void logoutSetsUserAsLoggedOut() {
        authService.register(buildRegisterRequest());

        LoginUserRequest login = new LoginUserRequest();
        login.setUsername("johndoe");
        login.setPassword("pass123");
        authService.login(login);

        LogoutUserRequest logout = new LogoutUserRequest();
        logout.setUsername("johndoe");

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

    // --- getUserById ---

    @Test
    void getUserByIdReturnsCorrectUser() {
        authService.register(buildRegisterRequest());
        String id = userRepository.findByUsername("johndoe").get().getId();

        UserResponse response = userService.getUserById(id);

        assertNotNull(response);
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    void getUserByIdThrowsWhenNotFound() {
        assertThrows(UserNotFoundException.class,
                () -> userService.getUserById("nonexistent-id"));
    }
}
