package dev.michaelgoldman.journalbackend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.michaelgoldman.journalbackend.application.port.in.TodoSort;
import dev.michaelgoldman.journalbackend.testsupport.TodoStoreFake;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TodoServiceTest {
    private final TodoStoreFake storeFake = new TodoStoreFake();
    private final TodoService todoService = new TodoService(storeFake);

    @Nested
    class FindPage {

        @Test
        void shouldDelegateToStoreWithFixedPageSize() {
            // Arrange
            int pageNumber = 2;
            TodoSort sort = TodoSort.OLDEST;
            int pageSize = 20;

            // Act
            todoService.findPage(pageNumber, sort);

            // Assert
            assertEquals(pageNumber, storeFake.getPageNumberPassed());
            assertEquals(pageSize, storeFake.getPageSizePassed());
            assertEquals(sort, storeFake.getSortPassed());
        }
    }
}
