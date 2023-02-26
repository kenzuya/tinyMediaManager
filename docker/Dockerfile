# Building application container
FROM max06net/base-gui:0.3.0-bullseye-slim

ENV APP=tinyMediaManager
ENV ALLOW_DIRECT_VNC=true

ARG TMM_VERSION=4.3.9
ARG TMM=https://gitlab.com/api/v4/projects/9945251/packages/maven/org/tinymediamanager/tinyMediaManager/${TMM_VERSION}/tinyMediaManager-${TMM_VERSION}-linux.tar.gz

# Install requirements for TMM
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
    wget -q -O tmm.tar.gz ${TMM} && \
    tar xzf tmm.tar.gz -C /tmp/ && \
    mv /tmp/tinyMediaManager/* /app/ && \
    chmod -R 777 /app && \
    rm -R tmm.tar.gz /tmp/tinyMediaManager && \
    mkdir -p /data/data && \
    # Remove some unneeded stuff.
    rm -rf /var/lib/apt/lists/*

ADD launcher-extra.yml /app/
ADD start.sh /app/

LABEL Name="tinyMediaManager" Version=${TMM_VERSION}

WORKDIR /app
CMD ["/app/start.sh"]
