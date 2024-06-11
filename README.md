
# Master's Thesis
### Automated Design Space Exploration for Hardware and Software Implementations on Heterogeneous MPSoCs
The [thesis](https://www.diva-portal.org/smash/search.jsf?dswid=-5961) tackled the problem of providing design space exploration support for platforms with both hardware- and software-programmable processing units. It is focusing on exploring how application execution can best be distributed to an FPGA and CPUs on the [Zynq UltraScale+ MPSoC ZCU102 Evaluation Kit](https://www.xilinx.com/products/boards-and-kits/ek-u1-zcu102-g.html#inside) to achieve best throughput and use the least amount of processing units.

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

## Development Environment
The development environment used for this project was:
- Ubuntu 22.04 LTS
- Gradle 8.1.1
- IDeSyDe 0.8.3 

## Development Setup
The IDeSyDe tool is used through an executable and some supporting folders. The IDeSyDe version that covers hardware-software design space exploration can be downloaded from this [link](https://github.com/forsyde/IDeSyDe/releases/tag/v0.8.3) (this requires [minizinc](https://www.minizinc.org/) version 2.8.5). Unzip the release in the top level directory and update the paths accordingly in `run.sh`. These **changes** are needed as development currently has the IDeSyDe repository cloned in the top level directory, and `IDeSyDe/scripts/make-all-linux.sh` is used to produce the executable.

The Gradle application is built automatically with required dependencies when the run-script is executed.

## Creating New or Altering System Specifications
Extending the platform or application specifications ...




