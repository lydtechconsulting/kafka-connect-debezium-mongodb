package demo.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("items")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    public static final int VARCHAR_MAX_LENGTH = 4096;

    @MongoId
    private String id;

    private String name;
}
