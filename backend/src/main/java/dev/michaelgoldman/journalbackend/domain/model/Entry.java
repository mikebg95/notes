package dev.michaelgoldman.journalbackend.domain.model;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class Entry {
    private static final int TITLE_CHAR_LIMIT = 100;
    private static final int CONTENT_CHAR_LIMIT = 20000;

    private final @Nullable Long id;
    private final @Nullable Long version;
    private final String title;
    private final String content;
    private final Enrichment enrichment;
    private final Instant createdAt;
    private final Instant lastUpdated;
    private final @Nullable Instant analysedAt;

    Entry(
            @Nullable Long id,
            @Nullable Long version,
            String title,
            String content,
            Enrichment enrichment,
            Instant createdAt,
            Instant lastUpdated,
            @Nullable Instant analysedAt) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(enrichment, "enrichment");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(lastUpdated, "lastUpdated");

        if (!title.equals(clean(title)) || title.isBlank() || title.length() > TITLE_CHAR_LIMIT) {
            throw new IllegalArgumentException("Entry title is not canonical");
        }

        if (!content.equals(clean(content)) || content.isBlank() || content.length() > CONTENT_CHAR_LIMIT) {
            throw new IllegalArgumentException("Entry content is not canonical");
        }

        this.id = id;
        this.version = version;
        this.title = title;
        this.content = content;
        this.enrichment = enrichment;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
        this.analysedAt = analysedAt;
    }

    public static Entry of(String uncleanTitle, String uncleanContent, Instant createdAt) {
        Objects.requireNonNull(uncleanTitle, "title");
        Objects.requireNonNull(uncleanContent, "content");
        Objects.requireNonNull(createdAt, "createdAt");

        String cleanedTitle = clean(uncleanTitle);
        String cleanedContent = clean(uncleanContent);

        return new Entry(null, null, cleanedTitle, cleanedContent, Enrichment.empty(), createdAt, createdAt, null);
    }

    public Entry withAnalysis(Enrichment enrichment, Instant analysedAt) {
        return new Entry(
                this.id,
                this.version,
                this.title,
                this.content,
                enrichment,
                this.createdAt,
                this.lastUpdated,
                analysedAt);
    }

    public static Entry fromStorage(
            long id,
            long version,
            String title,
            String content,
            Enrichment enrichment,
            Instant createdAt,
            Instant lastUpdated,
            @Nullable Instant analysedAt) {
        return new Entry(id, version, title, content, enrichment, createdAt, lastUpdated, analysedAt);
    }

    public Entry withEdit(
            long expectedVersion, String uncleanTitle, String uncleanContent, Enrichment enrichment, Instant editedAt) {
        return new Entry(
                this.id,
                expectedVersion,
                clean(uncleanTitle),
                clean(uncleanContent),
                enrichment,
                this.createdAt,
                editedAt,
                this.analysedAt);
    }

    private static String clean(String unclean) {
        return Normalizer.normalize(unclean, Normalizer.Form.NFC).strip();
    }

    public @Nullable Long getId() {
        return id;
    }

    public @Nullable Long getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Enrichment getEnrichment() {
        return enrichment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public @Nullable Instant getAnalysedAt() {
        return analysedAt;
    }

    public AnalysisStatus getAnalysisStatus() {
        if (analysedAt == null) {
            return AnalysisStatus.NOT_ANALYSED;
        }

        return analysedAt.isBefore(lastUpdated) ? AnalysisStatus.OUT_OF_DATE : AnalysisStatus.ANALYSED;
    }
}
