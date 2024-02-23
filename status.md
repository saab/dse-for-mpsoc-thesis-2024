
# Goals and progress
- O1: Decide which parameters for the hardware components that are relevant based on relevance for the various applications that target different computing aspects.
    - selected so far: total LE area, actor's area, "frequency", memory

- O2: Understand how platform and application specifications can be integrated with IDeSyDe and implement a dummy flow.
    - rodolfo is working on a step-by-step instruction
    - the available _Functions_ for forsyde io components are supported
    - documentation at https://forsyde.github.io/forsyde-io/usage/catalog

- O3: Decide which representation format to use for the platform specification.
    - going with forsyde io, intuitive to create via java code giving a lot of flexibility

- O4: Create a platform specification of the hardware components with the selected parameters.
    - Both CPUs should be done being modeled and connected to an OCM

- O5: Decide which representation format to use for the application specifications.
    - going with forsyde io, intuitive to create via java code giving a lot of flexibility

- O6: Derive application specifications that have a known best mapping and schedule on the platform.
    - not started

- O7: Extend the IDeSyDe exploration and identification steps to capture and analyze the new characteristics.
    - rodolfo is working on a formalization of the fpga area modeling aspect
    - rodolfo is working on a step-by-step guide on how to extend, primarily idesyde but maybe also forsyde io (although not needed due to the AddProperty() hack)

- O8: - Obtain application specifications for the given sample applications.
    - not started

- O9: Develop a method for assessing the validity and correctness of the DSE solutions.
    - not started

- O10: (Interesting) Apply DSE to more test cases to draw conclusions of the design alternatives.
    - not started