package com.michaelpippolito.fantasy;

import com.michaelpippolito.fantasy.mlb.repository.MlbGameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.michaelpippolito.fantasy.mlb.repository")
@EnableRetry
public class FantasyApplication {
    @Autowired
    private MlbGameRepository mlbGameRepository;

    public static void main(String[] args) {
        SpringApplication.run(FantasyApplication.class, args);
    }
}
