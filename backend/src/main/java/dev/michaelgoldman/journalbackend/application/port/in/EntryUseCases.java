package dev.michaelgoldman.journalbackend.application.port.in;

import dev.michaelgoldman.journalbackend.domain.exception.EntryNotFoundException;
import dev.michaelgoldman.journalbackend.domain.exception.EntryVersionConflictException;
import dev.michaelgoldman.journalbackend.domain.model.Entry;

public interface EntryUseCases {
    Entry create(CreateEntryCommand command);

    /**
     * @throws EntryNotFoundException if no entry with this id exists
     * @throws EntryVersionConflictException if the entry was modified since the version in the command
     */
    Entry update(UpdateEntryCommand command);

    /**
     * @throws EntryNotFoundException if no entry with this id exists
     */
    Entry analyse(long id);

    /**
     * @throws EntryNotFoundException if no entry with this id exists
     */
    Entry findById(long id);

    /**
     * @throws EntryNotFoundException if no entry with this id exists
     */
    void deleteById(long id);

    Page<Entry> findPage(FindEntriesQuery query);
}
