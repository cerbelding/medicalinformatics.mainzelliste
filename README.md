# Mainzelliste

Die Mainzelliste ist ein webbasierter Pseudonymisierungsdienst erster Stufe. Sie erlaubt die Erzeugung von Personenidentifikatoren (PID) aus identifizierenden Attributen (IDAT), dank Record-Linkage-Funktionalität auch bei schlechter Qualität identifizierender Daten. Ihre Funktionen werden über eine REST-Schnittstelle bereitgestellt.

Weitere Informationen und Dokumentation zur Mainzelliste finden Sie auf der [Projektseite der Universitätsmedizin Mainz](http://www.mainzelliste.de).

Um immer auf dem aktuellen Stand zu bleiben, registrieren Sie sich auf unserer [Mailingliste](https://lists.uni-mainz.de/sympa/subscribe/mainzelliste).

## Releaseinformationen

### 1.3
- Mit Erscheinen dieser Version liegt erstmals eine umfassende Schnittstellenbeschreibung vor.
  Sie kann als PDF-Dokument von der Projektseite (s.o.) heruntergeladen werden. Softwareversion 1.3
  unterstützt sowohl die bisherige (1.0) als auch die aktuelle (2.0) Schnittstellenversion. 
- Die Implementierung der Schnittstellenversion 2.0 bietet folgende Neuigkeiten und Änderungen:
	- Mittels des „readPatients“-Tokens und eines GET-Requests können IDs und IDAT vorhandener Patienten
	  ausgelesen werden.
	- Im Redirect-Aufruf kann die Token-ID mittels einer speziellen Template-Variable als URL-Parameter 
	  übergeben werden.
	- Beim Anlegen eines „addPatient“-Tokens können mehrere zu erzeugende ID-Typen als Array im Parameter 
	  „idTypes“ angegeben werden. Die bisherige Syntax (ein ID-Typ als String) entfällt.
	- Der Callback-Aufruf überträgt nicht mehr nur eine einzige ID, sondern alle im „addPatient“-Token
	  angefragten IDs als Array. 
	- Entsprechendes gilt für die Rückgabe von POST /patients bei Nutzung des JSON-Formats (Accept: 
	  application/json).
- Bugfixes:
	- Ein Fehler im Record Linkage führte dazu, dass die Kombination aus gleichen Nachnamen, 
	  verschiedenen Vornamen und leeren Geburtsnamenfeldern als Übereinstimmung klassifiziert wurde (gemeldet von Benjamin Gathmann).
	- Ein in der Konfiguration angegebener Session-Timeout wurde nicht, wie dokumentiert, in Minuten, 
	  sondern in Sekunden berechnet.
	- Der Callbackaufruf funktionierte nicht wie dokumentiert (gemeldet von Michael Storck).
	- In der Beispielkonfiguration war „ß“ nicht als gültiges Zeichen definiert (gemeldet von Benjamin Gathmann).
	- Beim Löschen invalider Sessions trat eine ConcurrentModificationException auf (gemeldet von Benjamin Gathmann).

Wir empfehlen allen Benutzern ein Update auf die aktuelle Version. Das Update nimmt keine Datenbankänderungen vor.

Die Schnittstellenänderungen werden nur aktiv, wenn ein zugreifendes System die Schnittstellenversion 2.0 anfordert (für Details siehe Schnittstellendokument). Die Implementierung ist damit abwärtskompatibel zu den bisherigen Veröffentlichungen.


### 1.2
- Sessions verfallen bei Inaktivität nach einer konfigurierbaren Zeit (Details siehe Konfigurationshandbuch, Parameter „sessionTimeout“)
- In der Konfiguration können Felder definiert werden, die nicht im Record Linkage verwendet werden. Diese werden als Zusatzfelder in der Datenbank gespeichert.
- Bugfixes:
	- Monat des Geburtsdatums wurde auf Ergebnisseite falsch angezeigt (behoben von Daniel Volk)
	- Löschen einer nicht-existenten Session verursachte NullPointerException
	- Fehler in der Gewichtsberechnung des EpilinkMatcher führte in manchen Fällen zu zu hohen Matchgewichten (gemeldet von Dirk Langner) 

Beim Update ist zu beachten, dass in Version 1.2 Sessions standardmäßig nach 10 Minuten Inaktivität gelöscht werden. Falls dies nicht erwünscht ist, ist der Konfigurationsparameter "sessionTimeout" anzupassen.

Das Update nimmt keine Datenbankänderungen vor.

### 1.1
- Umstellung von Abhängigkeitsmanagement und Build auf Maven (Beitrag von Jens Schwanke)
- Zusatzinformationen in context.xml-Vorlage für die Benutzung in Netbeans
- Bugfixes:
	- Fehlerhafte Zeichen in Konfigurationsvorlage (gemeldet von Maximilian Ataian)
	- Fehlerhafter Standardpfad für Konfigurationsdatei
	- Fehler in IDGeneratorMemory verursachte Datenbankfehler wegen doppeltem Primary Key

Allen Benutzern wird das Update auf Version 1.1 empfohlen. Beim ersten Start werden Änderungen an der Datenbank zur Anpassung an die neue Version vorgenommen. Zur Sicherheit empfehlen wir, vor dem Update die Datenbank zu sichern.

Entwickler benötigen Maven-Unterstützung, um das Projekt zu öffnen. Hinweise dazu finden sich in der aktualisierten Entwicklerdokumentation.  

### 1.0
- Erste Veröffentlichung

## Beiträge
Als Communityprojekt lebt die Mainzelliste von den Beiträgen der Forschergemeinschaft. Wir bedanken uns für die Codebeiträge der folgenden Kollegen:

- Maximilian Ataian, Universitätsmedizin Mainz
- Benjamin Gathmann, Universitätsklinikum Freiburg
- Jens Schwanke, Universitätsmedizin Göttingen
- Daniel Volk, Universitätsmedizin Mainz
- Dirk Langner, Universitätsmedizin Greifswald
