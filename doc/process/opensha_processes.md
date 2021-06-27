
# Opensha Processes

## 1. Rupture sets

Rupture sets are built using the `run_{TYPE}_rupture_sets.py scripts`. There are three scripts, one each totr Azimuthal, Coulomb and Subduction.

### 1.1 Producing rupture sets

 - update the rupture specifications and the the job descriptions in `automation\run_{TYPE}_rupture_sets.py`
 - run the script (note that coulomb must be run on cluster due to memory demands)
 - make sure the Toshi env vars are set

#### example, building subduction ruptures



## s5cmd to transfer S3 data efficiently

examples

```
./s5cmd -numworkers 64 cp --acl public-read DATA9/ s3://nzshm22-rupset-diags-poc/DATA9/
./s5cmd --stat -numworkers 128 cp --acl public-read index.html s3://nzshm22-rupset-diags-poc/
```
