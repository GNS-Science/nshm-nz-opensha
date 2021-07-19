#!pythhon3
"""
script to apply modified slip rates to subduction tiles in short order around east cape.

basically take the slip rate in col22 (for) 30km tiles) and then for each row-wise copy the value to cols 23-30
"""

import csv
from pathlib import Path


folder = Path("/home/chrisbc/DEV/GNS/opensha-new/nshm-nz-opensha/src/main/resources/faultModels")
assert folder.exists()
hkm30_tiles = csv.reader(open(Path(folder, "hk_tile_parameters_30-short.csv"),'r'))

row_rates= {}

def process(line):
    if line[0] == 'along_strike_index':
        pass
    elif line[0] == '22':
        row_rates[line[1]] = line[9]
    elif (22 < int(line[0]) < 31):
        line[9] = row_rates[line[1]]
    return line


#header = hkm30_tiles.next()
# print(header)
# print(header.index("slip_deficit (mm/yr)"))
#
"""
along_strike_index,
down_dip_index,
lon1(deg),
lat1(deg),
lon2(deg),
lat2(deg),
dip (deg),
top_depth (km),
bottom_depth (km),
slip_deficit (mm/yr),
tile_geometry
"""
output = []
for row in hkm30_tiles:
    output.append(process(row))

outf = csv.writer(open(Path(folder, "hk_tile_parameters_30-short-flat-eastcape.csv"), 'w' ))

outf.writerows(output)

