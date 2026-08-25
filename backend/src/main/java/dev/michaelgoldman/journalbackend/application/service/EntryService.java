package dev.michaelgoldman.journalbackend.application.service;

import dev.michaelgoldman.journalbackend.application.port.in.CreateEntryCommand;
import dev.michaelgoldman.journalbackend.application.port.in.EntryUseCases;
import dev.michaelgoldman.journalbackend.application.port.in.FindEntriesQuery;
import dev.michaelgoldman.journalbackend.application.port.in.Page;
import dev.michaelgoldman.journalbackend.application.port.in.UpdateEntryCommand;
import dev.michaelgoldman.journalbackend.application.port.out.EntryEnricher;
import dev.michaelgoldman.journalbackend.application.port.out.EntryPageQuery;
import dev.michaelgoldman.journalbackend.application.port.out.EntryStore;
import dev.michaelgoldman.journalbackend.domain.exception.EnrichmentFailedException;
import dev.michaelgoldman.journalbackend.domain.exception.EntryNotFoundException;
import dev.michaelgoldman.journalbackend.domain.exception.EntryVersionConflictException;
import dev.michaelgoldman.journalbackend.domain.model.Enrichment;
import dev.michaelgoldman.journalbackend.domain.model.Entry;
import dev.michaelgoldman.journalbackend.domain.model.Tag;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class EntryService implements EntryUseCases {
    private final EntryStore entryStore;
    private final EntryEnricher entryEnricher;

    public EntryService(EntryStore entryStore, EntryEnricher entryEnricher) {
        this.entryStore = entryStore;
        this.entryEnricher = entryEnricher;
    }

    @Override
    public Entry createEntry(CreateEntryCommand command) {
        Instant createdAt = Instant.now();
        Entry created = entryStore.create(Entry.of(command.title(), command.content(), createdAt));

        try {
            Enrichment enrichment = entryEnricher.enrich(command.title(), command.content());
            return entryStore.update(created.withAnalysis(enrichment, Instant.now()));
        } catch (EnrichmentFailedException e) {
            return created;
        }
    }

    @Override
    public Entry updateEntry(UpdateEntryCommand command) {
        Entry found = findOrThrow(command.id());

        Entry edit = found.withEdit(
                command.version(),
                command.title(),
                command.content(),
                Enrichment.fromEdit(command.summary(), command.tags(), command.todos(), command.mood()),
                Instant.now());

        return entryStore.update(edit);
    }

    @Override
    public Entry analyse(long id) {
        Entry entry = findOrThrow(id);

        try {
            Enrichment enrichment = entryEnricher.enrich(entry.getTitle(), entry.getContent());
            Instant analysedAt = Instant.now();
            Entry enriched = entry.withAnalysis(enrichment, analysedAt);
            entryStore.update(enriched);

            return enriched;
        } catch (EnrichmentFailedException | EntryVersionConflictException e) {
            return findOrThrow(id);
        }
    }

    @Override
    public Entry findById(long id) {
        return findOrThrow(id);
    }

    @Override
    public void deleteById(long id) {
        boolean deleted = entryStore.deleteById(id);
        if (!deleted) {
            throw new EntryNotFoundException(id);
        }
    }

    @Override
    public Page<Entry> findPage(FindEntriesQuery query) {
        Set<Tag> tags = new HashSet<>();

        for (String tagString : query.tags()) {
            Optional<Tag> optional = Tag.of(tagString);
            optional.ifPresent(tags::add);
        }

        EntryPageQuery entryPageQuery =
                new EntryPageQuery(query.pageNumber(), query.pageSize(), query.search(), tags, query.moods());

        return entryStore.findPage(entryPageQuery);
    }

    private Entry findOrThrow(long id) {
        return entryStore.findById(id).orElseThrow(() -> new EntryNotFoundException(id));
    }
}
