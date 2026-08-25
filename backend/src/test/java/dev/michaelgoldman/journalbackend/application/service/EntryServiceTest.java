package dev.michaelgoldman.journalbackend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.michaelgoldman.journalbackend.application.port.in.CreateEntryCommand;
import dev.michaelgoldman.journalbackend.application.port.in.FindEntriesQuery;
import dev.michaelgoldman.journalbackend.application.port.in.Page;
import dev.michaelgoldman.journalbackend.application.port.in.UpdateEntryCommand;
import dev.michaelgoldman.journalbackend.application.port.out.EntryPageQuery;
import dev.michaelgoldman.journalbackend.domain.exception.EntryNotFoundException;
import dev.michaelgoldman.journalbackend.domain.exception.EntryVersionConflictException;
import dev.michaelgoldman.journalbackend.domain.model.AnalysisStatus;
import dev.michaelgoldman.journalbackend.domain.model.Enrichment;
import dev.michaelgoldman.journalbackend.domain.model.Entry;
import dev.michaelgoldman.journalbackend.domain.model.Mood;
import dev.michaelgoldman.journalbackend.domain.model.Tag;
import dev.michaelgoldman.journalbackend.testsupport.EntryEnricherFake;
import dev.michaelgoldman.journalbackend.testsupport.EntryStoreFake;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EntryServiceTest {

    private static final long VALID_ID = 1L;
    private static final long NON_EXISTING_ID = 99L;
    private static final long VALID_VERSION = 3L;
    private static final String VALID_TITLE = "Valid title";
    private static final String VALID_CONTENT = "Example of some valid content.";
    private static final String VALID_SUMMARY = "Some summary";
    private static final List<String> VALID_TAGS_LIST = List.of("work", "gym");
    private static final List<String> VALID_TODOS = List.of("1", "2");
    private static final Mood VALID_MOOD = Mood.NEUTRAL;
    private static final String VALID_MOOD_STRING = "NEUTRAL";
    private static final Enrichment VALID_ENRICHMENT =
            Enrichment.fromEdit(VALID_SUMMARY, VALID_TAGS_LIST, VALID_TODOS, VALID_MOOD);

    private static final int VALID_PAGE_NUMBER = 0;
    private static final int VALID_PAGE_SIZE = 20;
    private static final String VALID_SEARCH = "Deploy";
    private static final Set<Mood> VALID_MOODS = Set.of(Mood.HAPPY, Mood.NEUTRAL);
    private static final Set<Tag> VALID_TAGS_SET = Set.of(new Tag("work"), new Tag("gym"));

    private static final String NEW_TITLE = "New Title";
    private static final String NEW_CONTENT = "Here is some new content.";
    private static final String NEW_SUMMARY = "The new summary has arrived!";
    private static final Mood NEW_MOOD = Mood.FRUSTRATED;
    private static final List<String> NEW_TAGS = List.of("z", "y", "x");
    private static final List<String> NEW_TODOS = List.of("9", "8");

    private static final Instant T1 = Instant.parse("2026-08-13T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-08-13T09:10:00Z");
    private static final Instant T3 = Instant.parse("2026-08-13T09:20:00Z");

    private final EntryStoreFake storeFake = new EntryStoreFake();
    private final EntryEnricherFake enricherFake = new EntryEnricherFake();

    private final EntryService entryService = new EntryService(storeFake, enricherFake);

    @Nested
    @DisplayName("CreateEntry")
    class CreateEntry {
        @Test
        void shouldReturnAnalysedEntry() {
            // Arrange
            CreateEntryCommand command = new CreateEntryCommand(VALID_TITLE, VALID_CONTENT);
            Enrichment willReturn =
                    Enrichment.fromModel(VALID_SUMMARY, VALID_TAGS_LIST, VALID_TODOS, VALID_MOOD_STRING);
            enricherFake.willReturn(willReturn);

            // Act
            Entry entry = entryService.createEntry(command);
            Enrichment enrichment = entry.getEnrichment();

            // Assert
            assertEquals(VALID_TITLE, entry.getTitle());
            assertEquals(VALID_CONTENT, entry.getContent());
            assertEquals(willReturn, enrichment);
            assertNotNull(entry.getCreatedAt());
            assertNotNull(entry.getAnalysedAt());
        }

        @Test
        void shouldPersistTheEnrichment() {
            // Arrange
            CreateEntryCommand command = new CreateEntryCommand(VALID_TITLE, VALID_CONTENT);
            Enrichment willReturn =
                    Enrichment.fromModel(VALID_SUMMARY, VALID_TAGS_LIST, VALID_TODOS, VALID_MOOD_STRING);
            enricherFake.willReturn(willReturn);

            // Act
            Entry entry = entryService.createEntry(command);
            Entry stored = storedCopyOf(entry);

            // Assert
            assertEquals(willReturn, stored.getEnrichment());
        }

        @Test
        void whenEnrichmentFails_shouldPersistEntryWithoutEnrichment() {
            // Arrange
            CreateEntryCommand command = new CreateEntryCommand(VALID_TITLE, VALID_CONTENT);
            enricherFake.willFail();

            // Act
            Entry entry = entryService.createEntry(command);
            Entry stored = storedCopyOf(entry);

            // Assert
            assertEquals(AnalysisStatus.NOT_ANALYSED, entry.getAnalysisStatus());
            assertNull(stored.getAnalysedAt());
            assertEquals(Enrichment.empty(), stored.getEnrichment());
        }

        @Test
        void shouldEnrichWithCorrectTitleAndContent() {
            // Arrange
            CreateEntryCommand command = new CreateEntryCommand(VALID_TITLE, VALID_CONTENT);

            // Act
            entryService.createEntry(command);

            // Assert
            assertEquals(VALID_TITLE, enricherFake.lastTitle());
            assertEquals(VALID_CONTENT, enricherFake.lastContent());
        }
    }

    @Nested
    @DisplayName("UpdateEntry")
    class UpdateEntry {
        @Test
        void shouldReturnUpdatedEntry() {
            // Arrange
            Entry seeded = seedAnalysedEntry();
            long seededVersion = Objects.requireNonNull(seeded.getVersion());
            UpdateEntryCommand command = updateCommandFor(seeded);
            Enrichment newEnrichment = Enrichment.fromEdit(NEW_SUMMARY, NEW_TAGS, NEW_TODOS, NEW_MOOD);

            // Act
            Entry updated = entryService.updateEntry(command);

            // Assert
            assertEquals(seeded.getId(), updated.getId());
            assertEquals(seededVersion + 1, updated.getVersion());
            assertEquals(NEW_TITLE, updated.getTitle());
            assertEquals(NEW_CONTENT, updated.getContent());
            assertEquals(newEnrichment, updated.getEnrichment());
            assertEquals(seeded.getCreatedAt(), updated.getCreatedAt());
            assertNotEquals(seeded.getLastUpdated(), updated.getLastUpdated());
        }

        @Test
        void shouldSetAnalysisStatusOutOfDate() {
            // Arrange
            UpdateEntryCommand command = updateCommandFor(seedAnalysedEntry());

            // Act
            Entry updated = entryService.updateEntry(command);

            // Assert
            assertEquals(AnalysisStatus.OUT_OF_DATE, updated.getAnalysisStatus());
        }

        @Test
        void whenEntryNotFound_shouldThrowEntryNotFoundException() {
            // Arrange
            UpdateEntryCommand command = new UpdateEntryCommand(
                    VALID_ID,
                    VALID_VERSION,
                    VALID_TITLE,
                    VALID_CONTENT,
                    VALID_SUMMARY,
                    VALID_MOOD,
                    VALID_TAGS_LIST,
                    VALID_TODOS);

            // Act & Assert
            assertThrows(EntryNotFoundException.class, () -> entryService.updateEntry(command));
        }

        @Test
        void whenVersionIsStale_shouldThrowEntryVersionConflictException() {
            // Arrange
            UpdateEntryCommand command = updateCommandFor(seedAnalysedEntry());
            long currentVersion = storeFake.incrementVersion(command.id());

            // Act & Assert
            EntryVersionConflictException thrown =
                    assertThrows(EntryVersionConflictException.class, () -> entryService.updateEntry(command));
            assertEquals(currentVersion, thrown.getCurrentVersion());
        }
    }

    @Nested
    @DisplayName("Analyse")
    class Analyse {

        @Test
        void whenAnalysisSucceeds_shouldAddEnrichmentToEntry() {
            // Arrange
            Entry created = seedNotAnalysedEntry();
            long createdId = Objects.requireNonNull(created.getId());
            enricherFake.willReturn(VALID_ENRICHMENT);

            // Act
            Entry analysed = entryService.analyse(createdId);
            long analysedId = Objects.requireNonNull(analysed.getId());
            Entry fetched = storeFake.findById(analysedId).orElseThrow();

            // Assert
            assertEquals(VALID_TITLE, fetched.getTitle());
            assertEquals(VALID_ENRICHMENT, fetched.getEnrichment());
        }

        @Test
        void whenAnalysisSucceeds_shouldNotChangeLastUpdated() {
            // Arrange
            Entry created = seedNotAnalysedEntry();
            long createdId = Objects.requireNonNull(created.getId());
            enricherFake.willReturn(VALID_ENRICHMENT);

            // Act
            Entry analysed = entryService.analyse(createdId);
            long analysedId = Objects.requireNonNull(analysed.getId());
            Entry fetched = storeFake.findById(analysedId).orElseThrow();

            // Assert
            assertEquals(AnalysisStatus.ANALYSED, fetched.getAnalysisStatus());
            assertEquals(created.getLastUpdated(), fetched.getLastUpdated());
        }

        @Test
        void whenEntryDoesNotExist_shouldThrowEntryNotFoundException() {
            // Act & Assert
            assertThrows(EntryNotFoundException.class, () -> entryService.analyse(NON_EXISTING_ID));
        }

        @Test
        void whenAnalysisFails_shouldKeepExistingEnrichment() {
            // Arrange
            Entry seeded = seedAnalysedEntry();
            long createdId = Objects.requireNonNull(seeded.getId());
            enricherFake.willFail();

            // Act
            entryService.analyse(createdId);
            Entry fetched = storeFake.findById(createdId).orElseThrow();

            // Assert
            assertEquals(seeded.getEnrichment(), fetched.getEnrichment());
            assertEquals(seeded.getAnalysedAt(), fetched.getAnalysedAt());
        }

        @Test
        void whenUserEditsEntryDuringAnalysis_shouldKeepTheEdit() {
            // Arrange
            Entry seeded = seedAnalysedEntry();
            long seededId = Objects.requireNonNull(seeded.getId());
            long seededVersion = Objects.requireNonNull(seeded.getVersion());

            enricherFake.runDuringEnrich(() -> {
                Entry stored = storeFake.findById(seededId).orElseThrow();
                storeFake.update(stored.withEdit(seededVersion, NEW_TITLE, NEW_CONTENT, stored.getEnrichment(), T3));
            });

            // Act & Assert
            Entry result = entryService.analyse(seededId);

            assertEquals(VALID_ENRICHMENT, result.getEnrichment());
            assertEquals(NEW_TITLE, result.getTitle());
            assertEquals(NEW_CONTENT, result.getContent());
        }
    }

    @Nested
    @DisplayName("FindEntryById")
    class FindById {

        @Test
        void whenEntryExists_shouldReturnPersistedEntry() {
            // Arrange
            Entry created = storeFake.create(Entry.of(VALID_TITLE, VALID_CONTENT, T1));

            // Act
            long createdId = Objects.requireNonNull(created.getId());
            Entry found = entryService.findById(createdId);

            // Assert
            assertEquals(createdId, found.getId());
            assertEquals(VALID_TITLE, found.getTitle());
            assertEquals(VALID_CONTENT, found.getContent());
        }

        @Test
        void whenEntryDoesNotExist_shouldThrowEntryNotFoundException() {
            // Act & Assert
            assertThrows(EntryNotFoundException.class, () -> entryService.findById(NON_EXISTING_ID));
        }
    }

    @Nested
    @DisplayName("DeleteEntryById")
    class DeleteById {

        @Test
        void whenEntryExists_shouldRemoveItFromTheStore() {
            // Arrange
            Entry entry = storeFake.create(Entry.of(VALID_TITLE, VALID_CONTENT, T1));
            long id = Objects.requireNonNull(entry.getId());

            // Act
            entryService.deleteById(id);

            // Assert
            assertTrue(storeFake.findById(id).isEmpty());
        }

        @Test
        void whenEntryDoesNotExist_shouldThrowEntryNotFoundException() {
            // Act & Assert
            assertThrows(EntryNotFoundException.class, () -> entryService.deleteById(NON_EXISTING_ID));
        }
    }

    @Nested
    @DisplayName("FindEntriesPage")
    class FindPage {

        @Test
        void whenValidQueryPassed_shouldPassCanonicalQueryToStore() {
            // Arrange
            FindEntriesQuery query = new FindEntriesQuery(
                    VALID_PAGE_NUMBER, VALID_PAGE_SIZE, VALID_SEARCH, VALID_TAGS_LIST, VALID_MOODS);
            EntryPageQuery expected =
                    new EntryPageQuery(VALID_PAGE_NUMBER, VALID_PAGE_SIZE, VALID_SEARCH, VALID_TAGS_SET, VALID_MOODS);

            // Act
            entryService.findPage(query);

            // Assert
            assertEquals(expected, storeFake.lastQuery());
        }

        @Test
        void whenValidQueryPassed_shouldReturnCorrectPage() {
            // Arrange
            List<Entry> seededEntries = seedEntriesForPage();
            FindEntriesQuery query = new FindEntriesQuery(
                    VALID_PAGE_NUMBER, VALID_PAGE_SIZE, VALID_SEARCH, VALID_TAGS_LIST, VALID_MOODS);

            // Act
            Page<Entry> page = entryService.findPage(query);

            // Assert
            assertEquals(VALID_PAGE_NUMBER, page.pageNumber());
            assertEquals(VALID_PAGE_SIZE, page.pageSize());
            assertTrue(page.content().containsAll(seededEntries));
        }

        @Test
        void whenTagStringsAreUnclean_shouldPassCleanedTagsToStore() {
            // Arrange
            List<String> uncleanTags = List.of("   work  ", "\tGYm\t");
            FindEntriesQuery query =
                    new FindEntriesQuery(VALID_PAGE_NUMBER, VALID_PAGE_SIZE, VALID_SEARCH, uncleanTags, VALID_MOODS);

            // Act
            entryService.findPage(query);

            // Assert
            assertEquals(VALID_TAGS_SET, storeFake.lastQuery().tags());
        }

        @Test
        void whenTagStringsAreInvalid_shouldDropThemFromQuery() {
            // Arrange
            List<String> includesInvalidTags = List.of("   ", "work", "", "gym");
            FindEntriesQuery query = new FindEntriesQuery(
                    VALID_PAGE_NUMBER, VALID_PAGE_SIZE, VALID_SEARCH, includesInvalidTags, VALID_MOODS);

            // Act
            entryService.findPage(query);

            // Assert
            assertEquals(VALID_TAGS_SET, storeFake.lastQuery().tags());
        }
    }

    private Entry storedCopyOf(Entry entry) {
        return storeFake.findById(Objects.requireNonNull(entry.getId())).orElseThrow();
    }

    private Entry seedAnalysedEntry() {
        Entry created = storeFake.create(Entry.of(VALID_TITLE, VALID_CONTENT, T1));
        return storeFake.update(created.withAnalysis(VALID_ENRICHMENT, T2));
    }

    private Entry seedNotAnalysedEntry() {
        return storeFake.create(Entry.of(VALID_TITLE, VALID_CONTENT, T1));
    }

    private List<Entry> seedEntriesForPage() {
        Entry created1 = storeFake.create(Entry.of("Entry 1", VALID_CONTENT, T1));
        Entry created2 = storeFake.create(Entry.of("Entry 2", VALID_CONTENT, T1));
        Entry created3 = storeFake.create(Entry.of("Entry 3", VALID_CONTENT, T1));

        return List.of(created1, created2, created3);
    }

    private UpdateEntryCommand updateCommandFor(Entry entry) {
        return new UpdateEntryCommand(
                Objects.requireNonNull(entry.getId()),
                Objects.requireNonNull(entry.getVersion()),
                NEW_TITLE,
                NEW_CONTENT,
                NEW_SUMMARY,
                NEW_MOOD,
                NEW_TAGS,
                NEW_TODOS);
    }
}
