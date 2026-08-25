package dev.michaelgoldman.journalbackend.testsupport;

import dev.michaelgoldman.journalbackend.application.port.in.Page;
import dev.michaelgoldman.journalbackend.application.port.out.EntryPageQuery;
import dev.michaelgoldman.journalbackend.application.port.out.EntryStore;
import dev.michaelgoldman.journalbackend.domain.exception.EntryVersionConflictException;
import dev.michaelgoldman.journalbackend.domain.model.Entry;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public class EntryStoreFake implements EntryStore {
    private final Map<Long, Entry> entries = new HashMap<>();
    private long nextId = 0L;
    private static final long INITIAL_VERSION = 1L;
    private @Nullable EntryPageQuery lastQuery;

    public long incrementVersion(long entryId) {
        Entry entry = Objects.requireNonNull(entries.get(entryId));
        Objects.requireNonNull(entry.getVersion());
        long currentVersion = entry.getVersion();
        Entry incremented = Entry.fromStorage(
                entryId,
                currentVersion + 1,
                entry.getTitle(),
                entry.getContent(),
                entry.getEnrichment(),
                entry.getCreatedAt(),
                entry.getLastUpdated(),
                entry.getAnalysedAt());
        entries.replace(entryId, incremented);

        return currentVersion + 1;
    }

    @Override
    public Entry create(Entry entry) {
        long id = ++nextId;

        Entry created = copyWithIdentity(id, INITIAL_VERSION, entry);
        entries.put(id, created);

        return created;
    }

    @Override
    public Entry update(Entry entry) {
        long id = Objects.requireNonNull(entry.getId(), "cannot update an entry with no id");
        long version = Objects.requireNonNull(entry.getVersion(), "cannot update an entry with no version");

        if (!entries.containsKey(id)) {
            throw new IllegalStateException("no entry with id " + id);
        }

        Entry existing = Objects.requireNonNull(entries.get(id));
        long existingVersion = Objects.requireNonNull(existing.getVersion());

        if (version != existingVersion) {
            throw new EntryVersionConflictException(id, existingVersion);
        }

        Entry updated = copyWithIdentity(id, version + 1, entry);
        entries.put(id, updated);

        return updated;
    }

    @Override
    public Optional<Entry> findById(long id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public boolean deleteById(long id) {
        Entry deleted = entries.remove(id);
        return deleted != null;
    }

    @Override
    public Page<Entry> findPage(EntryPageQuery query) {
        lastQuery = query;
        List<Entry> all = List.copyOf(entries.values());

        Comparator<Entry> newestFirst = Comparator.comparing(Entry::getCreatedAt)
                .thenComparing(e -> Objects.requireNonNull(e.getId()))
                .reversed();

        List<Entry> content = all.stream()
                .sorted(newestFirst)
                .skip((long) query.pageNumber() * query.pageSize())
                .limit(query.pageSize())
                .toList();

        return new Page<>(content, query.pageNumber(), query.pageSize(), all.size());
    }

    private Entry copyWithIdentity(long id, long version, Entry entry) {
        return Entry.fromStorage(
                id,
                version,
                entry.getTitle(),
                entry.getContent(),
                entry.getEnrichment(),
                entry.getCreatedAt(),
                entry.getLastUpdated(),
                entry.getAnalysedAt());
    }

    public EntryPageQuery lastQuery() {
        return Objects.requireNonNull(lastQuery, "findPage was never called");
    }
}
