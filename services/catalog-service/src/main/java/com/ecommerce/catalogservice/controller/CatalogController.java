package com.ecommerce.catalogservice.controller;

import com.ecommerce.catalogservice.dto.ItemDTO;
import com.ecommerce.catalogservice.entity.Item;
import com.ecommerce.catalogservice.service.ItemService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("/items")
    public List<ItemDTO> getAllItems() {
        return itemService.getAllItems().stream()
                .map(item -> modelMapper.map(item, ItemDTO.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<ItemDTO> getItemById(@PathVariable Long id) {
        Optional<Item> item = itemService.getItemById(id);
        return item.map(value -> ResponseEntity.ok(modelMapper.map(value, ItemDTO.class)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/items")
    public ItemDTO createItem(@RequestBody ItemDTO itemDTO) {
        Item item = modelMapper.map(itemDTO, Item.class);
        item.setCreatedAt(java.time.LocalDateTime.now());
        item.setUpdatedAt(java.time.LocalDateTime.now());
        return modelMapper.map(itemService.createItem(item), ItemDTO.class);
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ItemDTO> updateItem(@PathVariable Long id, @RequestBody ItemDTO itemDTO) {
        try {
            Item item = modelMapper.map(itemDTO, Item.class);
            item.setUpdatedAt(java.time.LocalDateTime.now());
            Item updatedItem = itemService.updateItem(id, item);
            return ResponseEntity.ok(modelMapper.map(updatedItem, ItemDTO.class));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}