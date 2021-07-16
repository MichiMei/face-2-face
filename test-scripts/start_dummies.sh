#!/bin/bash

# start dummies as screen sessions, specify bootstrapping address and port range (for dummies) below

#bootstrapping address: <<<edit here>>>
bt_ip=217.230.149.37
#let bt_ip=192.168.178.21
let bt_port=30100

# dummy ports (from...to): <<<edit here>>>
let o_port1=30100
let o_portN=30149

# publish dummy port: <<<edit here>>>
let p_port=30199

# parameters
let dummy=1
let publish=2
directory=data



# start publish dummy
cd publish_dummy
for (( N=$o_port1; N<=$o_portN; N++ ))
do
	screen -S dummy$N -d -m java -Xmx50m -jar p2projekt21.jar $dummy $N $bt_ip $bt_port
	echo started dummy $N
done
cd ..
