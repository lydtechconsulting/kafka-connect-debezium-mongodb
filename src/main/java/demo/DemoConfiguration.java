package demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Slf4j
@EnableMongoRepositories("demo.repository")
@ComponentScan(basePackages = {"demo"})
@Configuration
public class DemoConfiguration {
}
