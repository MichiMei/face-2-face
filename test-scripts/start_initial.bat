::@echo off

:: start initial dummy in new cmd window, specify port for the initial dummy

:: bootstrapping address: <<<edit here>>>
SET bt_ip=217.230.149.37
::SET bt_ip=192.168.178.21
SET bt_port=30000

:: dummy ports (from...to): <<<edit here>>>
SET /a o_port1=30001
SET /a o_portN=30049

:: publish dummy port: <<<edit here>>>
SET p_port=30099

:: parameters
SET dummy=1
SET publish=2
SET directory=data



:: start initial
cd dummy
START java -Xmx50m -jar p2projekt21.jar %dummy% %bt_port% -1 -1
cd ..

pause