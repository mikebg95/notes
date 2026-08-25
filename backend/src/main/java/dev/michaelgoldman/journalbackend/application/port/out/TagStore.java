package dev.michaelgoldman.journalbackend.application.port.out;

import dev.michaelgoldman.journalbackend.domain.model.Tag;
import java.util.List;
import java.util.Set;

public interface TagStore {
    /**
     * Stores the tags that do not exist yet, leaving the existing ones untouched. Two entries analysed at the same
     * moment can both find a tag missing and both insert it, so the implementation recovers from the resulting
     * duplicate by reading the row that won. That failed insert poisons the surrounding transaction, so this must run
     * before the transaction that writes the entry.
     *
     * @param tags canonical tags, as carried by an entry's enrichment
     */
    void ensureExist(Set<Tag> tags);

    /**
     * @return every tag, sorted alphabetically by value
     */
    List<Tag> findAll();
}
