# Building application container
FROM docker.pkg.github.com/max06/base-gui/base-gui:latest

ENV TMM_VERSION=4.1.6
ENV APP=tinyMediaManager

ARG LIBZEN=https://mediaarea.net/download/binary/libzen0/0.4.39/libzen0v5_0.4.39-1_amd64.Debian_10.deb
ARG LIBMEDIAINFO=https://mediaarea.net/download/binary/libmediainfo0/21.03/libmediainfo0v5_21.03-1_amd64.Debian_10.deb
ARG TMM=https://gitlab.com/api/v4/projects/9945251/packages/maven/org/tinymediamanager/tinyMediaManager/${TMM_VERSION}/tinyMediaManager-${TMM_VERSION}-linux.tar.gz

# Install requirements for TMM
RUN \
    apt-get update && \
    apt-get install --no-install-recommends -y \
        wget \
        ca-certificates \
        libcurl3-gnutls \
        libmms0 \
        ffmpeg \
        yad && \
    wget -q ${LIBZEN} && \
    wget -q ${LIBMEDIAINFO} && \
    dpkg -i *.deb && \
    wget -q -O tmm.tar.gz ${TMM} && \
    tar xzf tmm.tar.gz -C /tmp/ && \
    mv /tmp/tinyMediaManager/* /app/ && \
    rm -R tmm.tar.gz /tmp/tinyMediaManager && \
    mkdir -p /data/data && \
    # Remove some unneeded stuff.
    rm *.deb && \
    rm -rf /var/lib/apt/lists/* 

ADD launcher-extra.yml /app/
ADD tmm.prop /data/data/

LABEL Name=tinyMediaManager v=${TMM_VERSION}

WORKDIR /app
CMD ["/app/tinyMediaManager"]
