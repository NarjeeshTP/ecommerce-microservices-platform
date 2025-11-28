package com.ecommerce.catalogservice.service;

import com.ecommerce.catalogservice.entity.Item;
import com.ecommerce.catalogservice.exception.ResourceNotFoundException;
import com.ecommerce.catalogservice.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Optional<Item> getItemById(Long id) {
        return itemRepository.findById(id);
    }

    public Item createItem(Item item) {
        return itemRepository.save(item);
    }

    public Item updateItem(Long id, Item itemDetails) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item", "id", id));
        item.setName(itemDetails.getName());
        item.setPrice(itemDetails.getPrice());
        item.setDescription(itemDetails.getDescription());
        item.setQuantity(itemDetails.getQuantity());
        item.setCategory(itemDetails.getCategory());
        item.setUpdatedAt(itemDetails.getUpdatedAt());
        return itemRepository.save(item);
    }

    public void deleteItem(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item", "id", id);
        }
        itemRepository.deleteById(id);
    }

    public Page<Item> searchItems(String name, String category, Pageable pageable) {
        return itemRepository.searchItems(name, category, pageable);
    }
}