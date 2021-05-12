export PATH=/usr/lib/jvm/java-11-openjdk-amd64/bin:$PATH
export JAVA_CLASSPATH=/home/chrisbc/DEV/GNS/opensha-new/nshm-nz-opensha/build/libs/nshm-nz-opensha-all.jar
export CLASSNAME=nz.cri.gns.NZSHM22.opensha.util.NZSHM22_PythonGateway
export NZSHM22_APP_PORT=25333

cd /home/chrisbc/DEV/GNS/opensha-new
java -Xms4G -Xmx12G -classpath ${JAVA_CLASSPATH} ${CLASSNAME} > /home/chrisbc/DEV/GNS/opensha-new/nshm-nz-opensha/src/python/automation/tmp/java_app.25333.log &
python /home/chrisbc/DEV/GNS/opensha-new/nshm-nz-opensha/src/python/automation/scaling/rupture_set_builder_task.py /home/chrisbc/DEV/GNS/opensha-new/nshm-nz-opensha/src/python/automation/tmp/config.25333.json > /home/chrisbc/DEV/GNS/opensha-new/nshm-nz-opensha/src/python/automation/tmp/python_script.25333.log

#Kill the Java gateway server
kill -9 $!

