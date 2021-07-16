#!/bin/bash

# start initial dummy as screen session, specify port for the initial dummy

#bootstrapping address: <<<edit here>>>
bt_ip=217.230.149.37
#let bt_ip=192.168.178.21
let bt_port=30100

# dummy ports (from...to): <<<edit here>>>
let o_port1=30100
let o_portN=30199

# publish dummy port: <<<edit here>>>
let p_port=30199

# parameters
let dummy=1
let publish=2
directory=data



# start initial
cd dummy
screen -S initial -d -m java -Xmx50m -jar p2projekt21.jar $dummy $bt_port -1 -1
echo started initial dummy
cd ..
