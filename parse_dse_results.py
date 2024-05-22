import sys
import os
import json


def extract_json_results(dirp: str) -> list[dict]:
    DECISION_MODEL = "AperiodicAsynchronousDataflowToPartitionedMemoryMappableMulticoreAndPL_Orchestratror.json"
    json_files = [
        f
        for f in os.listdir(dirp)
        if f.endswith(".json")
        and "intermediate" not in f  # exclude non-pareto
        and DECISION_MODEL in f
    ]

    if len(json_files) == 0:
        raise ValueError(f"No relevant files found in {dirp}")

    json_results = []
    for f in json_files:
        with open(os.path.join(dirp, f), "r") as file:
            json_results.append(json.load(file))
            json_results[-1]["file"] = f
    return json_results


def parse_dse_results(dirp: str) -> None:
    results = extract_json_results(dirp)

    for res in results:
        print("-" * 80)
        print(f'Solution: {res["file"]}')

        # multiple dataflows (applications?)
        dataflow: dict = res.get("aperiodic_asynchronous_dataflows", [])[0]

        actors = dataflow.get("processes", [])

        actor_min_throughput: dict = dataflow.get(
            "process_minimum_throughput", {a: None for a in actors}
        )

        actor_mapping: dict = res.get(
            "processes_to_runtime_scheduling", {a: None for a in actors}
        ) | res.get("processes_to_logic_programmable_areas", {a: None for a in actors})
        breakpoint()
        execution_times: dict = res.get(
            "instrumented_computation_times", {a: None for a in actors}
        )
        wcet: dict = execution_times.get(
            "worst_execution_times", {a: None for a in actors}
        )

        for name in actors:
            print(f"Actor: {name}:")
            print(f"\t- Mapped to: {actor_mapping[name]}")
            print(f"\t- Min throughput: {actor_min_throughput[name]}")
            print(f"\t- WCET: {wcet[name]}")
        print()

        schedules: dict = res.get("super_loop_schedules", {})
        for pe in schedules:
            print(f"PE: {pe}")
            print(f'\t- Schedule: {" -> ".join(schedules[pe])}')


if __name__ == "__main__":
    print()
    args = sys.argv[1:]
    if len(args) != 1:
        raise ValueError("Usage: python3 parse_dse_results.py <dirpath_explored>")
    dirp = args[0]
    parse_dse_results(dirp)
