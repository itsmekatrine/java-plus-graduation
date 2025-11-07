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
import ru.practicum.controller.adminAPI.AdminCategoryController;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.service.CategoryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(AdminCategoryController.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class AdminCategoryControllerTest {

    private final ObjectMapper objectMapper;
    private final MockMvc mvc;

    @MockBean
    private final CategoryService service;

    @Test
    @SneakyThrows
    void createCategory_whenOk() {
        NewCategoryDto dto = NewCategoryDto.builder()
                .name("cat")
                .build();
        assertThat(dto.getName()).isEqualTo("cat");
        assertThat(objectMapper.writeValueAsString(dto)).contains("cat");
        CategoryDto expected = CategoryDto.builder()
                .name("cat")
                .id(1L)
                .build();

        when(service.save(dto)).thenReturn(expected);

        String contentAsString = mvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().is(201))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(service, times(1)).save(dto);
        verifyNoMoreInteractions(service);
        assertThat(contentAsString).isEqualTo(objectMapper.writeValueAsString(expected));
    }

    @Test
    @SneakyThrows
    void createCategory_whenIncomeDtoIsInvalid() {
        NewCategoryDto dto = NewCategoryDto.builder()
                .name("")
                .build();

        mvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().is(400));
    }

    @Test
    @SneakyThrows
    void deleteCategory_whenOk() {
        Integer catId = 1;
        mvc.perform(delete("/admin/categories/{catId}", catId))
                .andExpect(status().is(204));
    }

    @Test
    @SneakyThrows
    void updateCategory() {
        Long catId = 1L;
        CategoryDto dto = CategoryDto.builder()
                .name("cat")
                .build();

        CategoryDto invoked = CategoryDto.builder()
                .name("cat")
                .id(catId)
                .build();

        CategoryDto expected = CategoryDto.builder()
                .name("cat")
                .id(1L)
                .build();

        when(service.update(invoked)).thenReturn(expected);

        String contentAsString = mvc.perform(patch("/admin/categories/{catId}", catId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(service, times(1)).update(invoked);
        verifyNoMoreInteractions(service);
        assertThat(contentAsString).isEqualTo(objectMapper.writeValueAsString(expected));
    }
}