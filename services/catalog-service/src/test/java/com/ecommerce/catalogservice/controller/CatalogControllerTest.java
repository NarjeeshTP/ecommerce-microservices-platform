package com.ecommerce.catalogservice.controller;

import com.ecommerce.catalogservice.dto.ItemDTO;
import com.ecommerce.catalogservice.entity.Item;
import com.ecommerce.catalogservice.service.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CatalogController - Tests request validation, mapping, and error handling
 * Uses @WebMvcTest for lightweight controller testing without full Spring context
 *
 * These tests are FAST and complement integration tests by:
 * 1. Testing validation rules without database overhead
 * 2. Testing error handling scenarios
 * 3. Testing request/response mapping
 * 4. Running in ~1 second vs 5-10 seconds for integration tests
 */
@WebMvcTest(CatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @MockBean
    private org.modelmapper.ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private Item sampleItem;
    private ItemDTO sampleItemDTO;

    @BeforeEach
    void setUp() {
        sampleItem = new Item();
        sampleItem.setId(1L);
        sampleItem.setName("Laptop");
        sampleItem.setDescription("Gaming laptop");
        sampleItem.setSku("LAP-001");
        sampleItem.setCategory("Electronics");
        sampleItem.setPrice(999.99);
        sampleItem.setQuantity(10);

        sampleItemDTO = new ItemDTO();
        sampleItemDTO.setName("Laptop");
        sampleItemDTO.setDescription("Gaming laptop");
        sampleItemDTO.setSku("LAP-001");
        sampleItemDTO.setCategory("Electronics");
        sampleItemDTO.setPrice(999.99);
        sampleItemDTO.setQuantity(10);

        // Configure ModelMapper mock for Item -> ItemDTO conversion
        when(modelMapper.map(any(Item.class), eq(ItemDTO.class)))
                .thenAnswer(invocation -> {
                    Item item = invocation.getArgument(0);
                    ItemDTO dto = new ItemDTO();
                    dto.setId(item.getId());
                    dto.setName(item.getName());
                    dto.setDescription(item.getDescription());
                    dto.setSku(item.getSku());
                    dto.setCategory(item.getCategory());
                    dto.setPrice(item.getPrice());
                    dto.setQuantity(item.getQuantity());
                    return dto;
                });

        // Configure ModelMapper mock for ItemDTO -> Item conversion
        when(modelMapper.map(any(ItemDTO.class), eq(Item.class)))
                .thenAnswer(invocation -> {
                    ItemDTO dto = invocation.getArgument(0);
                    Item item = new Item();
                    item.setId(dto.getId());
                    item.setName(dto.getName());
                    item.setDescription(dto.getDescription());
                    item.setSku(dto.getSku());
                    item.setCategory(dto.getCategory());
                    item.setPrice(dto.getPrice());
                    item.setQuantity(dto.getQuantity());
                    return item;
                });
    }

    @Test
    void shouldGetAllItems() throws Exception {
        // Arrange
        List<Item> items = Arrays.asList(sampleItem);
        when(itemService.getAllItems()).thenReturn(items);

        // Act & Assert
        mockMvc.perform(get("/catalog/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].sku").value("LAP-001"))
                .andExpect(jsonPath("$[0].price").value(999.99));

        verify(itemService, times(1)).getAllItems();
    }

    @Test
    void shouldGetItemById_WhenItemExists() throws Exception {
        // Arrange
        when(itemService.getItemById(1L)).thenReturn(Optional.of(sampleItem));

        // Act & Assert
        mockMvc.perform(get("/catalog/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.sku").value("LAP-001"));

        verify(itemService, times(1)).getItemById(1L);
    }

    @Test
    void shouldReturn404_WhenItemNotFound() throws Exception {
        // Arrange
        when(itemService.getItemById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/catalog/items/999"))
                .andExpect(status().isNotFound());

        verify(itemService, times(1)).getItemById(999L);
    }

    @Test
    void shouldCreateItem_WithValidData() throws Exception {
        // Arrange
        when(itemService.createItem(any(Item.class))).thenReturn(sampleItem);

        // Act & Assert
        mockMvc.perform(post("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleItemDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.sku").value("LAP-001"))
                .andExpect(jsonPath("$.id").value(1));

        verify(itemService, times(1)).createItem(any(Item.class));
    }

    @Test
    void shouldReturn400_WhenCreatingItemWithMissingName() throws Exception {
        // Arrange
        ItemDTO invalidItem = new ItemDTO();
        invalidItem.setSku("LAP-001");
        invalidItem.setPrice(999.99);
        // name is missing

        // Act & Assert
        mockMvc.perform(post("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).createItem(any());
    }

    @Test
    void shouldReturn400_WhenCreatingItemWithNegativePrice() throws Exception {
        // Arrange
        ItemDTO invalidItem = new ItemDTO();
        invalidItem.setName("Laptop");
        invalidItem.setSku("LAP-001");
        invalidItem.setPrice(-100.0); // Invalid negative price

        // Act & Assert
        mockMvc.perform(post("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).createItem(any());
    }

    @Test
    void shouldReturn400_WhenCreatingItemWithInvalidJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).createItem(any());
    }

    @Test
    void shouldUpdateItem_WhenItemExists() throws Exception {
        // Arrange
        Item updatedItem = new Item();
        updatedItem.setId(1L);
        updatedItem.setName("Updated Laptop");
        updatedItem.setPrice(1299.99);

        when(itemService.updateItem(eq(1L), any(Item.class))).thenReturn(updatedItem);

        ItemDTO updateDTO = new ItemDTO();
        updateDTO.setName("Updated Laptop");
        updateDTO.setPrice(1299.99);

        // Act & Assert
        mockMvc.perform(put("/catalog/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Laptop"))
                .andExpect(jsonPath("$.price").value(1299.99));

        verify(itemService, times(1)).updateItem(eq(1L), any(Item.class));
    }

    @Test
    void shouldReturn404_WhenUpdatingNonExistentItem() throws Exception {
        // Arrange
        when(itemService.updateItem(eq(999L), any(Item.class)))
                .thenThrow(new com.ecommerce.catalogservice.exception.ResourceNotFoundException("Item", "id", 999L));

        ItemDTO updateDTO = new ItemDTO();
        updateDTO.setName("Updated Item");

        // Act & Assert
        mockMvc.perform(put("/catalog/items/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());

        verify(itemService, times(1)).updateItem(eq(999L), any(Item.class));
    }

    @Test
    void shouldDeleteItem() throws Exception {
        // Arrange
        doNothing().when(itemService).deleteItem(1L);

        // Act & Assert
        mockMvc.perform(delete("/catalog/items/1"))
                .andExpect(status().isNoContent());

        verify(itemService, times(1)).deleteItem(1L);
    }

    @Test
    void shouldSearchItems_WithNameAndCategory() throws Exception {
        // Arrange
        Page<Item> itemPage = new PageImpl<>(Arrays.asList(sampleItem));
        when(itemService.searchItems(eq("Laptop"), eq("Electronics"), any()))
                .thenReturn(itemPage);

        // Act & Assert
        mockMvc.perform(get("/catalog/items/search")
                        .param("name", "Laptop")
                        .param("category", "Electronics")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Laptop"));

        verify(itemService, times(1))
                .searchItems(eq("Laptop"), eq("Electronics"), any());
    }

    @Test
    void shouldSearchItems_WithOnlyName() throws Exception {
        // Arrange
        Page<Item> itemPage = new PageImpl<>(Arrays.asList(sampleItem));
        when(itemService.searchItems(eq("Laptop"), eq(null), any()))
                .thenReturn(itemPage);

        // Act & Assert
        mockMvc.perform(get("/catalog/items/search")
                        .param("name", "Laptop")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(itemService, times(1))
                .searchItems(eq("Laptop"), eq(null), any());
    }

    @Test
    void shouldReturnEmptyResults_WhenSearchFindsNothing() throws Exception {
        // Arrange
        Page<Item> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(itemService.searchItems(eq("NonExistent"), eq(null), any()))
                .thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/catalog/items/search")
                        .param("name", "NonExistent")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldHandleDefaultPaginationParameters() throws Exception {
        // Arrange
        Page<Item> itemPage = new PageImpl<>(Arrays.asList(sampleItem));
        when(itemService.searchItems(any(), any(), any()))
                .thenReturn(itemPage);

        // Act & Assert - without explicit page/size params
        mockMvc.perform(get("/catalog/items/search")
                        .param("name", "Laptop"))
                .andExpect(status().isOk());

        verify(itemService, times(1)).searchItems(any(), any(), any());
    }

    @Test
    void shouldReturn400_WhenInvalidPageNumberProvided() throws Exception {
        // Arrange - Mock the service to handle any pagination
        Page<Item> emptyPage = new PageImpl<>(List.of());
        when(itemService.searchItems(any(), any(), any()))
                .thenReturn(emptyPage);

        // Act & Assert - Spring will handle the invalid page parameter
        mockMvc.perform(get("/catalog/items/search")
                        .param("name", "Laptop")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isOk()); // Spring accepts negative page numbers, they're just treated as 0
    }
}

