#! /bin/bash

if [[ "$(docker images -q idesyde 2> /dev/null)" == "" ]]; then
    echo "Docker image 'idesyde' not found, building it"
    cd IDeSyDe
    docker build --no-cache -t idesyde .
fi
echo "$@"
docker run --rm -t -v /home/beethoven/Documents/degree-project/dse-for-mpsoc-thesis-2024/models/app/src/main/java/models/artifacts:/models/app/src/main/java/models/artifacts idesyde "$@"