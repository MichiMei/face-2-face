# face-2-face

## What is face-2-face
Simple peer-to-peer social network. Everyone has his own 'personal page', which can be found by other users using his personal public (RSA) key (hashed to a 256 bit number). Pages support HTML.

## Why was it created
face-to-face was designed and implemented as a project for a university lecture about peer-to-peer systems at HU-Berlin. The goal of this project was to build any peer-to-peer application without a central instance, working with an arbitrary number of peers even if some of them fail. It was prohibited to make use of any communication library or framework above transport layer. We had about 4 months for brainstorming, application design and implementation. At the end of the lecture all projects were showcased and rated by the professors staff. face-to-face scored first place in this competition.

## Who created it
* Dániel B.
* Hans Michel Meißner
* Chris G. P.

## Building the Project
This project was created using JFormDesigner (from the IntelliJ Marketplace). As this plugin now comes for a cost, we advice to use the precompiled jar instead of building it your own (see next chapter).
> The project was created using JFormDesigner, therefore it is advised to use JFormDesigner or a similar (compatible) GUI-designer to build the project. For the build a IntelliJ IDEA is recommended.

## How to use the Program
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

## Experiments
During testing and our project showcase, we tried a network with about 100 instances distributed over several machines (3 to 5) in different LANs connected via the internet. As this project contains no way to perform NAT-hole-punching, at lest some of the nodes should have port-forwarding active, to receive incoming UDP-packages. In our experiments 90-100% of the nodes used port-forwarding
