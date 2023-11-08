package demo.util;

import demo.domain.Item;

public class TestDomainData {

    public static Item buildItem(String id, String name) {
        return Item.builder()
                .id(id)
                .name(name)
                .build();
    }
}
