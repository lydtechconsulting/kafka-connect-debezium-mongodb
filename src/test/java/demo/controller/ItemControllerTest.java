package demo.controller;

import java.net.URI;

import demo.exception.ItemNotFoundException;
import demo.rest.api.CreateItemRequest;
import demo.rest.api.GetItemResponse;
import demo.rest.api.UpdateItemRequest;
import demo.service.ItemService;
import demo.util.TestRestData;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItemControllerTest {

    private ItemService serviceMock;
    private ItemController controller;

    @BeforeEach
    public void setUp() {
        serviceMock = mock(ItemService.class);
        controller = new ItemController(serviceMock);
    }

    @Test
    public void testCreateItem_Success() {
        String itemId = randomAlphabetic(8);
        CreateItemRequest request = TestRestData.buildCreateItemRequest(RandomStringUtils.randomAlphabetic(8));
        when(serviceMock.createItem(request)).thenReturn(itemId);
        ResponseEntity response = controller.createItem(request);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(response.getHeaders().getLocation(), equalTo(URI.create(itemId.toString())));
        verify(serviceMock, times(1)).createItem(request);
    }

    @Test
    public void testCreateItem_ServiceThrowsException() {
        CreateItemRequest request = TestRestData.buildCreateItemRequest(RandomStringUtils.randomAlphabetic(8));
        doThrow(new RuntimeException("Service failure")).when(serviceMock).createItem(request);
        ResponseEntity response = controller.createItem(request);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
        verify(serviceMock, times(1)).createItem(request);
    }

    @Test
    public void testUpdateItem_Success() {
        String itemId = randomAlphabetic(8);
        UpdateItemRequest request = TestRestData.buildUpdateItemRequest(RandomStringUtils.randomAlphabetic(8));
        ResponseEntity response = controller.updateItem(itemId, request);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
        verify(serviceMock, times(1)).updateItem(itemId, request);
    }

    @Test
    public void testUpdateItem_NotFound() {
        String itemId = randomAlphabetic(8);
        UpdateItemRequest request = TestRestData.buildUpdateItemRequest(RandomStringUtils.randomAlphabetic(8));
        doThrow(new ItemNotFoundException()).when(serviceMock).updateItem(itemId, request);
        ResponseEntity response = controller.updateItem(itemId, request);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
        verify(serviceMock, times(1)).updateItem(itemId, request);
    }

    @Test
    public void testUpdateItem_ServiceThrowsException() {
        String itemId = randomAlphabetic(8);
        UpdateItemRequest request = TestRestData.buildUpdateItemRequest(RandomStringUtils.randomAlphabetic(8));
        doThrow(new RuntimeException("Service failure")).when(serviceMock).updateItem(itemId, request);
        ResponseEntity response = controller.updateItem(itemId, request);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
        verify(serviceMock, times(1)).updateItem(itemId, request);
    }

    @Test
    public void testGetItem_Success() {
        String itemId = randomAlphabetic(8);
        GetItemResponse getItemResponse = TestRestData.buildGetItemResponse(itemId, "test-item");
        when(serviceMock.getItem(itemId)).thenReturn(getItemResponse);
        ResponseEntity<GetItemResponse> response = controller.getItem(itemId);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody().getId(), equalTo(itemId));
        assertThat(response.getBody().getName(), equalTo("test-item"));
        verify(serviceMock, times(1)).getItem(itemId);
    }

    @Test
    public void testGetItem_NotFound() {
        String itemId = randomAlphabetic(8);
        when(serviceMock.getItem(itemId)).thenThrow(new ItemNotFoundException());
        ResponseEntity<GetItemResponse> response = controller.getItem(itemId);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
        verify(serviceMock, times(1)).getItem(itemId);
    }

    @Test
    public void testDeleteItem_Success() {
        String itemId = randomAlphabetic(8);
        ResponseEntity response = controller.deleteItem(itemId);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
        verify(serviceMock, times(1)).deleteItem(itemId);
    }

    @Test
    public void testDeleteItem_NotFound() {
        String itemId = randomAlphabetic(8);
        doThrow(new ItemNotFoundException()).when(serviceMock).deleteItem(itemId);
        ResponseEntity response = controller.deleteItem(itemId);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
        verify(serviceMock, times(1)).deleteItem(itemId);
    }
}
