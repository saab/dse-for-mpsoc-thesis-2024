#!/bin/bash

export ROOT_DIR=$(pwd)
CUSTOM_PROJECT_PATH=$ROOT_DIR/models
ARTIFACTS_PATH=$CUSTOM_PROJECT_PATH/app/src/main/java/models/artifacts
DSE_PATH=$ROOT_DIR/IDeSyDe
DSE_EXECUTABLE=idesyde


gradle_run() {
    args="$@ --ROOT-DIR=$ROOT_DIR"
    gradle run --args="${args}"
    if [ $? -ne 0 ]; then
        echo "Gradle command '$@' failed"
        exit 1
    fi
}


if [ $# -lt 2 ]; then
    echo "USAGE: ./run.sh <platform_name> <application_name>";
    exit 1
fi

#? create system specification files
cd $CUSTOM_PROJECT_PATH
gradle_run build $1 $2

#? store files in special folder for this run
now=$(date +%T)
dirname=$1-$2-$now
mkdir $ARTIFACTS_PATH/$dirname
plat=$ARTIFACTS_PATH/$dirname/$1.fiodl
appl=$ARTIFACTS_PATH/$dirname/$2.fiodl
mv $ARTIFACTS_PATH/$1.fiodl $plat
mv $ARTIFACTS_PATH/$2.fiodl $appl

#? visualize initial specifications
# gradle_run to_kgt $plat
# gradle_run to_kgt $appl

#? perform dse with constructed system models
cd $DSE_PATH
./$DSE_EXECUTABLE -v DEBUG -p 5 \
    --x-total-time-out 6000 \
    --run-path $ARTIFACTS_PATH/$dirname \
    $plat \
    $appl

#? quit if there are no reverse identifications
if ! [ "$(ls -A $ARTIFACTS_PATH/$dirname/reversed)" ]; then
    echo "No solution found"
    mv $ARTIFACTS_PATH/$dirname $ARTIFACTS_PATH/$dirname\-\(failed\)
    exit 1
fi

#? visualize and parse solution
cd $CUSTOM_PROJECT_PATH
i=1
for fiodl_file in $ARTIFACTS_PATH/$dirname/reversed/*.fiodl; do
    file=$ARTIFACTS_PATH/$dirname/solution_$i.fiodl
    cp $fiodl_file $file

    # gradle_run to_kgt $file
    gradle_run parse_solution $file
    ((i++))
done

