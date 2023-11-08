package demo.service;

import java.util.Optional;

import demo.domain.Item;
import demo.exception.ItemNotFoundException;
import demo.repository.ItemRepository;
import demo.rest.api.CreateItemRequest;
import demo.rest.api.GetItemResponse;
import demo.rest.api.UpdateItemRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(@Autowired ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public String createItem(CreateItemRequest request) {
        Item item = Item.builder()
                .name(request.getName())
                .build();
        item = itemRepository.save(item);
        log.info("Item created with id: {}", item.getId());
        return item.getId();
    }

    public void updateItem(String itemId, UpdateItemRequest request) {
        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if(itemOpt.isPresent()) {
            log.info("Found item with id: " + itemId);
            Item item = itemOpt.get();
            item.setName(request.getName());
            itemRepository.save(item);
            log.info("Item updated with id: {} - name: {}", itemId, request.getName());
        } else {
            log.error("Item with id: {} not found.", itemId);
            throw new ItemNotFoundException();
        }
    }

    public GetItemResponse getItem(String itemId) {
        Optional<Item> itemOpt = itemRepository.findById(itemId);
        GetItemResponse getItemResponse;
        if(itemOpt.isPresent()) {
            log.info("Found item with id: {}", itemOpt.get().getId());
            getItemResponse = GetItemResponse.builder()
                    .id(itemOpt.get().getId())
                    .name(itemOpt.get().getName())
                    .build();
        } else {
            log.warn("Item with id: {} not found.", itemId);
            throw new ItemNotFoundException();
        }
        return getItemResponse;
    }

    public void deleteItem(String itemId) {
        Optional<Item> itemOpt = itemRepository.findById(itemId);
        if(itemOpt.isPresent()) {
            itemRepository.delete(itemOpt.get());
            log.info("Deleted item with id: {}", itemOpt.get().getId());
        } else {
            log.error("Item with id: {} not found.", itemId);
            throw new ItemNotFoundException();
        }
    }
}
