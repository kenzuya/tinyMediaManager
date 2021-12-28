package org.tinymediamanager.core;

import java.nio.file.Path;

public class MediaFilePosition {

    private final Path path;
    private final int position;

    public MediaFilePosition(Path path, int position) {
        this.path = path;
        this.position = position;
    }

    public Path getPath() {
        return path;
    }

    public int getPosition() {
        return position;
    }
}
