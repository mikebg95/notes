package dev.michaelgoldman.journalbackend.testsupport;

import dev.michaelgoldman.journalbackend.application.port.out.EntryEnricher;
import dev.michaelgoldman.journalbackend.domain.exception.EnrichmentFailedException;
import dev.michaelgoldman.journalbackend.domain.model.Enrichment;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class EntryEnricherFake implements EntryEnricher {
    private boolean shouldFail = false;

    private Enrichment enrichment =
            Enrichment.fromModel("Some summary", List.of("a", "b", "c"), List.of("1", "2"), "NEUTRAL");

    private @Nullable String title;
    private @Nullable String content;

    private Runnable duringEnrich = () -> {};

    public void willRunDuringEnrich(Runnable action) {
        this.duringEnrich = action;
    }

    public void willReturn(Enrichment enrichment) {
        this.enrichment = enrichment;
    }

    public void willFail() {
        this.shouldFail = true;
    }

    @Override
    public Enrichment enrich(String title, String content) {
        this.title = title;
        this.content = content;

        if (shouldFail) {
            throw new EnrichmentFailedException("LLM model enrichment failed");
        }

        duringEnrich.run();

        return enrichment;
    }

    public @Nullable String lastTitle() {
        return title;
    }

    public @Nullable String lastContent() {
        return content;
    }
}
