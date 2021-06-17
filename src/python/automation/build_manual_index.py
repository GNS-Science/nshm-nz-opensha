#build_rupture_set_index

"""
Simple script to creawte valid URLs to the rupture sets built

only to be used until we have automated rupture reporting

"""

import os
# import os.path
import shutil
import fnmatch
from pathlib import PurePath, Path
from datetime import datetime as dt
import pytz

import base64
import json
import collections

from scaling.toshi_api import ToshiApi

class GeneralTaskBuilder():
    """
    find the metadata.json and make this available for the HTML
    """
    def __init__(self, path, date_path ):
        self._dir_name = path
        self._date_path = date_path



    def get_template(self, info, mfd_dirs):
        """
        {'key': 'RmlsZTo0NTkuMDlnaEda', 'meta': {'rupture_set_file_id': 'RmlsZTo0NTkuMDlnaEda',
        'generation_task_id': 'UnVwdHVyZUdlbmVyYXRpb25UYXNrOjE4MUNqSFFa',
        'short_name': 'CFM_0_9_SANSTVZ_D90-0.1', 'rupture_class': 'Azimuth', 'max_inversion_time': '1380', 'completion_energy': '0.2', 'round_number': '0'},
        'solution_relative_path': 'UnVwdHVyZUdlbmVyYXRpb25UYXNrOjE4MUNqSFFa/InversionSolution-RmlsZTo2-rnd0-t1380_RmlsZTo0NTkuMDlnaEda.zip',
        'index_path': 'RmlsZTo0NTkuMDlnaEda/DiagnosticsReport/index.html'}

        """
        m = info['meta']
        report_info  = f"{m['short_name']} {m['rupture_class']} energy({m['completion_energy']}) round({m['round_number']})"

        if m['rupture_set_file_id'] in mfd_dirs:
            extra_link = f'&nbsp;<a href="{self._date_path}-{self.set_number}/{m["rupture_set_file_id"]}/named_fault_mfds/mfd_index.html" >Named MFDS</a>'
        else:
            extra_link = ''

        return  f'''<li>{report_info}&nbsp;
    <a href="{self._date_path}-{self.set_number}/{info['index_path']}" >Diagnostics report</a>&nbsp;
    <a href="{self._date_path}-{self.set_number}/{info['solution_relative_path']}" >Download solution file</a>
    {extra_link}</li>'''

API_URL  = os.getenv('NZSHM22_TOSHI_API_URL', "http://127.0.0.1:5000/graphql")
API_KEY = os.getenv('NZSHM22_TOSHI_API_KEY', "")
S3_URL = os.getenv('NZSHM22_TOSHI_S3_URL',"http://localhost:4569")

def gt_template(node):
    title = node.get('title')
    description = node.get('description')

    NZ_timezone = pytz.timezone('NZ')
    created = dt.strptime(node.get('created'), "%Y-%m-%dT%H:%M:%S.%f%z").astimezone(NZ_timezone)

    return f"""
    <h2>{title}</h2>
    <p>{created.strftime("%Y-%m-%d %H:%M:%S %z")}</p>

    <p>{description}</p>
    """

def rgt_template(rgt):
    """'id': 'UnVwdHVyZUdlbmVyYXRpb25UYXNrOjE4ODNXcnFN', 'created': '2021-06-10T10:23:23.457361+00:00', 'state': 'DONE', 'result': 'SUCCESS',"""
    rid = rgt['id']
    result = rgt['result']
    fname = None
    # return f'<li><a href="{TUI}RuptureGenerationTask/{rid}">Rupture set {rid}</a>result: {result}</li>'
    for file_node in rgt['files']['edges']:
        fn = file_node['node']
        if fn['role'] == 'WRITE' and 'zip' in fn['file']['file_name']:
            fname = fn['file']['file_name']
            fid = fn['file']['id']
            break

    if fname:
        return f'''<li>
            <a href="{TUI}RuptureGenerationTask/{rid}">{rid}</a> result: {result} &nbsp;
            <a href="{TUI}FileDetail/{fid}">File detail</a> &nbsp;
            <a href="{UPLOAD_FOLDER}/{fid}/DiagnosticsReport/index.html">Diagnostics report</a>
        </li>
        '''
    else:
       return f'''<li>
            <a href="{TUI}RuptureGenerationTask/{rid}">{rid}</a> result: {result}
        </li>
        '''

def inv_template(rgt):

    rid = rgt['id']
    result = rgt['result']
    fname = None
    fault_model = ""
    # return f'<li><a href="{TUI}RuptureGenerationTask/{rid}">Rupture set {rid}</a>result: {result}</li>'
    for file_node in rgt['files']['edges']:
        fn = file_node['node']
        #get solution details
        if fn['role'] == 'WRITE' and 'zip' in fn['file']['file_name']:
            fname = fn['file']['file_name']
            fid = fn['file']['id']

        #extract mmode from the rupture set
        if fn['role'] == 'READ' and 'zip' in fn['file']['file_name']:
            for kv_pair in fn['file']['meta']:
                if kv_pair['k'] == 'fault_model':
                    fault_model = kv_pair['v']
                    break


    if fname:
        return f'''<li>
            <a href="{TUI}RuptureGenerationTask/{rid}">{rid}</a> result: {result} &nbsp;
            <a href="{TUI}FileDetail/{fid}">File detail</a> &nbsp;
            <a href="{UPLOAD_FOLDER}/{fid}/mag_rates/MAG_rates_log_fixed_yscale.png">Mag Rate overall</a>
            <a href="{UPLOAD_FOLDER}/{fid}/named_fault_mfds/mfd_index.html">Named fault MFDs</a>
        </li>
        '''
    else:
       return f'''<li>
            <a href="{TUI}RuptureGenerationTask/{rid}">{rid}</a> result: {result}
        </li>
        '''

if __name__ == "__main__":

    #rupture_class = "Azimuth" #"Coulomb"

    headers={"x-api-key":API_KEY}
    general_api = ToshiApi(API_URL, S3_URL, None, with_schema_validation=True, headers=headers)

    GID = "R2VuZXJhbFRhc2s6MTg3OEtweFI=" #Azimuthal Stirling 2010
    #GID = "R2VuZXJhbFRhc2s6MjE3Qk1YREw=" #Azimuthal 3,4,5
    #GID = "R2VuZXJhbFRhc2s6MjMwWUc4TE4=" #Coulomb 3,4,5
    #GID = "R2VuZXJhbFRhc2s6MjUwYVhrVzY=" #Azimuthal 3,4,5
    GID = "R2VuZXJhbFRhc2s6MTkyS3d1ZTY=" #Coulomb Stirling
    GID = "R2VuZXJhbFRhc2s6Mjk2MmlTNEs="
    GID = "R2VuZXJhbFRhc2s6Mjk1WWlSaUo=" #Coulomb minSS 3,4,5

    TUI = "http://simple-toshi-ui.s3-website-ap-southeast-2.amazonaws.com/"
    UPLOAD_FOLDER = "DATA6"

    gentask = general_api.get_general_task_subtask_files(GID)
    node = gentask['node']

    #Write Section info
    print(gt_template(node))
    print("<ul>")

    for child_node in node['children']['edges']:
        rgt = child_node['node']['child']

        print(rgt_template(rgt))
        #print(inv_template(rgt))

    print("</ul>")
    print("<hr />")

