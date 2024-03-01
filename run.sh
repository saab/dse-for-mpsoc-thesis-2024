#!/bin/bash


PROJECT_PATH=/home/beethoven/Documents/degree-project/dse-for-mpsoc-thesis-2024/models
ARTIFACTS_PATH=$PROJECT_PATH/app/src/main/java/models/artifacts
DSE_PATH=/home/beethoven/Documents/degree-project/dse-for-mpsoc-thesis-2024/experiments
DSE_EXECUTABLE=idesyde
DSE_OUTPUT_PATH=$DSE_PATH/results

# create system specification files (fiodl), specificaiton in code
cd $PROJECT_PATH
gradle run --args="build"

# clear the output directory
rm -rf $DSE_OUTPUT_PATH/*

# perform dse with constructed system models
cd $DSE_PATH
./$DSE_EXECUTABLE -v DEBUG -p 4 -o $DSE_OUTPUT_PATH \
    --x-improvement-time-out 10 \
    $DSE_PATH/demo/toy_tiled_2core.fiodl \
    $ARTIFACTS_PATH/ToySDF.fiodl
    # $DSE_PATH/demo/toy_sdf_tiny.fiodl
    # $ARTIFACTS_PATH/MPSoC.fiodl \

# inspect if there are any reverse identifications
if [ "$(ls -A $DSE_OUTPUT_PATH/reversed)" ]; then
    echo "Found at least one solution"
else
    echo "No solution found"
fi
