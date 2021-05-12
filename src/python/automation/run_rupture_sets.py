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
from scaling.rupture_set_task_factory import RuptureSetTaskFactory

API_URL  = os.getenv('TOSHI_API_URL', "http://127.0.0.1:5000/graphql")
API_KEY = os.getenv('TOSHI_API_KEY', "")
S3_URL = os.getenv('TOSHI_S3_URL',"http://localhost:4569")


def run_tasks(general_task_id, models, crustal_files, jump_limits, ddw_ratios, strategies,
            max_cumulative_azimuths, min_sub_sects_per_parents, thinning_factors, max_sections = 1000):

    #set up a task_factory with default config
    # readlink -e $(which java)
    my_jre = os.path.dirname("/usr/lib/jvm/java-11-openjdk-amd64/bin/java")
    work_path = PurePath(os.getcwd(), "tmp")
    jar_path = "/home/chrisbc/DEV/GNS/opensha-new/nshm-nz-opensha/build/libs/nshm-nz-opensha-all.jar"
    root_folder = "/home/chrisbc/DEV/GNS/opensha-new"

    task_factory = RuptureSetTaskFactory(root_folder, work_path, jre_path=my_jre, app_jar_path=jar_path, task_config_path=work_path)
    task_count = 0

    for model in models:
        if USE_API:
            #crustal_id = ruptgen_api.upload_file(crustal_filename) ##TODO this shouldn't be needed, should find the file, pull it and then run!
            pass
        else:
            crustal_id = None

        for strategy in strategies:
            for distance in jump_limits:
                for max_cumulative_azimuth in max_cumulative_azimuths:
                    for min_sub_sects_per_parent in min_sub_sects_per_parents:
                        for ddw in ddw_ratios:
                            for thinning_factor in thinning_factors:
                                # builder = app.getBuilder()

                                task_arguments = dict(
                                    max_sections=max_sections,
                                    down_dip_width=ddw,
                                    connection_strategy=strategy,
                                    crustal_filename=None,
                                    filekey=None,
                                    fault_model=model, #instead of filename. filekey
                                    max_jump_distance=distance,
                                    max_cumulative_azimuth=max_cumulative_azimuth,
                                    min_sub_sects_per_parent=min_sub_sects_per_parent,
                                    thinning_factor=thinning_factor
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
                            return

if __name__ == "__main__":

    USE_API = True
    JAVA_THREADS = 4

    #get the root path for the task local data
    root_folder = PurePath(os.getcwd())

    #limit test size, nomally 1000 for NZ CFM
    max_sections = 200

    if USE_API:
        headers={"x-api-key":API_KEY}
        general_api = GeneralTask(API_URL, S3_URL, None, with_schema_validation=True, headers=headers)
        #create new task in toshi_api
        general_task_id = general_api.create_task(
            created=dt.datetime.now(tzutc()).isoformat(),
            agent_name=pwd.getpwuid(os.getuid()).pw_name,
            title="Build crustal ruptures with run_rupture_sets.py with new FM enum",
            description="""with more sub-tasks and max_sections=200

            FM with pooling for paralleled tasks
            """
        )

    ##Test parameters
    crustal_files = {}
    models = ["CFM_0_3_SANSTVZ"] #"CFM_0_9_SANSTVZ_D90"]

    strategies = ['UCERF3', ] #'POINTS'] #, 'UCERF3' == DOWNDIP]
    jump_limits = [4.0, 4.5, 5.0, 5.1] # , 5.1, 5.2, 5.3]
    ddw_ratios = [0.5,] # 1.0, 1.5, 2.0, 2.5]
    min_sub_sects_per_parents = [2,] #3,4]
    max_cumulative_azimuths = [560.0,] # 580.0, 600.0]
    thinning_factors = [0.0,] # 0.05, 0.1, 0.2]

    #Run the tasks....
    #actually run them ....
    pool = Pool(JAVA_THREADS)

    scripts = []
    for script_file in run_tasks(general_task_id, models,
        crustal_files, jump_limits, ddw_ratios, strategies,
        max_cumulative_azimuths, min_sub_sects_per_parents,
        thinning_factors, max_sections):

        print('scheduling: ', script_file)
        scripts.append(script_file)

    def call_script(script_name):
        print("call_script called with:", script_name)
        check_call(['bash', script_name])

    pool.map(call_script, scripts)
    pool.close()
    pool.join()

    print("Done!")
