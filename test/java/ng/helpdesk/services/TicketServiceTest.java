package ng.helpdesk.services;

import ng.helpdesk.data.models.Role;
import ng.helpdesk.data.models.User;
import ng.helpdesk.data.repositories.TicketRepository;
import ng.helpdesk.data.repositories.UserRepository;
import ng.helpdesk.dtos.requests.AssignAgentRequest;
import ng.helpdesk.dtos.requests.CreateTicketRequest;
import ng.helpdesk.dtos.requests.UpdateTicketStatusRequest;
import ng.helpdesk.dtos.responses.TicketResponse;
import ng.helpdesk.exceptions.TicketNotFoundException;
import ng.helpdesk.exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private User customer;
    private User agent;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        userRepository.deleteAll();

        customer = new User();
        customer.setName("Customer John");
        customer.setUsername("c_john");
        customer.setEmail("john@example.com");
        customer.setPassword("secret123");
        customer.setRole(Role.CUSTOMER);
        customer = userRepository.save(customer);

        agent = new User();
        agent.setName("Agent Smith");
        agent.setUsername("a_smith");
        agent.setEmail("smith@example.com");
        agent.setPassword("agent123");
        agent.setRole(Role.AGENT);
        agent = userRepository.save(agent);
    }

    @Test
    void testCreateTicketSuccessfully() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setCustomerId(customer.getId());
        request.setTitle("Login Issue");
        request.setDescription("Cannot log into my portal account");
        request.setPriority("HIGH");

        TicketResponse response = ticketService.createTicket(request);

        assertNotNull(response.getId());
        assertEquals("Login Issue", response.getTitle());
        assertEquals("Cannot log into my portal account", response.getDescription());
        assertEquals("HIGH", response.getPriority());
        assertEquals("OPEN", response.getStatus());
        assertEquals(customer.getId(), response.getCustomerId());
    }

    @Test
    void testCreateTicketThrowsWhenCustomerNotFound() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setCustomerId("non_existent_id");
        request.setTitle("Bug Report");
        request.setDescription("UI broken");
        request.setPriority("LOW");

        assertThrows(UserNotFoundException.class, () -> ticketService.createTicket(request));
    }

    @Test
    void testCreateTicketThrowsWhenUserIsNotCustomer() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setCustomerId(agent.getId()); // Agent is not CUSTOMER
        request.setTitle("Network Issue");
        request.setDescription("Office network down");
        request.setPriority("MEDIUM");

        assertThrows(IllegalArgumentException.class, () -> ticketService.createTicket(request));
    }

    @Test
    void testGetAllTicketsAndGetById() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setCustomerId(customer.getId());
        request.setTitle("Billing query");
        request.setDescription("Wrong charge on invoice");
        request.setPriority("MEDIUM");

        TicketResponse created = ticketService.createTicket(request);

        List<TicketResponse> all = ticketService.getAllTickets();
        assertEquals(1, all.size());

        TicketResponse fetched = ticketService.getTicketById(created.getId());
        assertEquals("Billing query", fetched.getTitle());
    }

    @Test
    void testGetTicketByIdThrowsWhenNotFound() {
        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicketById("invalid_id"));
    }

    @Test
    void testGetTicketsByCustomer() {
        CreateTicketRequest req1 = new CreateTicketRequest();
        req1.setCustomerId(customer.getId());
        req1.setTitle("Ticket 1");
        req1.setDescription("Desc 1");
        req1.setPriority("LOW");
        ticketService.createTicket(req1);

        CreateTicketRequest req2 = new CreateTicketRequest();
        req2.setCustomerId(customer.getId());
        req2.setTitle("Ticket 2");
        req2.setDescription("Desc 2");
        req2.setPriority("HIGH");
        ticketService.createTicket(req2);

        List<TicketResponse> customerTickets = ticketService.getTicketsByCustomer(customer.getId());
        assertEquals(2, customerTickets.size());
    }

    @Test
    void testAssignAgentSuccessfully() {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setCustomerId(customer.getId());
        req.setTitle("Hardware Failure");
        req.setDescription("Mouse does not work");
        req.setPriority("LOW");
        TicketResponse created = ticketService.createTicket(req);

        AssignAgentRequest assignRequest = new AssignAgentRequest();
        assignRequest.setAgentId(agent.getId());

        TicketResponse updated = ticketService.assignAgent(created.getId(), assignRequest);
        assertEquals(agent.getId(), updated.getAgentId());
    }

    @Test
    void testAssignAgentThrowsWhenAgentNotFound() {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setCustomerId(customer.getId());
        req.setTitle("Issue");
        req.setDescription("Desc");
        req.setPriority("LOW");
        TicketResponse created = ticketService.createTicket(req);

        AssignAgentRequest assignRequest = new AssignAgentRequest();
        assignRequest.setAgentId("missing_agent_id");

        assertThrows(UserNotFoundException.class, () -> ticketService.assignAgent(created.getId(), assignRequest));
    }

    @Test
    void testAssignAgentThrowsWhenUserIsNotAgent() {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setCustomerId(customer.getId());
        req.setTitle("Issue");
        req.setDescription("Desc");
        req.setPriority("LOW");
        TicketResponse created = ticketService.createTicket(req);

        AssignAgentRequest assignRequest = new AssignAgentRequest();
        assignRequest.setAgentId(customer.getId()); // customer is not AGENT

        assertThrows(IllegalArgumentException.class, () -> ticketService.assignAgent(created.getId(), assignRequest));
    }

    @Test
    void testUpdateStatusSuccessfully() {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setCustomerId(customer.getId());
        req.setTitle("Status Check");
        req.setDescription("Pending investigation");
        req.setPriority("HIGH");
        TicketResponse created = ticketService.createTicket(req);

        UpdateTicketStatusRequest statusRequest = new UpdateTicketStatusRequest();
        statusRequest.setStatus("IN_PROGRESS");

        TicketResponse updated = ticketService.updateStatus(created.getId(), statusRequest);
        assertEquals("IN_PROGRESS", updated.getStatus());
    }

    @Test
    void testDeleteTicketSuccessfully() {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setCustomerId(customer.getId());
        req.setTitle("Delete Me");
        req.setDescription("To be removed");
        req.setPriority("LOW");
        TicketResponse created = ticketService.createTicket(req);

        ticketService.deleteTicket(created.getId());
        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicketById(created.getId()));
    }
}
