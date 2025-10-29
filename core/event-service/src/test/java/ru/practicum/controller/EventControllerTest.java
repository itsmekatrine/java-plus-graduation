package ru.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.client.StatsClient;
import ru.practicum.controller.publicAPI.EventController;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.Location;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.EventState;
import ru.practicum.service.CommentService;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private StatsClient statsClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnEventsList() throws Exception {
        EventShortDto dto = EventShortDto.builder()
                .id(1L)
                .title("Мероприятие")
                .annotation("Описание")
                .category(new CategoryDto(1L, "Категория"))
                .paid(true)
                .eventDate(LocalDateTime.now().plusDays(5))
                .initiator(new UserShortDto(2L, "Вася"))
                .views(10L)
                .confirmedRequests(3L)
                .build();

        when(eventService.searchEvents(any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(dto.getId()))
                .andExpect(jsonPath("$[0].title").value(dto.getTitle()))
                .andExpect(jsonPath("$[0].annotation").value(dto.getAnnotation()))
                .andExpect(jsonPath("$[0].category.id").value(dto.getCategory().getId()))
                .andExpect(jsonPath("$[0].paid").value(dto.getPaid()))
                .andExpect(jsonPath("$[0].eventDate").exists())
                .andExpect(jsonPath("$[0].initiator.name").value(dto.getInitiator().getName()))
                .andExpect(jsonPath("$[0].views").value(dto.getViews()))
                .andExpect(jsonPath("$[0].confirmedRequests").value(dto.getConfirmedRequests()));
    }

    @Test
    void shouldReturnEventFullDtoById() throws Exception {
        EventFullDto dto = EventFullDto.builder()
                .id(1L)
                .title("Мероприятие")
                .annotation("Описание")
                .category(new CategoryDto(1L, "Категория"))
                .paid(true)
                .eventDate(LocalDateTime.now().plusDays(5))
                .initiator(new UserShortDto(2L, "Вася"))
                .views(10L)
                .confirmedRequests(3L)
                .description("Подробное описание")
                .participantLimit(100)
                .state(EventState.PUBLISHED)
                .createdOn(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                .publishedOn(LocalDateTime.now())
                .location(Location.builder().lat(1.0).lon(2.0).build())
                .requestModeration(true)
                .build();

        when(eventService.getEventById(1L)).thenReturn(dto);

        mockMvc.perform(get("/events/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dto.getId()))
                .andExpect(jsonPath("$.title").value(dto.getTitle()))
                .andExpect(jsonPath("$.annotation").value(dto.getAnnotation()))
                .andExpect(jsonPath("$.category.id").value(dto.getCategory().getId()))
                .andExpect(jsonPath("$.paid").value(dto.getPaid()))
                .andExpect(jsonPath("$.eventDate").exists())
                .andExpect(jsonPath("$.initiator.name").value(dto.getInitiator().getName()))
                .andExpect(jsonPath("$.views").value(dto.getViews()))
                .andExpect(jsonPath("$.confirmedRequests").value(dto.getConfirmedRequests()))
                .andExpect(jsonPath("$.description").value(dto.getDescription()))
                .andExpect(jsonPath("$.participantLimit").value(dto.getParticipantLimit()))
                .andExpect(jsonPath("$.state").value(dto.getState().toString()))
                .andExpect(jsonPath("$.createdOn").exists())
                .andExpect(jsonPath("$.publishedOn").exists())
                .andExpect(jsonPath("$.location.lat").value(dto.getLocation().getLat()))
                .andExpect(jsonPath("$.location.lon").value(dto.getLocation().getLon()))
                .andExpect(jsonPath("$.requestModeration").value(dto.getRequestModeration()));
    }
}