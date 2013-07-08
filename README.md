# Mainzelliste

Die Mainzelliste ist ein webbasierter Pseudonymisierungsdienst erster Stufe. Sie erlaubt die Erzeugung von Personenidentifikatoren (PID) aus identifizierenden Attributen (IDAT), dank Record-Linkage-Funktionalität auch bei schlechter Qualität identifizierender Daten. Ihre Funktionen werden über eine REST-Schnittstelle bereitgestellt.

Weitere Informationen zur Mainzelliste finden Sie auf der [Projektseite der Universitätsmedizin Mainz](http://www.mainzelliste.de).

## Releaseinformationen
### 1.1
- Umstellung von Abhängigkeitsmanagement und Build auf Maven. Hinweise zum Import finden sich in der aktualisierten Entwicklerdokumentation.
- Korrigiert irreführende Benennung in Klasse IDGeneratorMemory ("idString" -> "idType").
- Korrigiert fehlerhafte Speicherung von IDGeneratorMemory-Objekten. Symptom: Nach Neustart der Applikation führt Anlegen eines neuen Patienten zum Abbruch wegen verletzer Unique-Constraint in der Datenbank.

Wegen des letztgenannten Fehlers wird allen Benutzern das Update auf Version 1.1 empfohlen. Beim ersten Start werden Änderungen an der Datenbank zur Anpassung an die neue Version vorgenommen. Zur Sicherheit empfehlen wir, vor dem Update die Datenbank zu sichern.  

### 1.0
- Erste Veröffentlichung

