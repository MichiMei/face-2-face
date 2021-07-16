This testscripts can be used to automatically start dummy clients (with no application function).

Files for windows (.bat) and Unix (.sh) are included.
Windows files start a new cmd-window for each instance, Unix will start each instance in a new Screen-session.

start_initial.x: starts a initial dummy-node
start_dummies.x: starts several dummy-nodes with a bootstrapping address and for a port range
start_publish.x: starts a publish-dummy-node (for a given port and bootstrapping address) which publishes all pages in its data folder. The data-folder must be named 'data' and needs to be in the 'publish_dummy' subdirectory with a 'p2projekt21.jar' file.

killScreens.sh: stops all (detached) screen sessions, usefull to stop the dummies (only usable for Unix)
