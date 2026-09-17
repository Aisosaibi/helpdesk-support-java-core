package ng.helpdesk.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

// These values are calculated from the tickets collection and displayed by the landing page.
@Data
@AllArgsConstructor
public class TicketStatsResponse {
    private long open;
    private long inProgress;
    private long resolved;
}