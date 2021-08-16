package org.tinymediamanager.scraper.tpdb;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.tpdb.service.Controller;

abstract class TpdbMetadataProvider implements IMediaProvider {
    static final String ID = "tpdb";

    private final MediaProviderInfo providerInfo;

    protected Controller controller;

    TpdbMetadataProvider() {
        providerInfo = createMediaProviderInfo();
    }

    protected abstract String getSubId();

    protected MediaProviderInfo createMediaProviderInfo() {
        MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "metadataapi.net",
                "<html><h3>ThePornDB (TPDB)</h3></html>",
                TpdbMetadataProvider.class.getResource("/org/tinymediamanager/scraper/tpdb_logo.png"));

        info.getConfig().addText("apiKey", "", true);
        info.getConfig().load();

        return info;
    }

    @Override
    public boolean isActive() {
        return isFeatureEnabled() && isApiKeyAvailable(providerInfo.getConfig().getValue("apiKey"));
    }

    // thread safe initialization of the API
    protected synchronized void initAPI() throws ScrapeException {

        // create a new instance of the omdb api
        if (controller == null) {
            if (!isActive()) {
                throw new ScrapeException(new FeatureNotEnabledException(this));
            }

            controller = new Controller(false);
        }

        String userApiKey = providerInfo.getConfig().getValue("apiKey");
        if (StringUtils.isNotBlank(userApiKey)) {
            controller.setApiKey(userApiKey);
        } else {
            try {
                controller.setApiKey(getApiKey());
            } catch (Exception e) {
                throw new ScrapeException(e);
            }
        }
    }

    public MediaProviderInfo getProviderInfo() {
        return providerInfo;
    }

    protected abstract Logger getLogger();
}
