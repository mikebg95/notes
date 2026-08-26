package dev.michaelgoldman.journalbackend.application.port.in;

import dev.michaelgoldman.journalbackend.domain.model.Tag;
import java.util.List;

public interface TagUseCases {
    List<Tag> findAll();
}
