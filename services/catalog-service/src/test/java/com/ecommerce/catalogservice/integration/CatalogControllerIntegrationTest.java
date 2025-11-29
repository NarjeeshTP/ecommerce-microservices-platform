package com.ecommerce.catalogservice.integration;

import com.ecommerce.catalogservice.dto.ItemDTO;
import com.ecommerce.catalogservice.entity.Item;
import com.ecommerce.catalogservice.repository.ItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
    }

    @Test
    void shouldCreateItem() throws Exception {
        ItemDTO newItem = new ItemDTO();
        newItem.setName("Laptop");
        newItem.setDescription("Gaming laptop");
        newItem.setSku("LAP-001");
        newItem.setCategory("Electronics");
        newItem.setPrice(999.99);
        newItem.setQuantity(10);

        mockMvc.perform(post("/catalog/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.sku").value("LAP-001"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldGetItemById() throws Exception {
        // Given: item exists in DB
        Item savedItem = createTestItem("Mouse", "MOUSE-001");

        // When/Then: fetch by ID
        mockMvc.perform(get("/catalog/items/{id}", savedItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.sku").value("MOUSE-001"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentItem() throws Exception {
        mockMvc.perform(get("/catalog/items/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSearchItemsByCategory() throws Exception {
        // Given: multiple items
        createTestItem("Laptop", "LAP-001", "Electronics");
        createTestItem("Mouse", "MOUSE-001", "Electronics");
        createTestItem("Desk", "DESK-001", "Furniture");

        // When/Then: search by category
        mockMvc.perform(get("/catalog/items/search")
                        .param("category", "Electronics")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].category", everyItem(is("Electronics"))));
    }

    @Test
    void shouldUpdateItem() throws Exception {
        // Given: existing item
        Item existingItem = createTestItem("Old Name", "SKU-001");

        // When: update
        ItemDTO updateDTO = new ItemDTO();
        updateDTO.setName("New Name");
        updateDTO.setDescription("Updated description");
        updateDTO.setSku("SKU-001");
        updateDTO.setCategory("Electronics");
        updateDTO.setPrice(199.99);
        updateDTO.setQuantity(5);

        mockMvc.perform(put("/catalog/items/{id}", existingItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void shouldDeleteItem() throws Exception {
        Item item = createTestItem("ToDelete", "DEL-001");

        mockMvc.perform(delete("/catalog/items/{id}", item.getId()))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/catalog/items/{id}", item.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSupportPagination() throws Exception {
        // Given: 15 items
        for (int i = 1; i <= 15; i++) {
            createTestItem("Item " + i, "SKU-" + i);
        }

        // When/Then: first page
        mockMvc.perform(get("/catalog/items/search")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void shouldReturnCorrelationIdInResponseHeader() throws Exception {
        // When: make request without correlation ID
        mockMvc.perform(get("/catalog/items"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void shouldPreserveCustomCorrelationId() throws Exception {
        // Given: custom correlation ID
        String customCorrelationId = "test-correlation-123";

        // When: make request with custom correlation ID
        mockMvc.perform(get("/catalog/items")
                        .header("X-Correlation-ID", customCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", customCorrelationId));
    }

    @Test
    void shouldIncludeCorrelationIdInErrorResponse() throws Exception {
        // Given: custom correlation ID
        String customCorrelationId = "error-test-456";

        // When: request non-existent item
        mockMvc.perform(get("/catalog/items/99999")
                        .header("X-Correlation-ID", customCorrelationId))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-ID", customCorrelationId))
                .andExpect(jsonPath("$.correlationId").value(customCorrelationId));
    }

    // Helper methods
    private Item createTestItem(String name, String sku) {
        return createTestItem(name, sku, "General");
    }

    private Item createTestItem(String name, String sku, String category) {
        Item item = new Item();
        item.setName(name);
        item.setDescription("Test item");
        item.setSku(sku);
        item.setCategory(category);
        item.setPrice(99.99);
        item.setQuantity(10);
        item.setCreatedAt(java.time.LocalDateTime.now());
        item.setUpdatedAt(java.time.LocalDateTime.now());
        return itemRepository.save(item);
    }
}
