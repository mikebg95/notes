package dev.michaelgoldman.journalbackend;

import org.springframework.boot.SpringApplication;

public class TestJournalBackendApplication {

    static void main(String[] args) {
        SpringApplication.from(JournalBackendApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
