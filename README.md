# SelfOrganizingVSRController

This is the official repository for the Artificial Life journal (special issue on Emodied Intelligence) paper TODO, hosting all the code necessary to replicate the experiments. This work was carried out at the Evolutionary Robotics and Artificial Life Laboratory (ERALLab) at the Department of Engineering and Architecture, University of Trieste (Italy).

## Scope
By running
```
java -cp libs:JGEA.jar:libs/TwoDimHighlyModularSoftRobots.jar:target/SelfOrganizingController.jar Main {args}
```
where `{args}` is a placeholder for the arguments you must provide (see below), you will launch an evolutionary optimization of the embodied, self-organizing controller for voxel-based soft robots described in the paper. At the same time, a number of evolution metadata will be saved inside the `output` folder. The project has been tested with Java `14.0.2`.

## Structure
* `src` contains all the source code for the project;
* `libs` contains the .jar files for the dependencies (see below);
* `target` contains the main .jar file.

## Dependencies
The project relies on:
* [JGEA](https://github.com/ericmedvet/jgea), for the evolutionary optimization;
* [2D-VSR-Sim](https://github.com/ericmedvet/2dhmsr), for the simulation of voxel-based soft robots.

The corresponding jars have already been included in the directory `libs`. See `pom.xml` for more details on dependencies.

## Usage
This is a table of possible command-line arguments:

Argument       | Type                                         | Optional (yes/no) | Default
---------------|----------------------------------------------|-------------------|-------------------------
seed           | integer                                      | no                | -
morphology     | string                                       | no                | -
births         | integer                                      | no                | -
pop            | integer                                      | no                | -
configuration  | {modular, plain, unmodular}                  | yes               | plain
connectivity   | {minimal, full}                              | yes               | full
transfer       | {true, false}                                | yes               | false
threads        | integer                                      | yes               | # available cores on CPU

where {...} denotes a finite and discrete set of possible choices for the corresponding argument. The description for each argument is as follows:
* seed: the random seed for the experiment;
* morphology: the robot shape to evolve with;
* births: total number of births of the evolutionary algorithm;
* pop: population size of the evolutionary algorithm;
* configuration: one of the three modularity configurations investigated in the paper;
* connectivity: wether to evolve without topology optimization (minimal) or with (full), defaults to full;
* transfer: whether to perform re-optimization (as described in the paper) or not;
* threads: the number of threads to perform evolution with. Defaults to the number of available cores on the current CPU. Parallelization is taken care by JGEA and implements a distributed fitness assessment.

## Bibliography
TODO, if accepted
