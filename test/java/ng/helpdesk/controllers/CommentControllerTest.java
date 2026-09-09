package ng.helpdesk.controllers;

import ng.helpdesk.data.models.Role;
import ng.helpdesk.data.models.Ticket;
import ng.helpdesk.data.models.User;
import ng.helpdesk.data.repositories.CommentRepository;
import ng.helpdesk.data.repositories.TicketRepository;
import ng.helpdesk.data.repositories.UserRepository;
import ng.helpdesk.dtos.requests.CreateCommentRequest;
import ng.helpdesk.dtos.responses.CommentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private String existingUserId;
    private String existingTicketId;

    @BeforeEach
    void cleanUp() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();

        existingUserId = buildAndSaveUser();
        existingTicketId = buildAndSaveTicket(existingUserId);
    }

    private String buildAndSaveUser() {
        User user = new User();
        user.setName("Godwim David");
        user.setUsername("dave");
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
        request.setAuthorId(existingUserId);
        return request;
    }

    // --- POST /api/comments ---

    @Test
    void postCommentReturns201AndSavedComment() throws Exception {
        CreateCommentRequest request = buildCommentRequest();

        mockMvc.perform(post("/api/comments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body", is("Looking into this now")))
                .andExpect(jsonPath("$.ticketId", is(existingTicketId)))
                .andExpect(jsonPath("$.authorId", is(existingUserId)));
    }

    @Test
    void postCommentReturns404WhenTicketDoesNotExist() throws Exception {
        CreateCommentRequest request = buildCommentRequest();
        request.setTicketId("nonexistent ticketId");

        mockMvc.perform(post("/api/comments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void postCommentReturns404WhenAuthorDoesNotExist() throws Exception {
        CreateCommentRequest request = buildCommentRequest();
        request.setAuthorId("nonexistent autherId");

        mockMvc.perform(post("/api/comments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/comments/{id} ---

    @Test
    void getCommentByIdReturns200AndComment() throws Exception {
        CommentResponse posted = mockMvc.perform(post("/api/comments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildCommentRequest())))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .transform(json -> objectMapper.readValue(json, CommentResponse.class));

        mockMvc.perform(get("/api/comments/" + posted.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body", is("Looking into this now")))
                .andExpect(jsonPath("$.ticketId", is(existingTicketId)))
                .andExpect(jsonPath("$.authorId", is(existingUserId)));
    }

    @Test
    void getCommentByIdReturns404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/comments/nonexistent-comment-id"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/comments/ticket/{ticketId} ---

    @Test
    void getCommentsByTicketReturnsSavedComments() throws Exception {
        CreateCommentRequest request = buildCommentRequest();
        mockMvc.perform(post("/api/comments")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/comments/ticket/" + existingTicketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].body", is("Looking into this now")));
    }

    @Test
    void getCommentsByTicketReturnsEmptyListWhenNoneExist() throws Exception {
        mockMvc.perform(get("/api/comments/ticket/" + existingTicketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }
}