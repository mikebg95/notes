package dev.michaelgoldman.journalbackend.application.service;

import dev.michaelgoldman.journalbackend.application.port.in.TagUseCases;
import dev.michaelgoldman.journalbackend.application.port.out.TagStore;
import dev.michaelgoldman.journalbackend.domain.model.Tag;
import java.util.List;

public class TagService implements TagUseCases {

    private final TagStore tagStore;

    public TagService(TagStore tagstore) {
        this.tagStore = tagstore;
    }

    @Override
    public List<Tag> findAll() {
        return tagStore.findAll();
    }
}
