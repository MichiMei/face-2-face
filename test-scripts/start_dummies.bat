::@echo off

:: start dummies as new cmd-windows, specify bootstrapping address and port range (for dummies) below

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



:: start (secondary) dummies
cd dummy
FOR /L %%p IN (%o_port1%, 1, %o_portN%) DO START java -Xmx50m -jar p2projekt21.jar %dummy% %%p %bt_ip% %bt_port%
cd ..

pause