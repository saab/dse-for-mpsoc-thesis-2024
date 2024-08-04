#! /bin/bash

if [[ "$(docker images -q saab-dse 2> /dev/null)" == "" ]]; then
    echo "Docker image 'saab-dse' not found, building it"
    docker build -t saab-dse .
fi

docker run --rm -t -v /home/beethoven/Documents/degree-project/dse-for-mpsoc-thesis-2024/models/app/src/main/java/models/artifacts:/models/app/src/main/java/models/artifacts saab-dse "$@"
