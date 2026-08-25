package dev.michaelgoldman.journalbackend.application.port.out;

import dev.michaelgoldman.journalbackend.domain.model.Mood;
import dev.michaelgoldman.journalbackend.domain.model.Tag;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record EntryPageQuery(
        int pageNumber, int pageSize, @Nullable String search, Set<Tag> tags, Set<Mood> moods) {
    public EntryPageQuery {
        moods = Set.copyOf(moods);
        tags = Set.copyOf(tags);
    }
}
