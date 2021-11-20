package org.tinymediamanager.core;

import java.nio.file.Path;

public class MediaFilePosition {

    private final Path path;
    private final long position;

    public MediaFilePosition(Path path, long position) {
        this.path = path;
        this.position = position;
    }

    public Path getPath() {
        return path;
    }

    public long getPosition() {
        return position;
    }
}
