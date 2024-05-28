#!/bin/bash

ROOT_PATH=/home/beethoven/Documents/degree-project/dse-for-mpsoc-thesis-2024
CUSTOM_PROJECT_PATH=$ROOT_PATH/models
ARTIFACTS_PATH=$CUSTOM_PROJECT_PATH/app/src/main/java/models/artifacts
DSE_PATH=$ROOT_PATH/IDeSyDe
DSE_EXECUTABLE=idesyde
DSE_OUTPUT_PATH=$DSE_PATH/run

visualize() {
    if [ $# -ne 1 ]; then
	echo "Must provide filepath as argument"
	exit 1
    fi	
    local file="$1"
    gradle run --args="to_kgt $file"
    if [ $? -ne 0 ]; then
        echo "Visualization failed"
        exit 1
    fi
}

if [ $# -lt 2 ]; then
    echo "USAGE: ./run.sh <platform_name> <application_name>";
    exit 1
fi


# create system specification files
cd $CUSTOM_PROJECT_PATH
gradle run --args="build $1 $2"

# check if build was successful
if [ $? -ne 0 ]; then
    echo "Build failed"
    exit 1
fi

now=$(date +%T)
dirname=$1-$2-$now
mkdir $ARTIFACTS_PATH/$dirname
plat=$ARTIFACTS_PATH/$dirname/$1.fiodl
appl=$ARTIFACTS_PATH/$dirname/$2.fiodl
mv $ARTIFACTS_PATH/$1.fiodl $plat
mv $ARTIFACTS_PATH/$2.fiodl $appl


# visualize initial specifications
# visualize $plat
# visualize $appl

# clear the output directory
rm -rf $DSE_OUTPUT_PATH/*

# perform dse with constructed system models
cd $DSE_PATH
rm -rf $DSE_OUTPUT_PATH
./$DSE_EXECUTABLE -v DEBUG -p 5 \
    --x-total-time-out 60 \
    $plat \
    $appl

# quit if there are no reverse identifications
if ! [ "$(ls -A $DSE_OUTPUT_PATH/reversed)" ]; then
    echo "No solution found"
    mv $ARTIFACTS_PATH/$dirname $ARTIFACTS_PATH/$dirname\-\(failed\)
    exit 1
else
    cp -r $DSE_OUTPUT_PATH $ARTIFACTS_PATH/$dirname
fi

# for each fiodl solution in reversed:
# - visualize solution (kgt)
# - parse solution details
cd $CUSTOM_PROJECT_PATH
i=1
for fiodl_file in $DSE_OUTPUT_PATH/reversed/*.fiodl; do
    file=$ARTIFACTS_PATH/$dirname/solution_$i.fiodl
    cp $fiodl_file $file

    # visualize $file

    gradle run --args="parse_solution $file"
    if [ $? -ne 0 ]; then
        echo "Solution parsing failed (solution $i)"
        exit 1
    fi
    i=$((i+1))
done

