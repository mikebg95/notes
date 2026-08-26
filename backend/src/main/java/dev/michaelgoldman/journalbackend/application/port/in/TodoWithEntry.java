package dev.michaelgoldman.journalbackend.application.port.in;

import dev.michaelgoldman.journalbackend.domain.model.Mood;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record TodoWithEntry(
        String text,
        long entryId,
        String entryTitle,
        @Nullable Mood entryMood,
        Instant entryLastUpdated) {}
