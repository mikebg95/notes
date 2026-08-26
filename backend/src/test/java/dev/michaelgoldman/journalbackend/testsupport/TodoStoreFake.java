package dev.michaelgoldman.journalbackend.testsupport;

import dev.michaelgoldman.journalbackend.application.port.in.Page;
import dev.michaelgoldman.journalbackend.application.port.in.TodoSort;
import dev.michaelgoldman.journalbackend.application.port.in.TodoWithEntry;
import dev.michaelgoldman.journalbackend.application.port.out.TodoStore;
import java.util.Collections;
import org.jspecify.annotations.Nullable;

public class TodoStoreFake implements TodoStore {
    private int pageNumberPassed = -1;
    private int pageSizePassed = -1;
    private @Nullable TodoSort sortPassed = null;

    @Override
    public Page<TodoWithEntry> findPage(int pageNumber, int pageSize, TodoSort sort) {
        this.pageNumberPassed = pageNumber;
        this.pageSizePassed = pageSize;
        this.sortPassed = sort;

        return new Page<>(Collections.emptyList(), pageNumber, pageSize, 0);
    }

    public int getPageNumberPassed() {
        return pageNumberPassed;
    }

    public int getPageSizePassed() {
        return pageSizePassed;
    }

    public @Nullable TodoSort getSortPassed() {
        return sortPassed;
    }
}
