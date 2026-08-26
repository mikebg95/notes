package dev.michaelgoldman.journalbackend.testsupport;

import dev.michaelgoldman.journalbackend.application.port.out.TagStore;
import dev.michaelgoldman.journalbackend.domain.model.Tag;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TagStoreFake implements TagStore {
    private final Set<Tag> tags = new LinkedHashSet<>();

    public void addTags(List<Tag> newTags) {
        tags.addAll(newTags);
    }

    @Override
    public void ensureExist(Set<Tag> passedTags) {
        tags.addAll(passedTags);
    }

    @Override
    public List<Tag> findAll() {
        return tags.stream().sorted(Comparator.comparing(Tag::value)).toList();
    }

    public Set<Tag> getTags() {
        return tags;
    }
}
