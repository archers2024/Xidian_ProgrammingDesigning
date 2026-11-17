package com.campus.activity.config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import static org.junit.jupiter.api.Assertions.*;
class ConfigurationParsingTest {
 @Test void packagedConfigurationParsesWithoutDuplicateKeys()throws Exception{var sources=new YamlPropertySourceLoader().load("application",new ClassPathResource("application.yml"));assertFalse(sources.isEmpty());}
}
