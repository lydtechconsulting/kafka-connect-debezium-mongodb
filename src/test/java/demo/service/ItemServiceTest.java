package demo.service;

import java.util.Optional;

import demo.domain.Item;
import demo.exception.ItemNotFoundException;
import demo.repository.ItemRepository;
import demo.rest.api.CreateItemRequest;
import demo.rest.api.GetItemResponse;
import demo.rest.api.UpdateItemRequest;
import demo.util.TestDomainData;
import demo.util.TestRestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItemServiceTest {

    private ItemRepository itemRepositoryMock;
    private ItemService service;

    @BeforeEach
    public void setUp() {
        itemRepositoryMock = mock(ItemRepository.class);
        service = new ItemService(itemRepositoryMock);
    }

    @Test
    public void testCreateItem() {
        String itemId = randomAlphabetic(8);
        CreateItemRequest request = TestRestData.buildCreateItemRequest(randomAlphabetic(8));
        when(itemRepositoryMock.save(any(Item.class))).thenReturn(TestDomainData.buildItem(itemId, request.getName()));

        String newItemId = service.createItem(request);

        assertThat(itemId, equalTo(newItemId));
        verify(itemRepositoryMock, times(1)).save(any(Item.class));
    }

    @Test
    public void testUpdateItem() {
        String itemId = randomAlphabetic(8);
        UpdateItemRequest request = TestRestData.buildUpdateItemRequest(randomAlphabetic(8));
        when(itemRepositoryMock.findById(itemId)).thenReturn(Optional.of(TestDomainData.buildItem(itemId, request.getName())));

        service.updateItem(itemId, request);

        verify(itemRepositoryMock, times(1)).save(any(Item.class));
    }

    @Test
    public void testUpdateItem_NotFound() {
        String itemId = randomAlphabetic(8);
        UpdateItemRequest request = TestRestData.buildUpdateItemRequest(randomAlphabetic(8));
        when(itemRepositoryMock.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> service.updateItem(itemId, request));
    }

    @Test
    public void testGetItem() {
        String itemId = randomAlphabetic(8);
        when(itemRepositoryMock.findById(itemId)).thenReturn(Optional.of(TestDomainData.buildItem(itemId, "test-item")));

        GetItemResponse item = service.getItem(itemId);

        assertThat(item.getId(), equalTo(itemId));
        assertThat(item.getName(), equalTo("test-item"));
        verify(itemRepositoryMock, times(1)).findById(itemId);
    }

    @Test
    public void testGetItem_NotFound() {
        String itemId = randomAlphabetic(8);
        when(itemRepositoryMock.findById(itemId)).thenReturn(Optional.empty());
        assertThrows(ItemNotFoundException.class, () -> service.getItem(itemId));
    }

    @Test
    public void testDeleteItem() {
        String itemId = randomAlphabetic(8);
        when(itemRepositoryMock.findById(itemId)).thenReturn(Optional.of(TestDomainData.buildItem(itemId, "test-item")));

        service.deleteItem(itemId);

        verify(itemRepositoryMock, times(1)).findById(itemId);
    }

    @Test
    public void testDeleteItem_NotFound() {
        String itemId = randomAlphabetic(8);
        when(itemRepositoryMock.findById(itemId)).thenReturn(Optional.empty());
        assertThrows(ItemNotFoundException.class, () -> service.deleteItem(itemId));
    }
}
