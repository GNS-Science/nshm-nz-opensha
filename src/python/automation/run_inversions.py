import os
import pwd
import itertools
import stat
from pathlib import PurePath
from subprocess import check_call
from multiprocessing.dummy import Pool

import datetime as dt
from dateutil.tz import tzutc

from nshm_toshi_client.general_task import GeneralTask
from nshm_toshi_client.toshi_file import ToshiFile

from scaling.opensha_task_factory import OpenshaTaskFactory
from scaling.file_utils import download_files

import scaling.inversion_solution_builder_task


# Set up your local config, from environment variables, with some sone defaults
from scaling.local_config import (OPENSHA_ROOT, WORK_PATH, OPENSHA_JRE, FATJAR,
    JVM_HEAP_MAX, JVM_HEAP_START, USE_API, JAVA_THREADS,
    API_KEY, API_URL, S3_URL, CLUSTER_MODE)

# If you wish to override something in the main config, do so here ..
# WORKER_POOL_SIZE = 3
WORKER_POOL_SIZE = 1
JVM_HEAP_MAX = 30
JAVA_THREADS = 4
#USE_API = True

#If using API give this task a descriptive setting...
TASK_TITLE = "Inversions on TVZ/SansTVZ MFD - Coulomb"
TASK_DESCRIPTION = """

Total of 64 jobs
 - 4 Coulomb rupture sets from R2VuZXJhbFRhc2s6OTMyNDRibg==
 - rounds = 1
 - max_inversion_times = [8*60,] #3*60,]  #units are minutes
 - mfd_equality_weights = [1, 10, 100, 1000]
 - mfd_inequality_weights = [0, 10, 100, 1000]
 - slip_rate_weighting_types = ['UNCERTAINTY_ADJUSTED',]
 - slip_rate_weights = [1000,]
 - slip_uncertainty_scaling_factors = [2,]
 - completion_energies = [0.0] (disabled)

"""

def run_tasks(general_task_id, rupture_sets, rounds, completion_energies, max_inversion_times,
        mfd_equality_weights, mfd_inequality_weights, slip_rate_weighting_types,
        slip_rate_weights, slip_uncertainty_scaling_factors):
    task_count = 0
    task_factory = OpenshaTaskFactory(OPENSHA_ROOT, WORK_PATH, scaling.inversion_solution_builder_task,
        initial_gateway_port=25933,
        jre_path=OPENSHA_JRE, app_jar_path=FATJAR,
        task_config_path=WORK_PATH, jvm_heap_max=JVM_HEAP_MAX, jvm_heap_start=JVM_HEAP_START,
        pbs_ppn=JAVA_THREADS,
        pbs_script=CLUSTER_MODE)

    for (rid, rupture_set_info) in rupture_sets.items():
        for (round, completion_energy, max_inversion_time,
                mfd_equality_weight, mfd_inequality_weight, slip_rate_weighting_type,
                slip_rate_weight, slip_uncertainty_scaling_factor)\
            in itertools.product(
                rounds, completion_energies, max_inversion_times,
                mfd_equality_weights, mfd_inequality_weights, slip_rate_weighting_types,
                slip_rate_weights, slip_uncertainty_scaling_factors):

            task_count +=1

            task_arguments = dict(
                round = round,
                rupture_set_file_id=rupture_set_info['id'],
                rupture_set=rupture_set_info['filepath'],
                completion_energy=completion_energy,
                max_inversion_time=max_inversion_time,
                mfd_equality_weight=mfd_equality_weight,
                mfd_inequality_weight=mfd_inequality_weight,
                slip_rate_weighting_type=slip_rate_weighting_type,
                slip_rate_weight=slip_rate_weight,
                slip_uncertainty_scaling_factor=slip_uncertainty_scaling_factor
                )

            job_arguments = dict(
                task_id = task_count,
                round = round,
                java_threads=JAVA_THREADS,
                jvm_heap_max = JVM_HEAP_MAX,
                java_gateway_port=task_factory.get_next_port(),
                working_path=str(WORK_PATH),
                root_folder=OPENSHA_ROOT,
                general_task_id=general_task_id,
                use_api = USE_API,
                output_file = f"{str(WORK_PATH)}/InversionSolution-{str(rid)}-rnd{round}-t{max_inversion_time}.zip",
                )

            #write a config
            task_factory.write_task_config(task_arguments, job_arguments)

            script = task_factory.get_task_script()

            script_file_path = PurePath(WORK_PATH, f"task_{task_count}.sh")
            with open(script_file_path, 'w') as f:
                f.write(script)

            #make file executable
            st = os.stat(script_file_path)
            os.chmod(script_file_path, st.st_mode | stat.S_IEXEC)

            yield str(script_file_path)
            return

if __name__ == "__main__":

    t0 = dt.datetime.utcnow()

    GENERAL_TASK_ID = None

    headers={"x-api-key":API_KEY}
    general_api = GeneralTask(API_URL, S3_URL, None, with_schema_validation=True, headers=headers)
    file_api = ToshiFile(API_URL, S3_URL, None, with_schema_validation=True, headers=headers)

    #get input files from API
    upstream_task_id = "R2VuZXJhbFRhc2s6Mjk2MmlTNEs=" #Azimuthal
    upstream_task_id = "R2VuZXJhbFRhc2s6OTMyNDRibg==" #COulomb NZ CFM 0.3 & 0.9 with current UCERF4 defaults
    rupture_sets = download_files(general_api, file_api, upstream_task_id, str(WORK_PATH), overwrite=False)

    if USE_API:
        #create new task in toshi_api
        GENERAL_TASK_ID = general_api.create_task(
            created=dt.datetime.now(tzutc()).isoformat(),
            agent_name=pwd.getpwuid(os.getuid()).pw_name,
            title=TASK_TITLE,
            description=TASK_DESCRIPTION
        )

        print("GENERAL_TASK_ID:", GENERAL_TASK_ID)

    rounds = range(1)
    completion_energies = [0.0,] # 0.005]
    max_inversion_times = [8*60,] #3*60,]  #units are minutes
    max_inversion_times.reverse()

    mfd_equality_weights = [0, 10, 100, 1000]
    mfd_inequality_weights = [0, 10, 100, 1000]
    slip_rate_weighting_types = ['UNCERTAINTY_ADJUSTED',]
    slip_rate_weights = [1000,]
    slip_uncertainty_scaling_factors = [2,]

    pool = Pool(WORKER_POOL_SIZE)

    scripts = []
    for script_file in run_tasks(GENERAL_TASK_ID, rupture_sets, rounds, completion_energies, max_inversion_times,
        mfd_equality_weights, mfd_inequality_weights, slip_rate_weighting_types,
        slip_rate_weights, slip_uncertainty_scaling_factors
        ):
        print('scheduling: ', script_file)
        scripts.append(script_file)

    def call_script(script_name):
        print("call_script with:", script_name)
        if CLUSTER_MODE:
            check_call(['qsub', script_name])
        else:
            check_call(['bash', script_name])

    print('task count: ', len(scripts))
    print('worker count: ', WORKER_POOL_SIZE)

    pool.map(call_script, scripts)
    pool.close()
    pool.join()

    print("Done! in %s secs" % (dt.datetime.utcnow() - t0).total_seconds())
