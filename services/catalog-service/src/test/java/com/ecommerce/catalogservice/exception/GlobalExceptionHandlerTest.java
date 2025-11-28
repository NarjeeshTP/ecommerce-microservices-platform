package com.ecommerce.catalogservice.exception;

import com.ecommerce.catalogservice.config.TestSecurityConfig;
import com.ecommerce.catalogservice.controller.CatalogController;
import com.ecommerce.catalogservice.dto.ItemDTO;
import com.ecommerce.catalogservice.entity.Item;
import com.ecommerce.catalogservice.service.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for GlobalExceptionHandler.
 * Verifies that exceptions are properly caught and formatted.
 */
@WebMvcTest(CatalogController.class)
@Import({GlobalExceptionHandler.class, ModelMapper.class, TestSecurityConfig.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Mockito.reset(itemService);
    }

    @Test
    void shouldReturn400WhenValidationFails() throws Exception {
        // Create invalid ItemDTO (empty name, negative price)
        ItemDTO invalidItem = new ItemDTO();
        invalidItem.setName(""); // Invalid: blank name
        invalidItem.setPrice(-10.0); // Invalid: negative price
        invalidItem.setSku("TEST-001");

        mockMvc.perform(post("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/catalog/items"))
                .andExpect(jsonPath("$.fieldErrors").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void shouldReturn404WhenResourceNotFound() throws Exception {
        // Mock service to throw ResourceNotFoundException
        Mockito.when(itemService.updateItem(eq(999L), any(Item.class)))
                .thenThrow(new ResourceNotFoundException("Item", "id", 999L));

        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setName("Laptop");
        itemDTO.setPrice(1000.0);

        mockMvc.perform(put("/catalog/items/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Item not found with id: '999'"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/catalog/items/999"));
    }

    @Test
    void shouldReturn404WhenItemNotFoundInGetById() throws Exception {
        // Mock service to return empty Optional
        Mockito.when(itemService.getItemById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/catalog/items/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenDuplicateResourceExists() throws Exception {
        // This test demonstrates how DuplicateResourceException would be handled
        // You would throw this from your service when SKU already exists

        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setName("Laptop");
        itemDTO.setPrice(1000.0);
        itemDTO.setSku("LAP-001");

        // Mock service to throw DuplicateResourceException
        Item item = new Item();
        Mockito.when(itemService.createItem(any(Item.class)))
                .thenThrow(new DuplicateResourceException("Item", "sku", "LAP-001"));

        mockMvc.perform(post("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Item already exists with sku: 'LAP-001'"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/catalog/items"));
    }

    @Test
    void shouldReturn400WhenIllegalArgumentException() throws Exception {
        // Mock service to throw IllegalArgumentException
        Mockito.when(itemService.updateItem(eq(1L), any(Item.class)))
                .thenThrow(new IllegalArgumentException("Invalid item data"));

        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setName("Laptop");
        itemDTO.setPrice(1000.0);

        mockMvc.perform(put("/catalog/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid item data"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/catalog/items/1"));
    }

    @Test
    void shouldReturn500WhenUnexpectedExceptionOccurs() throws Exception {
        // Mock service to throw unexpected RuntimeException
        Mockito.when(itemService.getItemById(1L))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/catalog/items/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("A runtime error occurred. Please try again later."))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/catalog/items/1"));
    }

    @Test
    void shouldReturnValidItemWhenDataIsCorrect() throws Exception {
        ItemDTO itemDTO = new ItemDTO();
        itemDTO.setName("Laptop");
        itemDTO.setPrice(1000.0);
        itemDTO.setSku("LAP-001");
        itemDTO.setCategory("Electronics");

        Item savedItem = new Item();
        savedItem.setId(1L);
        savedItem.setName("Laptop");
        savedItem.setPrice(1000.0);
        savedItem.setSku("LAP-001");
        savedItem.setCategory("Electronics");

        Mockito.when(itemService.createItem(any(Item.class))).thenReturn(savedItem);

        mockMvc.perform(post("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(1000.0))
                .andExpect(jsonPath("$.sku").value("LAP-001"));
    }

    @Test
    void shouldHandleMultipleValidationErrors() throws Exception {
        // Create ItemDTO with multiple validation errors
        ItemDTO invalidItem = new ItemDTO();
        // name is null/blank
        invalidItem.setPrice(-100.0); // negative price

        mockMvc.perform(post("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }
}

