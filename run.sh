#!/bin/bash


PROJECT_PATH=/home/beethoven/Documents/degree-project/dse-for-mpsoc-thesis-2024/models
ARTIFACTS_PATH=$PROJECT_PATH/app/src/main/java/models/artifacts
DSE_PATH=/home/beethoven/Documents/degree-project/dse-for-mpsoc-thesis-2024/experiments
DSE_EXECUTABLE=idesyde
DSE_OUTPUT_PATH=$DSE_PATH/results

# create the input fiodl files based on current configuration
cd $PROJECT_PATH

gradle run --args="build"

# clear the output directory
rm -rf $DSE_OUTPUT_PATH/*

# run the DSE tool with the input models from 
$DSE_PATH/$DSE_EXECUTABLE -v DEBUG -p 4 -o $DSE_OUTPUT_PATH --x-improvement-time-out 10 \
    $ARTIFACTS_PATH/MPSoC.fiodl $ARTIFACTS_PATH/ToySDF.fiodl

echo $?

# inspect if there are any reverse identifications
if [ "$(ls -A $DSE_OUTPUT_PATH/reversed)" ]; then
    echo "Found at least one solution"
else
    echo "No solution found"
fi
