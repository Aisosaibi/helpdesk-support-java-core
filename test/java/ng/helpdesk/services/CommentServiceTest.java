package ng.helpdesk.services;

import ng.helpdesk.data.models.Role;
import ng.helpdesk.data.models.Ticket;
import ng.helpdesk.data.models.User;
import ng.helpdesk.data.repositories.CommentRepository;
import ng.helpdesk.data.repositories.TicketRepository;
import ng.helpdesk.data.repositories.UserRepository;
import ng.helpdesk.dtos.requests.CreateCommentRequest;
import ng.helpdesk.dtos.responses.CommentResponse;
import ng.helpdesk.exceptions.CommentNotFoundException;
import ng.helpdesk.exceptions.TicketNotFoundException;
import ng.helpdesk.exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private String existingUserId;
    private String existingTicketId;

    @BeforeEach
    public void cleanUp() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();

        existingUserId = buildAndSaveUser();
        existingTicketId = buildAndSaveTicket(existingUserId);
    }

    private String buildAndSaveUser() {
        User user = new User();
        user.setName("Godwin David");
        user.setUsername("Swizzy");
        user.setEmail("dave@gmail.com");
        user.setPassword("password123");
        user.setRole(Role.AGENT);
        User saved = userRepository.save(user);
        return saved.getId();
    }

    private String buildAndSaveTicket(String creatorId) {
        Ticket ticket = new Ticket();
        ticket.setTitle("Login issue");
        ticket.setDescription("Can't log in");
        ticket.setStatus("OPEN");
        ticket.setPriority("HIGH");
        ticket.setCustomerId(creatorId);
        Ticket saved = ticketRepository.save(ticket);
        return saved.getId();
    }

    private CreateCommentRequest buildCommentRequest() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setBody("Looking into this now");
        request.setTicketId(existingTicketId);
        request.setUserId(existingUserId);
        return request;
    }



    @Test
    public void postCommentSavesCommentAndReturnsResponse() {
        CommentResponse response = commentService.postComment(buildCommentRequest());

        assertNotNull(response);
        assertEquals("Looking into this now", response.getBody());
        assertEquals(existingTicketId, response.getTicketId());
        assertEquals(existingUserId, response.getUserId());
        assertEquals(1, commentRepository.count());
    }

    @Test
    public void postCommentThrowsWhenTicketDoesNotExist() {
        CreateCommentRequest request = buildCommentRequest();
        request.setTicketId("This ticket does not exist");

        assertThrows(TicketNotFoundException.class,
                () -> commentService.postComment(request));
        assertEquals(0, commentRepository.count());
    }

    @Test
    public void postCommentThrowsWhenAuthorDoesNotExist() {
        CreateCommentRequest request = buildCommentRequest();
        request.setUserId("This author does not exist");

        assertThrows(UserNotFoundException.class,
                () -> commentService.postComment(request));
        assertEquals(0, commentRepository.count());
    }



    @Test
    public void getCommentByIdReturnsCorrectComment() {
        CommentResponse posted = commentService.postComment(buildCommentRequest());

        CommentResponse fetched = commentService.getCommentById(posted.getId());

        assertNotNull(fetched);
        assertEquals("Looking into this now", fetched.getBody());
        assertEquals(existingTicketId, fetched.getTicketId());
        assertEquals(existingUserId, fetched.getUserId());
    }

    @Test
    public void getCommentByIdThrowsWhenNotFound() {
        assertThrows(CommentNotFoundException.class,
                () -> commentService.getCommentById("This comment does not exist"));
    }



    @Test
    public void getCommentsByTicketReturnsCommentsForThatTicket() {
        commentService.postComment(buildCommentRequest());

        List<CommentResponse> comments = commentService.getCommentsByTicket(existingTicketId);

        assertEquals(1, comments.size());
        assertEquals("Looking into this now", comments.get(0).getBody());
    }

    @Test
    public void getCommentsByTicketReturnsEmptyListWhenNoneExist() {
        List<CommentResponse> comments = commentService.getCommentsByTicket(existingTicketId);

        assertTrue(comments.isEmpty());
    }

    @Test
    public void getCommentsByTicketReturnsAllCommentsForThatTicket() {


        CreateCommentRequest firstRequest = buildCommentRequest();
        firstRequest.setBody("Looking into this now");


        CreateCommentRequest secondRequest = buildCommentRequest();
        secondRequest.setBody("I have found the problem");

        commentService.postComment(firstRequest);
        commentService.postComment(secondRequest);

        List<CommentResponse> comments =
                commentService.getCommentsByTicket(existingTicketId);

        assertEquals(2, comments.size());

        assertEquals("Looking into this now", comments.get(0).getBody());
        assertEquals("I have found the problem", comments.get(1).getBody());
    }

    @Test
    public void postCommentGeneratesCommentId() {
        CommentResponse response = commentService.postComment(buildCommentRequest());

        assertNotNull(response.getId());
        assertFalse(response.getId().isBlank());
    }

    @Test
    public void postCommentAssignsCorrectTicket() {
        CommentResponse response = commentService.postComment(buildCommentRequest());

        assertEquals(existingTicketId, response.getTicketId());
    }
}