
import sys, os, json


def extract_json_results(dirp: str) -> list[dict]:
    DECISION_MODEL = \
        'AperiodicAsynchronousDataflowToPartitionedMemoryMappableMulticore_Orchestratror.json'
    json_files = [
        f for f in os.listdir(dirp) 
        if f.endswith('.json') 
        and 'intermediate' not in f # exclude non-pareto
        and DECISION_MODEL in f
    ]

    if len(json_files) == 0:
        raise ValueError(f'No relevant files found in {dirp}')

    json_results = []
    for f in json_files:
        with open(os.path.join(dirp, f), 'r') as file:
            json_results.append(json.load(file))
            json_results[-1]['file'] = f
    return json_results


def parse_dse_results(dirp: str) -> None:
    json_results = extract_json_results(dirp)

    for res in json_results:
        print("-" * 80)
        print(f'Solution: {res["file"]}')
        dataflow: dict = res.get('aperiodic_asynchronous_dataflows', [])[0] # multiple dataflows (applications?)
        actors = dataflow.get('processes', [])
        actor_min_throughput: dict = dataflow.get('process_minimum_throughput', {a: None for a in actors})

        actor_mapping: dict = res.get('processes_to_runtime_scheduling', {a: None for a in actors})
        execution_times: dict = res.get('instrumented_computation_times', {a: None for a in actors})
        wcet: dict = execution_times.get('worst_execution_times', {a: None for a in actors})
        avg_et: dict = execution_times.get('average_execution_times', {a: None for a in actors})

        for name in actors:
            print(f'Actor: {name}:') 
            print(f'\t- Mapped to: {actor_mapping[name]}')
            print(f'\t- Min throughput: {actor_min_throughput[name]}')
            print(f'\t- WCET: {wcet[name]}')
            print(f'\t- Avg ET: {avg_et[name]}')
        print()

        schedules: dict = res.get('super_loop_schedules', {})
        for pe in schedules:
            print(f'PE: {pe}')
            print(f'\t- Schedule: {" -> ".join(schedules[pe])}')


if __name__ == '__main__':
    print()
    args = sys.argv[1:]
    if len(args) != 1:
        raise ValueError('Usage: python parse_dse_results.py <directory>')
    dirp = args[0]
    parse_dse_results(dirp)
    