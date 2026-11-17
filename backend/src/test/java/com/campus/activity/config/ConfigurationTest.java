package com.campus.activity.config;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConfigurationTest {
 @Test void packagedConfigurationIsValidAndHasNoDuplicateKeys()throws Exception{var documents=new org.springframework.boot.env.YamlPropertySourceLoader().load("application",new org.springframework.core.io.ClassPathResource("application.yml"));assertFalse(documents.isEmpty());}
}
