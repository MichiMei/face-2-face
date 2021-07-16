#!/bin/bash

# start publish-dummy screen session, specify bootstrapping address and port (for publish-dummy)

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



# start publish dummy
cd publish_dummy
screen -S publish -d -m java -Xmx50m -jar p2projekt21.jar $publish $p_port $bt_ip $bt_port $directory
echo started publish dummy
cd ..
