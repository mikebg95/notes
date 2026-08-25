package dev.michaelgoldman.journalbackend.application.port.in;

import dev.michaelgoldman.journalbackend.domain.model.Mood;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record FindEntriesQuery(
        int pageNumber, int pageSize, @Nullable String search, List<String> tags, Set<Mood> moods) {
    public FindEntriesQuery {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must not be negative");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be at least 1");
        }

        tags = List.copyOf(tags);
        moods = Set.copyOf(moods);
    }
}
