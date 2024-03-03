# nshm-nz-opensha
NSHM NZ Programme opensha applications (patterned on opensha-ucerf3 &amp; opensha-dev)

## Priorities:

 - adapt the NZ Fault model from CFM NZ to suit the opensha tools
 - extend opensha to support subduction interface fault models. This is needed for the (in)famous 'Hikurangi'. 

## Getting started

You need to have `git` and `jdk11` installed.

 ```
git clone https://github.com/GNS-Science/opensha.git &&\
git clone https://github.com/GNS-Science/nshm-nz-opensha.git
 ```

You might need to check out the correct branch for the `opensha` project. The branch name will be in `gradle.yml` in the 
`nshm-nz-opensha` project as the `ref` of the `Clone opensha` step. As of writing, this is 
`chrisbc/make_gridSourceProvider_protected`.

```bash
cd opensha
git checkout chrisbc/make_gridSourceProvider_protected
```

### Now you can jump into this project

 ```
 cd nshm-nz-opensha
 ```

### and build ....

 ```
 .\gradlew build
 ```

### or test just this code

```
 .\gradlew localTests --info
```
 
Test reports are found at  `./build/reports/tests/localTests/index.html`

### or test everything (slow....)
```
 .\gradlew test
```




