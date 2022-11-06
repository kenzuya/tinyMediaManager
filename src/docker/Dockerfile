# Building application container
FROM max06net/base-gui:0.2.1-buster-slim

LABEL "org.opencontainers.image.version"="${human.version}"
LABEL "org.opencontainers.image.title"="${project.artifactId}"
LABEL "org.opencontainers.image.description"="${project.description}"
LABEL "org.opencontainers.image.url"="${project.url}"
LABEL "org.opencontainers.image.licenses"="Apache License, Version 2.0"
LABEL "org.opencontainers.image.revision"="${buildNumber}"
LABEL "org.opencontainers.image.documentation"="https://hub.docker.com/r/tinymediamanager/tinymediamanager"
LABEL "org.opencontainers.image.source "="${project.scm.url}"

ENV APP=tinyMediaManager
ENV ALLOW_DIRECT_VNC=true

ARG LIBZEN=https://mediaarea.net/download/binary/libzen0/0.4.39/libzen0v5_0.4.39-1_amd64.Debian_10.deb
ARG LIBMEDIAINFO=https://mediaarea.net/download/binary/libmediainfo0/22.09/libmediainfo0v5_22.09-1_amd64.Debian_10.deb

# Install tmm & requirements
ADD tinyMediaManager /app
RUN \
    apt-get update && \
    apt-get install --no-install-recommends -y \
        wget \
        ca-certificates \
        libcurl3-gnutls \
        libmms0 \
        ffmpeg \
        yad \
        fonts-wqy-zenhei && \
    wget -q ${LIBZEN} && \
    wget -q ${LIBMEDIAINFO} && \
    dpkg -i *.deb && \
    mkdir -p /data/data && \
    # Remove some unneeded stuff.
    rm *.deb && \
    rm -rf /var/lib/apt/lists/*

ADD launcher-extra.yml /data/data/
ADD tmm.prop /data/data/

WORKDIR /app
CMD ["/app/tinyMediaManager", "-Dtmm.contentfolder=/data"]
