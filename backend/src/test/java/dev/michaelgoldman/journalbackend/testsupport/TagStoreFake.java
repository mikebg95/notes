package dev.michaelgoldman.journalbackend.testsupport;

import dev.michaelgoldman.journalbackend.application.port.out.TagStore;
import dev.michaelgoldman.journalbackend.domain.model.Tag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class TagStoreFake implements TagStore {
    private final List<Tag> tags = new ArrayList<>();

    public void addTags(List<Tag> newTags) {
        tags.addAll(newTags);
    }

    @Override
    public void ensureExist(Set<Tag> tags) {
        throw new UnsupportedOperationException("not needed yet");
    }

    @Override
    public List<Tag> findAll() {
        return tags.stream().sorted(Comparator.comparing(Tag::value)).toList();
    }
}
