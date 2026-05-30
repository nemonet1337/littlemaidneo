FROM mcr.microsoft.com/devcontainers/base:ubuntu

# /tmp のパーミッションを修正 (--userns=keep-id の Podman rootless 環境用)
RUN chmod 1777 /tmp

# Adoptium (Temurin) リポジトリを追加して JDK 25 をインストール
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       wget apt-transport-https gpg \
    && wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo $VERSION_CODENAME) main" \
       > /etc/apt/sources.list.d/adoptium.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends temurin-25-jdk \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"
