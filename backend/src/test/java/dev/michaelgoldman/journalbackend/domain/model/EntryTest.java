package dev.michaelgoldman.journalbackend.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class EntryTest {
    private static final Long VALID_ID = 1L;
    private static final Long VALID_VERSION = 3L;
    private static final String VALID_TITLE = "A valid title";
    private static final String VALID_CONTENT = "This is an example of some valid content.";
    private static final Enrichment VALID_ENRICHMENT = Enrichment.fromModel(
            "A short summary of the entry",
            List.of("work", "health"),
            List.of("Clean the house", "Call the dentist"),
            "HAPPY");

    private static final Instant T1 = Instant.parse("2026-08-13T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-08-13T09:10:00Z");
    private static final Instant T3 = Instant.parse("2026-08-13T09:15:00Z");
    private static final Instant T4 = Instant.parse("2026-08-13T09:20:00Z");

    private static final int TITLE_CHAR_LIMIT = 100;
    private static final int CONTENT_CHAR_LIMIT = 20000;

    @ParameterizedTest(name = "{0}")
    @MethodSource("uncleanValues")
    void whenUncleanTitleOrContentPassed_shouldCleanValue(String name, String clean, String unclean) {
        assertEquals(clean, Entry.of(unclean, VALID_CONTENT, T1).getTitle());
        assertEquals(clean, Entry.of(VALID_TITLE, unclean, T1).getContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void whenBlankTitleOrContentPassed_shouldThrowIllegalArgumentException(String value) {
        assertThrows(IllegalArgumentException.class, () -> Entry.of(value, VALID_CONTENT, T1));
        assertThrows(IllegalArgumentException.class, () -> Entry.of(VALID_TITLE, value, T1));
    }

    @Test
    void whenTitleExceedsCharLimit_shouldThrowException() {
        String atLimit = "a".repeat(TITLE_CHAR_LIMIT);
        String paddedButValid = " ".repeat(120) + "Clean up the house" + " ".repeat(30);
        String tooLong = "a".repeat(TITLE_CHAR_LIMIT + 1);

        assertDoesNotThrow(() -> Entry.of(atLimit, VALID_CONTENT, T1));
        assertDoesNotThrow(() -> Entry.of(paddedButValid, VALID_CONTENT, T1));
        assertThrows(IllegalArgumentException.class, () -> Entry.of(tooLong, VALID_CONTENT, T1));
    }

    @Test
    void whenContentExceedsCharLimit_shouldThrowException() {
        String atLimit = "a".repeat(CONTENT_CHAR_LIMIT);
        String paddedButValid = " ".repeat(30000) + "Clean up the house" + " ".repeat(30);
        String tooLong = "a".repeat(CONTENT_CHAR_LIMIT + 1);

        assertDoesNotThrow(() -> Entry.of(VALID_TITLE, atLimit, T1));
        assertDoesNotThrow(() -> Entry.of(VALID_TITLE, paddedButValid, T1));
        assertThrows(IllegalArgumentException.class, () -> Entry.of(VALID_TITLE, tooLong, T1));
    }

    @Test
    void whenConstructedWithNonCanonicalValue_shouldThrow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Entry(null, null, VALID_TITLE, "  Clean up the house ", Enrichment.empty(), T1, T2, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Entry(null, null, "  Clean up the house ", VALID_CONTENT, Enrichment.empty(), T1, T2, null));
    }

    @Test
    void whenContentHasParagraphs_shouldKeepThem() {
        String prose = "First paragraph.\n\nSecond paragraph.";

        assertEquals(prose, Entry.of(VALID_TITLE, "  " + prose + "  ", T1).getContent());
    }

    @Test
    void whenNewEntryCreated_enrichmentShouldBeEmptyAndAnalysedAtShouldBeNull() {
        Entry entry = Entry.of(VALID_TITLE, VALID_CONTENT, T1);
        Enrichment enrichment = entry.getEnrichment();
        Instant analysedAt = entry.getAnalysedAt();

        assertNull(enrichment.summary());
        assertTrue(enrichment.tags().isEmpty());
        assertTrue(enrichment.todos().isEmpty());
        assertNull(enrichment.mood());
        assertNull(analysedAt);
    }

    @Test
    void whenEnrichmentApplied_shouldReturnEntryWithEnrichmentAndAnalysedAt() {
        Entry minimalEntry = Entry.of(VALID_TITLE, VALID_CONTENT, T1);
        Entry enrichedEntry = minimalEntry.withAnalysis(VALID_ENRICHMENT, T3);

        assertEquals(VALID_TITLE, enrichedEntry.getTitle());
        assertEquals(VALID_CONTENT, enrichedEntry.getContent());
        assertEquals(VALID_ENRICHMENT, enrichedEntry.getEnrichment());
        assertNotNull(enrichedEntry.getAnalysedAt());
        assertEquals(T3, enrichedEntry.getAnalysedAt());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("timestampValues")
    void whenTimestampsAdded_shouldComputeAnalysisStatus(
            String name,
            AnalysisStatus expectedStatus,
            Enrichment enrichment,
            Instant lastUpdated,
            Instant analysedAt) {
        Entry entry = entryWith(enrichment, lastUpdated, analysedAt);
        assertEquals(expectedStatus, entry.getAnalysisStatus());
    }

    @Test
    void whenEntryEdited_shouldMoveLastUpdatedAndKeepAnalysedAt() {
        Entry oldEntry = entryWith(VALID_ENRICHMENT, T2, T3);

        Entry newEntry = oldEntry.withEdit(VALID_VERSION, "Modified title", "Modified content", VALID_ENRICHMENT, T4);

        assertEquals(T4, newEntry.getLastUpdated());
        assertEquals(T3, newEntry.getAnalysedAt());
        assertEquals(AnalysisStatus.OUT_OF_DATE, newEntry.getAnalysisStatus());
    }

    @Test
    void whenEditedWithUncleanValues_shouldCleanThemAndUseNewValues() {
        Entry oldEntry = entryWith(VALID_ENRICHMENT, T2, T3);
        Enrichment newEnrichment = Enrichment.fromModel("New summary", List.of("z"), List.of("9"), "SAD");

        Entry newEntry =
                oldEntry.withEdit(VALID_VERSION, "   Modified title   ", "   Modified content   ", newEnrichment, T4);

        assertEquals("Modified title", newEntry.getTitle());
        assertEquals("Modified content", newEntry.getContent());
        assertEquals(newEnrichment, newEntry.getEnrichment());
    }

    static Stream<Arguments> timestampValues() {
        return Stream.of(
                arguments("Not analysed", AnalysisStatus.NOT_ANALYSED, Enrichment.empty(), T2, null),
                arguments("Analysed", AnalysisStatus.ANALYSED, VALID_ENRICHMENT, T2, T3),
                arguments(
                        "Analysed at the same instant as last update",
                        AnalysisStatus.ANALYSED,
                        VALID_ENRICHMENT,
                        T2,
                        T2),
                arguments("Out of date", AnalysisStatus.OUT_OF_DATE, VALID_ENRICHMENT, T3, T2));
    }

    private Entry entryWith(Enrichment enrichment, Instant lastUpdated, Instant analysedAt) {
        return Entry.fromStorage(
                VALID_ID, VALID_VERSION, VALID_TITLE, VALID_CONTENT, enrichment, T1, lastUpdated, analysedAt);
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    static Stream<Arguments> uncleanValues() {
        return Stream.of(
                arguments("NFC normalisation", "caf\u00E9", "cafe\u0301"),
                arguments(
                        "Preserves case and inner whitespace, trims outer",
                        "Two  Words With   Caps",
                        "   Two  Words With   Caps   "));
    }
}
