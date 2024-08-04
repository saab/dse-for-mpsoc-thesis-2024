#!/bin/bash

ROOT_DIR=$(pwd)
CONTAINER_ARTIFACTS_DIR=/models/app/src/main/java/models/artifacts
HOST_ARTIFACTS_DIR=$ROOT_DIR$CONTAINER_ARTIFACTS_DIR

gradle_run() {
    $ROOT_DIR/saab-dse-wrapper.sh "--args=$@"

    if [[ $? -ne 0 ]]; then
        echo "Gradle command 'gradle run $@' failed"
        exit 1
    fi
}

if [[ $# -lt 2 ]]; then
    echo "USAGE: ./run.sh <platform_name> <application_name>";
    exit 1
fi

### store created files in unique folder
dirname=$1-$2-$(date +%T)
host_dirp=$HOST_ARTIFACTS_DIR/$dirname
mkdir -p $host_dirp
container_dirp=$CONTAINER_ARTIFACTS_DIR/$dirname

gradle_run "build $1 $2 $container_dirp"

plat=$1.fiodl
appl=$2.fiodl

### visualize specifications
gradle_run "to_kgt $container_dirp/$plat $container_dirp"
gradle_run "to_kgt $container_dirp/$appl $container_dirp"

### dse on constructed system models
$ROOT_DIR/idesyde-wrapper.sh \
    "$container_dirp/$plat" \
    "$container_dirp/$appl" \
    --run-path $container_dirp \
    -v DEBUG \
    --x-total-time-out 6000

if [[ $? -ne 0 ]]; then
    echo "DSE failed"
    exit 1
fi

### quit if there are no reverse identified solutions
if [[ -z "$(ls -A $host_dirp/reversed)" ]]; then
    echo "No solution found"
    exit 1
fi

### visualize and parse solution
i=1
for fiodl_file in $host_dirp/reversed/*.fiodl; do
    solution_name=solution_$i.fiodl
    echo "Copying $fiodl_file to $host_dirp/$solution_name"
    cp $fiodl_file $host_dirp/$solution_name

    container_solultion_path=$container_dirp/$solution_name
    gradle_run "to_kgt $container_solultion_path $container_dirp"
    gradle_run "parse_solution $container_solultion_path $container_dirp"
    ((i++))
done

