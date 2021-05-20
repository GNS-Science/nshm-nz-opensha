import json
import git
import csv
import os
import pwd
from pathlib import PurePath
import platform

import stat

from subprocess import check_call
from multiprocessing.dummy import Pool

import datetime as dt
from dateutil.tz import tzutc

from nshm_toshi_client.general_task import GeneralTask
from scaling.opensha_task_factory import OpenshaTaskFactory


API_URL  = os.getenv('TOSHI_API_URL', "http://127.0.0.1:5000/graphql")
API_KEY = os.getenv('TOSHI_API_KEY', "")
S3_URL = os.getenv('TOSHI_S3_URL',"http://localhost:4569")

USE_API = False
JAVA_THREADS = 4
WORKER_POOL_SIZE = 1
JVM_HEAP_MAX = 24
JVM_HEAP_START = 4


def run_tasks(general_task_id, rupture_sets, completion_energies, max_inversion_time):

    #set up a task_factory with default config
    # readlink -e $(which java)
    my_jre = os.path.dirname("/usr/lib/jvm/java-11-openjdk-amd64/bin/java")
    work_path = PurePath(os.getcwd(), "tmp")
    jar_path = "/home/chrisbc/DEV/GNS/opensha-new/nshm-nz-opensha/build/libs/nshm-nz-opensha-all.jar"
    root_folder = "/home/chrisbc/DEV/GNS/opensha-new"

    task_factory = OpenshaTaskFactory(root_folder, work_path, python_script="inversion_solution_builder_task.py",
        jre_path=my_jre, app_jar_path=jar_path,
        task_config_path=work_path, jvm_heap_max=JVM_HEAP_MAX, jvm_heap_start=JVM_HEAP_START,)
    task_count = 0

    for rupture_set in rupture_sets:
        for completion_energy in completion_energies:
            task_arguments = dict(
                rupture_set=rupture_set,
                completion_energy=completion_energy,
                max_inversion_time=max_inversion_time,
                scaling_relationship='TMG_CRU_2017', #'SHAW_2009_MOD'
                )


            job_arguments = dict(
                java_threads=JAVA_THREADS,
                java_gateway_port=task_factory.get_next_port(),
                working_path=str(work_path),
                root_folder=root_folder,
                general_task_id=general_task_id,
                use_api = USE_API,
                )

            #write a config
            task_factory.write_task_config(task_arguments, job_arguments)

            script = task_factory.get_task_script()
            task_count +=1

            # print((">" * 4) + f"TASK {task_count} " + (">" * 10))
            # print(script)
            # print('<' * 20)

            script_file_path = PurePath(work_path, f"task_{task_count}.sh")
            with open(script_file_path, 'w') as f:
                f.write(script)

            #make file executable
            st = os.stat(script_file_path)
            os.chmod(script_file_path, st.st_mode | stat.S_IEXEC)

            yield str(script_file_path)


if __name__ == "__main__":

    t0 = dt.datetime.utcnow()

    general_task_id = None

    """
    1) Baseline Inversion energy completion

    Test inversion energy Completion impacts:

    """

    if USE_API:
        headers={"x-api-key":API_KEY}
        general_api = GeneralTask(API_URL, S3_URL, None, with_schema_validation=True, headers=headers)
        #create new task in toshi_api
        general_task_id = general_api.create_task(
            created=dt.datetime.now(tzutc()).isoformat(),
            agent_name=pwd.getpwuid(os.getuid()).pw_name,
            title="Baseline Inversion energy completion",

            description="""Test inversion energy Completion impacts:

permutations:
 -
"""
        )

    ##Parameters
    rupture_sets = [
        "RupSet_Az_FM(CFM_0_9_SANSTVZ_D90)_mxSbScLn(0.5)_mxAzCh(60.0)_mxCmAzCh(560.0)_mxJpDs(5.0)_mxTtAzCh(60.0)_thFc(0.0).zip",
        "RupSet_Az_FM(CFM_0_9_SANSTVZ_D90)_mxSbScLn(0.5)_mxAzCh(60.0)_mxCmAzCh(560.0)_mxJpDs(5.0)_mxTtAzCh(60.0)_thFc(0.1).zip"
    ]
    rupt_folder = "/home/chrisbc/DEV/GNS/opensha-new/DATA/2022-05-19-02/"
    rupture_set_paths = [rupt_folder + fname for fname in rupture_sets]

    completion_energies = [0.1,] # 0.1, 0.001]
    max_inversion_time = 1  #units are minutes

    pool = Pool(WORKER_POOL_SIZE)

    scripts = []
    for script_file in run_tasks(general_task_id, rupture_set_paths, completion_energies, max_inversion_time):
        print('scheduling: ', script_file)
        scripts.append(script_file)

    def call_script(script_name):
        print("call_script called with:", script_name)
        check_call(['bash', script_name])

    pool.map(call_script, scripts)
    pool.close()
    pool.join()

    print("Done! in %s secs" % (dt.datetime.utcnow() - t0).total_seconds())
