package dev.michaelgoldman.journalbackend.application.port.out;

import dev.michaelgoldman.journalbackend.application.port.in.Page;
import dev.michaelgoldman.journalbackend.application.port.in.TodoSort;
import dev.michaelgoldman.journalbackend.application.port.in.TodoWithEntry;

public interface TodoStore {
    Page<TodoWithEntry> findPage(int pageNumber, int pageSize, TodoSort sort);
}
