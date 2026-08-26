package dev.michaelgoldman.journalbackend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.michaelgoldman.journalbackend.domain.model.Tag;
import dev.michaelgoldman.journalbackend.testsupport.TagStoreFake;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TagServiceTest {
    private static final List<Tag> TAGS = List.of(new Tag("a"), new Tag("b"), new Tag("c"));

    private final TagStoreFake storeFake = new TagStoreFake();
    private final TagService tagService = new TagService(storeFake);

    @Nested
    class FindAll {
        @Test
        void shouldReturnAllTags() {
            // Arrange
            storeFake.addTags(TAGS);

            // Act
            List<Tag> fetchedList = tagService.findAll();

            // Assert
            assertEquals(TAGS, fetchedList);
        }
    }
}
