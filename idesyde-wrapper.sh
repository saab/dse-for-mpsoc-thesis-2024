#! /bin/bash
# MIT License

# Copyright (c) 2024 Saab AB

# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:

# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.

if [[ "$(docker images -q idesyde 2> /dev/null)" == "" ]]; then
    echo "Docker image 'idesyde' not found, building it"
    cd IDeSyDe
    docker build --no-cache -t idesyde .
fi
echo "$@"
docker run --rm -t -v /home/beethoven/Documents/degree-project/dse-for-mpsoc-thesis-2024/models/app/src/main/java/models/artifacts:/models/app/src/main/java/models/artifacts idesyde "$@"