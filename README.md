[maven]: http://maven.apache.org/
[java]: https://www.oracle.com/java/index.html
[git]: https://git-scm.com/
[make]: https://www.gnu.org/software/make
[cytoscape]: https://cytoscape.org/
[directappinstall]: http://manual.cytoscape.org/en/stable/App_Manager.html#installing-apps
[cd]: https://en.wikipedia.org/wiki/Hierarchical_clustering_of_networks
[cdservice]: https://github.com/idekerlab/communitydetection-rest-server

Community Detection App for Cytoscape
======================================

[![Build Status](https://travis-ci.org/idekerlab/cy-community-detection.svg?branch=master)](https://travis-ci.org/idekerlab/cy-community-detection) [![Coverage Status](https://coveralls.io/repos/github/idekerlab/cy-community-detection/badge.svg?branch=master)](https://coveralls.io/github/idekerlab/cy-community-detection?branch=master)


Community Detection App is a Cytoscape App that leverages third party algorithms (via [REST service][cdservice])
to perform [hierarchical clustering/community detection][cd] on a given network. Leveraging
the [REST service][cdservice] allows incorporation of algorithms not easily portable/distributable
with this App. In addition, this tool offers for biologists Term Mapping/Enrichment (also via [service][cdservice]) on the
hierarchies generated by this App.

**Publication**

TODO ADD

Requirements to use
=====================

* [Cytoscape][cytoscape] 3.7 or above
* Internet connection to allow App to connect to remote services



Installation
==============

1. Download jar from releases on this page
1. Open Cytoscape and follow instructions [here][directappinstall]


Requirements to build (for developers)
========================================

* [Java][java] 8+ with jdk
* [Maven][maven] 3.4 or above


Building manually
====================

Commands below assume [Git][git] command line tools have been installed

```Bash
# Can also just download repo and unzip it
git clone https://github.com/idekerlab/cy-community-detection

cd cy-community-detection
mvn clean test install
```

The above command will create a jar file under **target/** named
**cy-community-detection-\<VERSION\>.jar** that can be installed
into [Cytoscape][cytoscape]

COPYRIGHT AND LICENSE
=======================

[Click here](LICENSE)

Acknowledgements
=================

* TODO denote funding sources