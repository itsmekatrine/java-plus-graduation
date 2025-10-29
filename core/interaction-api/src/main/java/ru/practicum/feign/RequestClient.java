package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", path = "/admin/requests")
public interface RequestClient {

    @GetMapping("/confirmed")
    Map<Long, List<ParticipationRequestDto>> getConfirmedRequests(@RequestParam List<Long> eventIds);

    @GetMapping("/count-by-event")
    Map<Long, Long> countByEvent(@RequestParam("eventIds") List<Long> eventIds,
                                 @RequestParam("status") String status);

    @GetMapping("/exists")
    boolean exists(@RequestParam("userId") Long userId, @RequestParam("eventId") Long eventId, @RequestParam("status") String status);
}
