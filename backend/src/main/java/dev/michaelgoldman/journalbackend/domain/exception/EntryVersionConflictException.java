package dev.michaelgoldman.journalbackend.domain.exception;

public class EntryVersionConflictException extends RuntimeException {

    private final long currentVersion;

    public EntryVersionConflictException(long entryId, long currentVersion, Throwable cause) {
        super("Entry " + entryId + " was modified by another writer", cause);
        this.currentVersion = currentVersion;
    }

    public EntryVersionConflictException(long entryId, long currentVersion) {
        super("Entry " + entryId + " was modified by another writer");
        this.currentVersion = currentVersion;
    }

    public long getCurrentVersion() {
        return currentVersion;
    }
}
