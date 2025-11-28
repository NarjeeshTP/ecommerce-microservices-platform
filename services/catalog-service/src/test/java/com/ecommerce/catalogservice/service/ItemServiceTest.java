package com.ecommerce.catalogservice.service;

import com.ecommerce.catalogservice.entity.Item;
import com.ecommerce.catalogservice.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ItemService - Fast, isolated tests of business logic
 * No database, no Spring context - just pure business logic testing
 *
 * These tests complement integration tests by:
 * 1. Running fast (milliseconds vs seconds)
 * 2. Testing edge cases that are hard to reproduce with real DB
 * 3. Providing precise failure identification
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item sampleItem;

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
    }

    @Test
    void shouldGetAllItems() {
        // Arrange
        List<Item> items = Arrays.asList(sampleItem);
        when(itemRepository.findAll()).thenReturn(items);

        // Act
        List<Item> result = itemService.getAllItems();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    void shouldGetItemById_WhenItemExists() {
        // Arrange
        when(itemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));

        // Act
        Optional<Item> result = itemService.getItemById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Laptop");
        assertThat(result.get().getSku()).isEqualTo("LAP-001");
        verify(itemRepository, times(1)).findById(1L);
    }

    @Test
    void shouldReturnEmpty_WhenItemDoesNotExist() {
        // Arrange
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Item> result = itemService.getItemById(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(itemRepository, times(1)).findById(999L);
    }

    @Test
    void shouldCreateItem() {
        // Arrange
        Item newItem = new Item();
        newItem.setName("Mouse");
        newItem.setPrice(29.99);

        Item savedItem = new Item();
        savedItem.setId(2L);
        savedItem.setName("Mouse");
        savedItem.setPrice(29.99);

        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);

        // Act
        Item result = itemService.createItem(newItem);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getName()).isEqualTo("Mouse");
        verify(itemRepository, times(1)).save(newItem);
    }

    @Test
    void shouldUpdateItem_WhenItemExists() {
        // Arrange
        Item updatedDetails = new Item();
        updatedDetails.setName("Updated Laptop");
        updatedDetails.setPrice(1299.99);
        updatedDetails.setDescription("Updated description");
        updatedDetails.setQuantity(5);
        updatedDetails.setCategory("Premium Electronics");

        when(itemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(itemRepository.save(any(Item.class))).thenReturn(sampleItem);

        // Act
        Item result = itemService.updateItem(1L, updatedDetails);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Laptop");
        assertThat(result.getPrice()).isEqualTo(1299.99);
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(itemRepository, times(1)).findById(1L);
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    void shouldThrowException_WhenUpdatingNonExistentItem() {
        // Arrange
        Item updatedDetails = new Item();
        updatedDetails.setName("Updated Item");

        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> itemService.updateItem(999L, updatedDetails))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Item not found");

        verify(itemRepository, times(1)).findById(999L);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void shouldDeleteItem() {
        // Arrange
        when(itemRepository.existsById(1L)).thenReturn(true);
        doNothing().when(itemRepository).deleteById(1L);

        // Act
        itemService.deleteItem(1L);

        // Assert
        verify(itemRepository, times(1)).existsById(1L);
        verify(itemRepository, times(1)).deleteById(1L);
    }

    @Test
    void shouldSearchItems_WithNameAndCategory() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Item> items = Arrays.asList(sampleItem);
        Page<Item> itemPage = new PageImpl<>(items, pageable, items.size());

        when(itemRepository.searchItems("Laptop", "Electronics", pageable))
            .thenReturn(itemPage);

        // Act
        Page<Item> result = itemService.searchItems("Laptop", "Electronics", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laptop");
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(itemRepository, times(1)).searchItems("Laptop", "Electronics", pageable);
    }

    @Test
    void shouldSearchItems_WithOnlyName() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Item> items = Arrays.asList(sampleItem);
        Page<Item> itemPage = new PageImpl<>(items, pageable, items.size());

        when(itemRepository.searchItems("Laptop", null, pageable))
            .thenReturn(itemPage);

        // Act
        Page<Item> result = itemService.searchItems("Laptop", null, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(itemRepository, times(1)).searchItems("Laptop", null, pageable);
    }

    @Test
    void shouldReturnEmptyPage_WhenNoItemsMatchSearch() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(itemRepository.searchItems("NonExistent", "Unknown", pageable))
            .thenReturn(emptyPage);

        // Act
        Page<Item> result = itemService.searchItems("NonExistent", "Unknown", pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void shouldHandleNullItemDetails_InUpdate() {
        // Arrange
        when(itemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));

        Item nullDetails = new Item();
        // All fields null except what's needed

        when(itemRepository.save(any(Item.class))).thenReturn(sampleItem);

        // Act
        Item result = itemService.updateItem(1L, nullDetails);

        // Assert
        assertThat(result).isNotNull();
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    void shouldPreserveOriginalSku_WhenUpdating() {
        // Arrange
        Item updatedDetails = new Item();
        updatedDetails.setName("Updated Name");
        updatedDetails.setPrice(1500.00);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));
        when(itemRepository.save(any(Item.class))).thenReturn(sampleItem);

        // Act
        itemService.updateItem(1L, updatedDetails);

        // Assert - SKU should remain unchanged
        assertThat(sampleItem.getSku()).isEqualTo("LAP-001");
    }
}

