package com.example.photogallery.web;

import com.example.photogallery.photo.PhotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Standalone MockMvc (no Spring context) so the controller is exercised without
 * touching the database or AWS — keeps {@code mvn verify} runnable anywhere.
 */
class GalleryControllerTest {

    private PhotoService photoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        photoService = mock(PhotoService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new GalleryController(photoService)).build();
    }

    @Test
    void galleryRendersAndIsHealthCheckable() throws Exception {
        when(photoService.listPhotos()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("gallery"))
                .andExpect(model().attributeExists("photos"));
    }

    @Test
    void deleteRemovesAndRedirectsHome() throws Exception {
        mockMvc.perform(post("/photos/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(photoService).delete(5L);
    }
}
