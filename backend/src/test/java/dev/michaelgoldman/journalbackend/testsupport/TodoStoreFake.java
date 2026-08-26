package dev.michaelgoldman.journalbackend.testsupport;

import dev.michaelgoldman.journalbackend.application.port.in.Page;
import dev.michaelgoldman.journalbackend.application.port.in.TodoSort;
import dev.michaelgoldman.journalbackend.application.port.in.TodoWithEntry;
import dev.michaelgoldman.journalbackend.application.port.out.TodoStore;
import java.util.ArrayList;
import java.util.List;

public class TodoStoreFake implements TodoStore {
    private final List<TodoWithEntry> todos = new ArrayList<>();

    public void addTodos(List<TodoWithEntry> todosToAdd) {
        todos.addAll(todosToAdd);
    }

    @Override
    public Page<TodoWithEntry> findPage(int pageNumber, int pageSize, TodoSort sort) {
        throw new UnsupportedOperationException("not needed yet");
    }
}
