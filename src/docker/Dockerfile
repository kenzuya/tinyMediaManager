# Building application container
FROM max06net/base-gui:edge-bullseye-slim

LABEL "org.opencontainers.image.version"="${human.version}"
LABEL "org.opencontainers.image.title"="${project.artifactId}"
LABEL "org.opencontainers.image.description"="${project.description}"
LABEL "org.opencontainers.image.url"="${project.url}"
LABEL "org.opencontainers.image.licenses"="Apache License, Version 2.0"
LABEL "org.opencontainers.image.revision"="${buildNumber}"
LABEL "org.opencontainers.image.documentation"="https://www.tinymediamanager.org/docs/docker"
LABEL "org.opencontainers.image.source "="${project.scm.url}"

ENV APP=tinyMediaManager
ENV ALLOW_DIRECT_VNC=true
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8
ENV LC_TIME=C.UTF-8

# Install tmm & requirements
ADD tinyMediaManager /app
RUN \
    apt-get update && \
    apt-get install --no-install-recommends -y \
        wget \
        ca-certificates \
        libcurl3-gnutls \
        libmms0 \
        yad \
        fonts-wqy-zenhei \
        libxtst6 && \
    mkdir -p /data/data && \
    # Remove some unneeded stuff.
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
CMD ["/app/tinyMediaManager", "-Dtmm.contentfolder=/data", "-noupdate=true"]
