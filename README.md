## Disclaimer

This repository is a fork of the official Mainzelliste project, embodying the features described in the paper “Mainzelliste SecureEpiLinker (MainSEL): Privacy-Preserving Record Linkage using Secure Multi-Party Computation”. 
It is intended that these new features will eventually be integrated back into the official Mainzelliste software.: https://bitbucket.org/medicalinformatics/mainzelliste/src

The SecureEpilinker sMPC Node C++ source code is available at https://github.com/medicalinformatics/SecureEpilinker

# Mainzelliste

Mainzelliste is a web-based first-level pseudonymization service. It allows for the creation of personal identifiers (PID) from identifying attributes (IDAT), and thanks to the record linkage functionality, this is even possible with poor quality identifying data. The functions are available through a RESTful web interface.

Further information and documentation on Mainzelliste can be found on the [project web page of the University Medical Center Mainz](http://www.unimedizin-mainz.de/imbei/informatik/ag-verbundforschung/mainzelliste.html?L=1).

In order to receive up-to-date information on the project, you can register to be on our [mailing list](https://lists.uni-mainz.de/sympa/subscribe/mainzelliste).

The following article describes the underlying concepts of Mainzelliste and the motivation for its development. Please cite it when referring to Mainzelliste in publications:

> Lablans M, Borg A, Ückert F. A RESTful interface to pseudonymization services in modern web applications. BMC Medical Informatics and Decision Making 2015, 15:2. <http://www.biomedcentral.com/1472-6947/15/2>.

Java developers should have a look at [Mainzelliste.Client](https://bitbucket.org/medinfo_mainz/mainzelliste.client), a library that handles the HTTP calls necessary for using Mainzelliste in a client application.

Information about the official docker image of Mainzelliste you can find [here](./docker.md). 
Information about the newman tests you can find [here](./docker.md). 

## References

Mainzelliste is used in various medical joint research projects, including:

- German Mukoviszidose Register ([Data protection concept](https://www.muko.info/fileadmin/user_upload/angebote/qualitaetsmanagement/register/datenschutzkonzept.pdf))
- European chILD-EU register ([Ethics/Data Safety](http://www.klinikum.uni-muenchen.de/Child-EU/en/child-eu-register/register/ethics_data_safety/index.html))
- [German Cancer Consortium](https://ccp-it.dktk.dkfz.de/)
- Cluster for Individualized Immune Intervention (Ci3) ([Meeting abstract on IT concept](http://www.egms.de/static/de/meetings/gmds2014/14gmds106.shtml))
- Studies conducted by the [LASER group](http://www.la-ser.com/)
- The [MIRACUM consortium](http://www.miracum.org/miracolix-tools)

- The [NEW ESID online database network](https://academic.oup.com/bioinformatics/advance-article/doi/10.1093/bioinformatics/btz525/5526873)
- The [VHL-Register](https://vhl-register.org/files/VHL-Vorgehensweise.pdf)
- [CURENet](https://cure-net.de/index.php/de/aktuelles) Network researching congenital urorectal malformations
- [asthesis longlife - das BQS Register](https://www.bqs.de/leistungen/wissenschaftliche-register/18-leistungen/47-register-basis-modul-asthesis)
- The [ParaReg](https://www.dmgp-kongress.de/fileadmin/congress/media/dmgp2019/druckelemente/DMGP2019_Abstractband.pdf) Registry for lifelong monitoring of paraplegic patients
- The [NEW ESID online database network](https://academic.oup.com/bioinformatics/advance-article/doi/10.1093/bioinformatics/btz525/5526873)
- Biobank network of the Ruhr University Bochum (BioNetRUB)

Another important use case is pseudonymization in central biobanks, for example:

- [Comprehensive Biomaterial Bank Marburg](http://www.cbbmr.de/informationen-allgemein/allgemeines.html)
- [Hannover Unified Biobank](http://www.pg-ss.imi.uni-erlangen.de/SiteCollectionDocuments/Hannover_HUB_IT_Kersting.pdf)

The Mainzelliste API has been implemented in the following projects and software products:

- [OSSE – Open Source Registry System for Rare Diseases in the EU](http://osse-register.de)
- [OpenClinica](https://openclinica.com/) (see [presentation on integrating Mainzelliste and other software](https://community.openclinica.com/sites/fileuploads/akaza/cms-community/Tomas%20Skripcak%20-%20Lessons%20learned.pdf))
- [secuTrial](http://secutrial.com) (see [modules description](http://www.secutrial.com/module/))
- [Semantic Clinical Registry System for Rare Diseases](http://aksw.org/Projects/SCRS.html)
- [MOSAIC](https://mosaic-greifswald.de/) (the external interface of the "Trusted Third Party Dispatcher" is oriented towards the token-based concept of the Mainzelliste API, see [Bialke et al., J Transl Med. 2015, 13:176](http://www.translational-medicine.com/content/13/1/176))
- [German Center for Cardiovascular Disease (DZHK)](https://dzhk.de) (see MOSAIC and the [data protection concept](https://dzhk.de/fileadmin/user_upload/Datenschutzkonzept_des_DZHK.pdf))
- [German National Cohort](https://nako.de) (see MOSAIC and the [data protection concept](http://nationale-kohorte.de/wp-content/uploads/2015/07/Treuhandstellenkonzept.pdf))
- [Electronic data capture system by Fraunhofer FOKUS](https://cdn3.scrivito.com/fokus/57a537e2ec27cb7b/0a3a0655dcc079f58890e39dbdca4781/E-HEALTH_Standards_PB_03-2015_v03.pdf)
- [CentraXX](http://www.kairos.de/produkte/centraxx/) by Kairos GmbH 
- Platform for medical research by [Climedo Health GmbH](https://www.climedo.de/)
- The [NEW ESID online database network] (https://academic.oup.com/bioinformatics/advance-article/doi/10.1093/bioinformatics/btz525/5526873)
- Biobank network of the Ruhr University Bochum (BioNetRUB)

We have compiled this list from the results of public search engines. If you use the Mainzelliste or its API, we would be glad to include your project in this list. Please don't hestitate to [contact us](mailto:info@mainzelliste.de).

## Contributing

We would love to include your useful changes to the Mainzelliste code in a future official release. See the related [Wiki page](https://bitbucket.org/medicalinformatics/mainzelliste/wiki/Contributing) for further information on contributing code.

## Release notes

All notable changes to the mainzelliste project you can see in the [changelog](./changelog.md)


### Contributions
As an open source project, Mainzelliste profits from contributions from the research community. We would like to thank the following colleagues for their code contributions (sorted by name in ascending order):

- Andreas Borg, Universitätsmedizin Mainz
- Benjamin Gathmann, Universitätsklinikum Freiburg
- Christian Koch, DKFZ-Heidelberg
- Daniel Volk, Universitätsmedizin Mainz
- David Croft, DKFZ-Heidelberg
- Dirk Langner, Universitätsmedizin Greifswald
- Florens Rohde, Universität Leipzig
- Florian Stampe, DKFZ-Heidelberg
- Galina Tremper, DKFZ-Heidelberg
- Jens Schwanke, Universitätsmedizin Göttingen
- Cornelius Knopp, Universitätsmedizin Göttingen 
- Martin Lablans, DKFZ-Heidelberg
- Matthias Lemmer, Universität Marburg
- Moanes Ben Amor, DKFZ-Heidelberg
- Maximilian Ataian, Universitätsmedizin Mainz
- Project FP7-305653-chILD-EU
- Stephan Rusch, Universitätsklinikum Freiburg
- Torben Brenner, DKFZ-Heidelberg
- Ziad Sehili, Universität Leipzig
