package ru.practicum.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusUpdateRequest {
    private Long eventId;
    private List<Long> requestIds;
    private RequestStatus status;
}
