package com.ecommerce.catalogservice.controller;

import com.ecommerce.catalogservice.dto.ItemDTO;
import com.ecommerce.catalogservice.entity.Item;
import com.ecommerce.catalogservice.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/catalog")
@Tag(name = "Catalog", description = "Catalog management APIs for items")
public class CatalogController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("/items")
    @Operation(summary = "Get all items", description = "Retrieve a list of all catalog items")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    public List<ItemDTO> getAllItems() {
        return itemService.getAllItems().stream()
                .map(item -> modelMapper.map(item, ItemDTO.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/items/{id}")
    @Operation(summary = "Get item by ID", description = "Retrieve a specific catalog item by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item found"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public ItemDTO getItemById(@Parameter(description = "ID of the item to retrieve") @PathVariable Long id) {
        Item item = itemService.getItemById(id)
                .orElseThrow(() -> new com.ecommerce.catalogservice.exception.ResourceNotFoundException("Item", "id", id));
        return modelMapper.map(item, ItemDTO.class);
    }

    @PostMapping("/items")
    @Operation(summary = "Create new item", description = "Add a new item to the catalog")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ItemDTO createItem(@Valid @RequestBody ItemDTO itemDTO) {
        Item item = modelMapper.map(itemDTO, Item.class);
        item.setCreatedAt(java.time.LocalDateTime.now());
        item.setUpdatedAt(java.time.LocalDateTime.now());
        return modelMapper.map(itemService.createItem(item), ItemDTO.class);
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "Update item", description = "Update an existing catalog item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item updated successfully"),
        @ApiResponse(responseCode = "404", description = "Item not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ItemDTO> updateItem(
            @Parameter(description = "ID of the item to update") @PathVariable Long id,
            @Valid @RequestBody ItemDTO itemDTO) {
        Item item = modelMapper.map(itemDTO, Item.class);
        item.setUpdatedAt(java.time.LocalDateTime.now());
        Item updatedItem = itemService.updateItem(id, item);
        return ResponseEntity.ok(modelMapper.map(updatedItem, ItemDTO.class));
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Delete item", description = "Remove an item from the catalog")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Item deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public ResponseEntity<Void> deleteItem(@Parameter(description = "ID of the item to delete") @PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items/search")
    @Operation(summary = "Search items", description = "Search catalog items by name and/or category with pagination")
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    public Page<ItemDTO> searchItems(
            @Parameter(description = "Item name to search for") @RequestParam(required = false) String name,
            @Parameter(description = "Category to filter by") @RequestParam(required = false) String category,
            Pageable pageable) {
        return itemService.searchItems(name, category, pageable)
                .map(item -> modelMapper.map(item, ItemDTO.class));
    }
}