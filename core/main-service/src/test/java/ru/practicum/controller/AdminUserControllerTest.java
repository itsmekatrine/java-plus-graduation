package ru.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.controller.adminAPI.AdminUserController;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(AdminUserController.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AdminUserControllerTest {

    private final ObjectMapper objectMapper;
    private final MockMvc mvc;

    @MockBean
    private final UserService service;

    @Test
    @SneakyThrows
    void create_whenOK() {
        NewUserRequest income = NewUserRequest.builder()
                .email("email@gmail.com")
                .name("name")
                .build();

        UserDto expected = UserDto.builder()
                .id(1L)
                .name("expected")
                .build();

        when(service.create(income)).thenReturn(expected);

        String contentAsString = mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(income)))
                .andExpect(status().is(201))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(service, times(1)).create(income);
        verifyNoMoreInteractions(service);
        assertThat(contentAsString).isEqualTo(objectMapper.writeValueAsString(expected));
    }

    @Test
    @SneakyThrows
    void create_whenMalformedEmail() {
        NewUserRequest income = NewUserRequest.builder()
                .email("123456")
                .name("name")
                .build();
        assertThat(mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(income)))
                .andExpect(status().is(400))
                .andReturn()
                .getResponse()
                .getContentAsString()).contains("email");
    }

    @Test
    @SneakyThrows
    void create_whenMalformedName() {
        NewUserRequest income = NewUserRequest.builder()
                .email("123@gmail.com")
                .name("   ")
                .build();

        assertThat(mvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(income)))
                .andExpect(status().is(400))
                .andReturn()
                .getResponse()
                .getContentAsString()).contains("name").contains("blank");
    }

    @Test
    @SneakyThrows
    void deleteUserById() {
        long userId = 1L;
        mvc.perform(delete("/admin/users/{userId}", userId))
                .andExpect(status().is(204));
    }
}