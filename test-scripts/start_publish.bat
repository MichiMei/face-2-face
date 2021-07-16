::@echo off

:: start publish-dummy in new cmd-window, specify bootstrapping address and port (for publish-dummy)

:: bootstrapping address: <<<edit here>>>
SET bt_ip=217.230.149.37
::SET bt_ip=192.168.178.21
SET bt_port=30100

:: dummy ports (from...to): <<<edit here>>>
SET /a o_port1=30001
SET /a o_portN=30049

:: publish dummy port: <<<edit here>>>
SET p_port=30099

:: parameters
SET dummy=1
SET publish=2
SET directory=data



:: start publish dummy
cd publish_dummy
START java -Xmx50m -jar p2projekt21.jar %publish% %p_port% %bt_ip% %bt_port% %directory%
cd ..

pause