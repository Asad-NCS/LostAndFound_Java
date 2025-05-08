package com.LostAndFound.LostAndFound.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("storage")
@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class StorageProperties {
    private String location = "uploads"; // Default folder

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}