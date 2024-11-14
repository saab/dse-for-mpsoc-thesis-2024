
# Design Space Exploration Master's Thesis
### Automated Design Space Exploration for Hardware and Software Implementations on Heterogeneous MPSoCs
The [thesis](http://www.diva-portal.org/smash/record.jsf?dswid=5562&pid=diva2%3A1912301&c=1&searchType=SIMPLE&language=sv&query=Automated+design+space+exploration+of+heterogeneous&af=%5B%5D&aq=%5B%5B%5D%5D&aq2=%5B%5B%5D%5D&aqe=%5B%5D&noOfRows=50&sortOrder=author_sort_asc&sortOrder2=title_sort_asc&onlyFullText=true&sf=undergraduate) tackled the problem of providing design space exploration support for platforms with both hardware- and software-programmable processing units. It is focusing on exploring how application execution can best be distributed to an FPGA and CPUs on the [Zynq UltraScale+ MPSoC ZCU102 Evaluation Kit](https://www.xilinx.com/products/boards-and-kits/ek-u1-zcu102-g.html#inside) to achieve best throughput and use the least amount of processing units.

## Incorporated Tools
The thesis was done in collaboration with the ForSyDe research group at KTH Kista and uses [ForSyDe IO](https://github.com/forsyde/forsyde-io) to create the system models (applications, platform) and [IDeSyDe](https://github.com/forsyde/IDeSyDe/) to perform automated design space exploration.

## Repository Structure
The repository is structured as follows and uses a Gradle app to handle interaction with the ForSyDe IO library used to create and manage system models:
- `run.sh`: Bash script that interprets command line arguments, calls the Gradle app to create system models, provides the generated system models as input to IDeSyDe and lastly calls the Gradle app to parse the design solutions produced by IDeSyDe.
- `models/`: The Gradle app directory

The *relevant* parts of the Gradle app is structured as follows (`app`):
- `build.gradle`: Defines the dependencies for the Gradle app: ForSyDe IO, through a number of sub-libraries (core, libforsyde, graphviz). These are referencing the ForSyDe IO repository with either branch names, or commit hashes to tailor which versions to use.
- `src/main/java/`
    - `App.java`: The main class of the app and is used to parse command line arguments and call the appropriate functions to create system models, parse design solutions, etc.
    - `models/application_model`: One file (`ApplicationBuilder.java`) that acts as a wrapper around the ForSyDe IO library to create application models via generic functions. This wrapper class can be instantiated in `ApplicationHandler.java` to create arbitrary application models within the given modeling scope. Applications are represented by the Synchronous Data Flow (SDF) model of computation with Actors and Channels.
    - `models/platform_model`: Same structure as the application model, but for the creating arbitrary platform models with memory elements, communication elements, FPGAs, CPUs and how they are interconnected. This wrapper class can be instantiated in  `PlatformHandler.java` to create arbitrary platform models within the given modeling scope.
    - `models/artifacts`: Each invocation of the run-script creates a new subfolder here. The subfolder contains the created system models (`.fiodl`), the design solutions (`solutionX.fiodl`) and cleartext solutions (`solutionX.txt`).
    - `models/utils`: Folder for storing utility functions used in the app. Includes units, constants, the parser for design solutions among other necessities. 

## Development Setup using Docker
_NOTE: This is only tested to work on **Ubuntu 22:04**, other Linux distributions are unknown and Windows is ensured to not be compatible (IDeSyDe image cannot be built)_

The development setup used VSCode for all interaction with `ForSyDe IO` and `IDeSyDe`, having this repo open and the run script available in the integrated terminal. Docker is used to host the runtime environment so it needs to be [installed](https://docs.docker.com/engine/install/) first.

**Clone this repository**:

`git clone git@github.com:saab/dse-for-mpsoc-thesis-2024.git`

**Initialize the IDeSyDe submodule**:

`git submodule init`

**Pull the latest content from the IDeSyDe submodule**:

`git submodule update`

**Run a Test Case for HW/SW DSE**:
This may take some time since relevant Docker containers must be built.
`./run.sh mpsoc tc1`

## Creating New or Altering System Specifications
The experimental flow originates from the command line arguments given to the run script: 
```
./run.sh <platform> <application>
```


The entrypoint to the Gradle app is `App.java` and the initial functionality recognizes the given command line option and then parses which specifications to build. 

```java
public static void main(...) {
    ...
    if (action.equals("build")) {
        CreateBuildSpecification(args);
    }
    ...
}

private static void CreateBuildSpecification(...) {
    ...
    case "mpsoc" -> PlatformHandler.MPSoCGraph();
    ...
}
```

Each unique application and platform type has its own specification function in either `ApplicationBuilder.java` or `PlatformBuilder.java`. Thus these should be extended to support new specifications. These functions interface with the corresponding functions defined in `PlatformHandler.java` and `ApplicationHandler.java`.
