#!/bin/bash

ROOT_PATH=/home/beethoven/Documents/degree-project/dse-for-mpsoc-thesis-2024
PROJECT_PATH=$ROOT_PATH/models
ARTIFACTS_PATH=$PROJECT_PATH/app/src/main/java/models/artifacts
DSE_PATH=$ROOT_PATH/experiments
DSE_EXECUTABLE=idesyde
DSE_OUTPUT_PARSER=parse_dse_results.py
DSE_OUTPUT_PATH=$DSE_PATH/run

if [ $# -lt 2 ]; then
    echo "USAGE: ./run.sh <platform_name> <application_name>";
    exit 1
fi


# create system specification files (fiodl), specificaiton in code
cd $PROJECT_PATH
gradle run --args="build $1 $2"

# check if build was successful
if [ $? -ne 0 ]; then
    echo "Build failed"
    exit 1
fi

plat=$1.fiodl
appl=$2.fiodl

gradle run --args="fpga_transform $ARTIFACTS_PATH/$plat $ARTIFACTS_PATH/$appl"

# check if transformation was successful
if [ $? -eq 1 ]; then
    echo "Transformation failed"
    exit 1
else
    plat=$1_Intermediate.fiodl
    appl=$2_Intermediate.fiodl
fi

# clear the output directory
rm -rf $DSE_OUTPUT_PATH/*

# perform dse with constructed system models
cd $DSE_PATH
./$DSE_EXECUTABLE -v DEBUG -p 10 -o $DSE_OUTPUT_PATH \
    --x-total-time-out 5 \
    $ARTIFACTS_PATH/$plat \
    $ARTIFACTS_PATH/$appl

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

# DEPRECATE IN FAVOR OF `gradle run --args="parse_dse_results <path>"`
cd $DSE_PATH
python3 $DSE_OUTPUT_PARSER $DSE_OUTPUT_PATH/explored
