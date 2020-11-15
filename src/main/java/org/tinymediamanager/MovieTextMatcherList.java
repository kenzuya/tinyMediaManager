package org.tinymediamanager;

import java.util.ResourceBundle;

public enum MovieTextMatcherList {

    TITLE("metatag.title"),
    TITLE_SORTABLE("metatag.title.sortable"),
    ORIGINAL_TITLE("metatag.originaltitle"),
    ORIGINAL_TITLE_SORTABLE("metatag.originaltitle.sortable"),
    SORTED_TITLE("metatag.sorttitle");

    private final String description;

    MovieTextMatcherList(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return ResourceBundle.getBundle("messages").getString(description);
    }
}
