package com.freightfox.githubsearcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GithubRepositorySearcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubRepositorySearcherApplication.class, args);
    }
}
