# p2projekt21
Authors: Dániel B., Hans Michel Meißner, Chris G. P.

## About the Project
Simple peer-to-peer social network. Everyone has his own 'personal page', which can be found by other users using his personal public (RSA) key (hashed to a 256 bit number). Pages support HTML.

## Building the Project
The project was created using JFormDesigner, therefore JFormDesigner or a similar GUI-designer is required to build the project (however JFormDesigner is highly recommended).
For the build a IntelliJ IDEA is recommended.
1. Install IntelliJ (https://www.jetbrains.com/de-de/idea/download/)
2. After the first start press 'Configure'->'Plugins', search for 'JFormDesigner' and install it.
3. Use 'Get from Version Control' to clone the project (https://scm.cms.hu-berlin.de/balintda/p2projekt21) 
4. If asked open as 'Maven project' (lower right corner as pop-up)
Now the Project can be started using the IDE or build using 'Build'->'Build Artifacts...'->'Build'. The p2projekt21.jar will be in <project_name>/out/artifacts/p2projekt21_jar/p2projekt21.jar

## Use the Program
You can run the project using the already compiled p2projekt21.jar (<project_name>/out/artifacts/p2projekt21_jar/p2projekt21.jar) with the command 'java -jar p2projekt21.jar'
The program will start with default parameters, asking for (an optional) own UDP-port and a bootstrapping address and port.
Next the GUI will show up, showing your own public key and 'personal page'. Pressing 'Upload' will try to connect the network and publish your page. You can switch to the tab 'Friends' to insert a friends key and search for his 'personal page'

## Expert Mode
The program can be called with several parameters:
java -jar p2projekt21.jar <mode> <own_port> <bootstrapping_address> <bootstrapping_port> <default_pages_dir>
- mode: 0=standard-gui-mode; 1=dummy-mode, no GUI and only kademlia functionality; 1-publish-dummy-mode, reads all pages from the specified directory and publishes them
- own_port: specify your own UDP-port (e.g. in case of port-forwarding), -1 for arbitrary
- bootstrapping_address: specify the address of a connected host, -1 skips bootstrapping
- bootstrapping_port: specify the port of the (same) connected host, -1 skips bootstrapping
- default_pages_dir: only for mode 2, directory containing pages (with public key and signature in the correct format) to publish in the network

Modes 1 and 2 are mainly for testing purposes to be able to run a greater number of kademlia nodes in the network. They only keep kademlia running without publishing or requesting any pages (with exception of publish dummies which will publish and republish the default pages).
