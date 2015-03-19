# Mainzelliste

Die Mainzelliste ist ein webbasierter Pseudonymisierungsdienst erster Stufe. Sie erlaubt die Erzeugung von Personenidentifikatoren (PID) aus identifizierenden Attributen (IDAT), dank Record-Linkage-Funktionalität auch bei schlechter Qualität identifizierender Daten. Ihre Funktionen werden über eine REST-Schnittstelle bereitgestellt.

Weitere Informationen und Dokumentation zur Mainzelliste finden Sie auf der [Projektseite der Universitätsmedizin Mainz](http://www.mainzelliste.de).

Um immer auf dem aktuellen Stand zu bleiben, registrieren Sie sich auf unserer [Mailingliste](https://lists.uni-mainz.de/sympa/subscribe/mainzelliste).

## Releaseinformationen

### 1.4

Diese Version implementiert die Schnittstellenversion 2.1 und damit die darin enthaltenen neuen Funktionen. Ausführliche Informationen dazu enthält das Schnittstellendokument, das von der [Projektseite](http://www.mainzelliste.de) heruntergeladen werden kann. Alle früheren Schnittstellenversionen werden weiterhin unterstützt. 

Wegen der zusätzlich umgesetzen Bugfixes empfehlen wir auch Anwendern, die die neuen Funktionen nicht benötigen, ein Upgrade. Das Upgrade ist von jeder früheren Version möglich und außer dem Austausch der Applikation sind keine weiteren Schritte zu unternehmen. 

Neue Funktionen:

- Patientendaten können mittels des `editPatient`-Tokens und eines PUT-Requests geändert werden.
- Sessions und Tokens können mittels DELETE-Requests explizit gelöscht werden.
- POST-Requests können genutzt werden, um andere Methoden (insbesondere PUT und DELETE) zu kapseln („method override“). Das erlaubt die Verwendung anderer Methoden als POST und GET aus HTML-Formularen.
- Cross origin resource sharing (CORS) für einfache GET-Zugriffe. Erlaubt z.B. den Abruf von IDAT aus einem Webbrowser über Domaingrenzen hinweg. Dazu müssen die erlaubten zugreifenden Hosts konfiguriert werden (siehe Konfigurationshandbuch, `servers.allowedOrigins`).
- Für Anwendungsfälle, bei denen die Redirect-Funktionalität genutzt wird, kann die Ergebnisseite, die nach dem Anlegen eines Patienten erscheint, mittels Konfiguration deaktiviert werden (siehe Konfigurationshandbuch, `result.show`).
- Für zugreifende Server können neben zugelassenen IP-Adressen nun auch Adressbereiche IPv4) in CIDR-Notation angegeben werden.
- Bei Anlegen eines Patienten (POST /patients) wird der Zeitpunkt des Zugriffs gespeichert (Vorarbeit für zukünftige Anwendungen).
- Bei Zugriff über die HTML-Schnittstelle werden Fehlermeldungen als Webseite formatiert ausgegeben. 
- Die Benutzerformulare wurden internationalisiert und stehen nun in Deutsch und Englisch zur Verfügung. Die Sprachauswahl erfolgt gemäß dem vom Browser gesendeten Header (Accept-Language), im Fall einer nicht unterstützten Sprache wird Englisch verwendet. Die englische Übersetzung wurde freundlicherweise vom Projekt „European Management Platform for Childhood Interstitial Lung Diseases“ (chILD-EU) zur Verfügung gestellt.
- Für die Benutzerformulare können Kontaktdaten und ein Logo konfiguriert werden, um den Betreiber der Instanz kenntlich zu machen (siehe Konfigurationshandbuch, `operator.*`).



Bugfixes:

- Die Fehlermeldung, die bei Eingabe einer ID unbekannten Typs ausgegeben wird, enthielt nicht den Typbezeichner, sondern die ID selbst.
- Die JSON-Ausgabe bei Zugriff aus Session / Token entsprach nicht der Schnittstellenbeschreibung.
- Tokenobjekte wurden nicht ordungsgemäß gelöscht, was in Anwendungsfällen mit langlebigen Sessions und großer Anzahl von Tokens zu Speicherproblemen führte.
- Beim Abschicken des Formulars zum Anlegen von Patienten aus dem Internet Explorer IE wurde teilweise dazu ein JSON-Objekt ausgegeben, was eine Fehlermeldung provozierte.
- Das reservierte Wort „value“ kam als Bezeichner in einer SQL-Abfrage vor, was bei Verwendung der Datenbank Firebird zu einem Datenbankfehler führte.
- Verschiedene Methoden waren nicht threadsicher, was in Anwendungsfällen mit vielen parallelen Zugriffen zu Fehlern führte.

Sonstige Änderungen:

- Die Javadoc-Kommentare des Programmcodes wurden vervollständigt.
- Der Programmcode wurde insgesamt bereinigt, das beinhaltet insbesondere die Entfernung von obsolet gewordenen Codeteilen, erledigten Kommentaren und Testmethoden.
- Der Filtername der Applikation im Deployment Descriptor (web.xml) wurde von "Mainzer ID-Framework" nach "Mainzelliste" umbenannt.
- Die Fehlermeldung bei Fehlschlagen des Callbacks nach Anlegen eines Patienten wurde differenziert, so dass insbesondere ein Mismatch zwischen angegebener und tatsächlich verwendeter API-Version schnell erkannt werden kann (Beitrag von Matthias Lemmer).

### 1.3.2
- Bugfix: Erstellung des Datenbankschemas scheiterte mit einer ReportingSQLException (gemeldet von Matthias Lemmer).

Dieses Bugfix-Release ist nur für Nutzer relevant, die eine neue Instanz der Mainzelliste auf einer leeren Datenbank aufsetzen. Bei bestehenden Instanzen kann v1.3.2 übersprungen werden.
	 
### 1.3.1
- Bugfixes und Verbesserungen:
	- GET request auf /patients mit ungültigem "readPatients"-Token verursachte eine NullPointerException.
	- Die Erstellung von "readPatients"-Tokens konnte durch Optimierung einer Datenbankabfrage und Einführung
	 eines Index beschleunigt werden.
	- Korrekturen in der Konfigurationsvorlage: 
		- Leerzeichen am Zeilenende bei "callback.allowedFormat" entfernt.
	  	- Tokenbezeichnung "readPatient" zu "readPatients" korrigiert.    

Wir empfehlen allen Nutzern das Einspielen dieses Bugfix-Releases. Eine Ausnahme sind Produktivinstanzen, die bereits konfiguriert sind und keine readPatients-Tokens verwenden; für diese kann v1.3.1 übersprungen werden.

Das Schema der SQL-Datenbank wird beim ersten Start der neuen Version aktualisiert, ggfls. mehrfach (1.0 -> 1.1 -> 1.3.1). Das geschieht automatisch, aber wie immer empfehlen wir ein Vorab-Backup der SQL-Datenbank.

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
	- Beim Löschen invalider Sessions trat eine ConcurrentModificationException auf (gemeldet von Stephan Rusch).

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
- Stephan Rusch, Universitätsklinikum Freiburg
- Jens Schwanke, Universitätsmedizin Göttingen
- Daniel Volk, Universitätsmedizin Mainz
- Dirk Langner, Universitätsmedizin Greifswald
- Matthias Lemmer, Universität Marburg
- Projekt FP7-305653-chILD-EU
