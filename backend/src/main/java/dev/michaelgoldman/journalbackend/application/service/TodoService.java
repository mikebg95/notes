package dev.michaelgoldman.journalbackend.application.service;

import dev.michaelgoldman.journalbackend.application.port.in.Page;
import dev.michaelgoldman.journalbackend.application.port.in.TodoSort;
import dev.michaelgoldman.journalbackend.application.port.in.TodoUseCases;
import dev.michaelgoldman.journalbackend.application.port.in.TodoWithEntry;
import dev.michaelgoldman.journalbackend.application.port.out.TodoStore;

public class TodoService implements TodoUseCases {
    private static final int PAGE_SIZE = 20;

    private final TodoStore todoStore;

    public TodoService(TodoStore todoStore) {
        this.todoStore = todoStore;
    }

    @Override
    public Page<TodoWithEntry> findPage(int pageNumber, TodoSort sort) {
        return todoStore.findPage(pageNumber, PAGE_SIZE, sort);
    }
}
