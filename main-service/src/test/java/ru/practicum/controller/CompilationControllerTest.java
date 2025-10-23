package ru.practicum.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.controller.publicAPI.CompilationController;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.service.CompilationService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompilationController.class)
class CompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

//    @Test
//    void shouldReturnAllCompilations() throws Exception {
//        List<CompilationDto> compilations = List.of(
//                CompilationDto.builder().id(1L).title("Favorites").build(),
//                CompilationDto.builder().id(2L).title("For Kids").build()
//        );
//
//        when(compilationService.getAllCompilations()).thenReturn(compilations);
//
//        mockMvc.perform(get("/compilations"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(compilations.size()))
//                .andExpect(jsonPath("$[0].id").value(compilations.get(0).getId().intValue()))
//                .andExpect(jsonPath("$[0].title").value(compilations.get(0).getTitle()))
//                .andExpect(jsonPath("$[1].id").value(compilations.get(1).getId().intValue()))
//                .andExpect(jsonPath("$[1].title").value(compilations.get(1).getTitle()));
//
//        verify(compilationService, times(1)).getAllCompilations();
//        verifyNoMoreInteractions(compilationService);
//    }

    @Test
    void shouldReturnCompilationById() throws Exception {
        long compId = 77L;
        CompilationDto dto = CompilationDto.builder().id(compId).title("Holiday Selection").build();

        when(compilationService.getCompilationById(compId)).thenReturn(dto);

        mockMvc.perform(get("/compilations/{compId}", compId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) compId))
                .andExpect(jsonPath("$.title").value("Holiday Selection"));

        verify(compilationService, times(1)).getCompilationById(compId);
        verifyNoMoreInteractions(compilationService);
    }
}
