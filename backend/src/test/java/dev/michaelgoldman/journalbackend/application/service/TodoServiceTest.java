package dev.michaelgoldman.journalbackend.application.service;

import dev.michaelgoldman.journalbackend.application.port.in.Page;
import dev.michaelgoldman.journalbackend.application.port.in.TodoSort;
import dev.michaelgoldman.journalbackend.application.port.in.TodoWithEntry;
import dev.michaelgoldman.journalbackend.domain.model.Mood;
import dev.michaelgoldman.journalbackend.testsupport.TodoStoreFake;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TodoServiceTest {

    private static final int VALID_PAGE_NUMBER = 0;
    private static final TodoSort VALID_SORT = TodoSort.NEWEST;
    private static final Instant T1 = Instant.parse("2026-08-13T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-08-13T09:10:00Z");
    private static final Instant T3 = Instant.parse("2026-08-13T09:20:00Z");

    private final TodoStoreFake storeFake = new TodoStoreFake();
    private final TodoService todoService = new TodoService(storeFake);

    @Nested
    class FindPage {

        @Test
        void whenArgumentsPassed_shouldReturnContentInCorrectOrder() {
            // Arrange
            TodoWithEntry todo1 = new TodoWithEntry("Buy milk", 1L, "Entry 1", Mood.HAPPY, T1);
            TodoWithEntry todo2 = new TodoWithEntry("Clean house", 1L, "Entry 1", Mood.HAPPY, T1);
            TodoWithEntry todo3 = new TodoWithEntry("Watch movie", 1L, "Entry 1", Mood.HAPPY, T1);
            TodoWithEntry todo4 = new TodoWithEntry("Do homework", 2L, "Entry 2", Mood.NEUTRAL, T2);
            TodoWithEntry todo5 = new TodoWithEntry("Call employer", 3L, "Entry 3", Mood.ANXIOUS, T3);

            storeFake.addTodos(List.of(todo1, todo2, todo3, todo4, todo5));
            Page<TodoWithEntry> todoPage = todoService.findPage(VALID_PAGE_NUMBER, VALID_SORT);

            // Act

            // Assert
        }
    }
}
