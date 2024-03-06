#!/bin/bash


PROJECT_PATH=/home/beethoven/Documents/degree-project/dse-for-mpsoc-thesis-2024/models
ARTIFACTS_PATH=$PROJECT_PATH/app/src/main/java/models/artifacts
DSE_PATH=/home/beethoven/Documents/degree-project/dse-for-mpsoc-thesis-2024/experiments
DSE_EXECUTABLE=idesyde
DSE_OUTPUT_PARSER=parse_dse_results.py
DSE_OUTPUT_PATH=$DSE_PATH/run


# create system specification files (fiodl), specificaiton in code
cd $PROJECT_PATH
gradle run --args="build"

# check if build was successful
if [ $? -ne 0 ]; then
    echo "Build failed"
    exit 1
fi

# clear the output directory
rm -rf $DSE_OUTPUT_PATH/*

# perform dse with constructed system models
cd $DSE_PATH
./$DSE_EXECUTABLE -v DEBUG -p 10 -o $DSE_OUTPUT_PATH \
    --x-improvement-time-out 5 \
    $ARTIFACTS_PATH/MPSoC.fiodl \
    $ARTIFACTS_PATH/ToySDF.fiodl
    # $DSE_PATH/demo/toy_tiled_2core.fiodl \
    # $DSE_PATH/demo/toy_sdf_tiny.fiodl

# quit if there are no reverse identifications
if ! [ "$(ls -A $DSE_OUTPUT_PATH/reversed)" ]; then
    echo "No solution found"
    exit 1
fi

# for each fiodl solution in reversed, create kgt file
cd $PROJECT_PATH
for fiodl_file in $DSE_OUTPUT_PATH/reversed/*.fiodl; do
    gradle run --args="to_kgt $fiodl_file"
done

cd $DSE_PATH
python3 $DSE_OUTPUT_PARSER $DSE_OUTPUT_PATH/explored
