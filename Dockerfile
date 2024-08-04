# syntax=docker/dockerfile:labs
FROM debian:bookworm-slim

ARG OPENJDK_VERSION=17
ARG GRADLE_HOME=/opt/gradle
ARG GRADLE_VERSION=8.1-rc-1
ARG GRADLE_FILE=gradle-${GRADLE_VERSION}-bin.zip

RUN apt-get update && \
    apt-get install -y \
    wget \
    unzip \
    openjdk-${OPENJDK_VERSION}-jdk && \
    apt-get clean

RUN wget https://services.gradle.org/distributions/${GRADLE_FILE} && \
    mkdir -p ${GRADLE_HOME} && \
    unzip -d ${GRADLE_HOME} ${GRADLE_FILE} && \
    rm ${GRADLE_FILE}
ENV PATH="${GRADLE_HOME}/gradle-${GRADLE_VERSION}/bin:${PATH}"


COPY models /models

WORKDIR /models

RUN gradle build

ENV ROOT_DIR=""

ENTRYPOINT [ "gradle", "run" ]
