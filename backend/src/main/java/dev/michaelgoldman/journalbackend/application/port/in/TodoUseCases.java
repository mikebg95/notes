package dev.michaelgoldman.journalbackend.application.port.in;

public interface TodoUseCases {
    Page<TodoWithEntry> findPage(int pageNumber, TodoSort sort);
}
