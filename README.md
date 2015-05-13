# Mainzelliste

Mainzelliste is a web-based first-level pseudonymization service. It allows for the creation of personal identifiers (PID) from identifying attributes (IDAT), and thanks to the record linkage functionality, this is even possible with poor quality identifying data. The functions are available through a RESTful web interface.

Further information and documentation on Mainzelliste can be found on the [project web page of the University Medical Center Mainz](http://www.mainzelliste.de).

In order to receive up-to-date information on the project, you can register to be on our [mailing list](https://lists.uni-mainz.de/sympa/subscribe/mainzelliste).

The following article describes the underlying concepts of Mainzelliste and the motivation for its development. Please cite it when referring to Mainzelliste in publications:

> Lablans M, Borg A, Ückert F. A RESTful interface to pseudonymization services in modern web applications. BMC Medical Informatics and Decision Making 2015, 15:2. <http://www.biomedcentral.com/1472-6947/15/2>.


## Release notes

###1.4.1

This is a bug fix release and we recommend an upgrade to all users. Upgrading is possible from every earlier release, and there are no steps necessary apart from replacing the binary. 

####Bug fixes:

- When a patient had been edited, the updated data was not considered for record linkage until after restarting the application (reported by Benjamin Gathmann). 
- POST /sessions/{sid}/tokens returned an invalid token object (reported by Matthias Lemmer).
- Confirming an unsure case failed due to missing api version in request URL (fixed by Benjamin Gathmann, see pull request #22).
- Date validation accepted some illegal dates when data was not entered through the select lists in the HTML form (fixed by Benjamin Gathmann, see pull request #24).
- Some Instances of EntityManager were not closed on errors, leading to a memory leak. 


####Other changes:

- Removed references to deleted Javascript files (contributed by Benjamin Gathmann, see pull request #20).
- The version number is now read from pom.xml, i.e. this is the only place in the source code where the version number is set.
- All HTML forms have been converted to HTML 5 and validated using the W3C markup validation service (https://validator.w3.org/).
- Logging of Jersey class WebComponent has been limited to SEVERE to avoid excessive warning messages in use cases where POST /patients is used with an empty request body (see http://stackoverflow.com/questions/2011895/how-to-fix-jersey-post-request-parameters-warning).
- Field#build can now be called with null as initialization object, returning an empty field. 
- Created tokens are logged entirely only in log level DEBUG. 
 

### 1.4.0

This release implements API version 2.1, including the new features presented therein. For detailed information, we refer to the comprehensive API document (currently only available in German), which can be downloaded from the [project web site](http://www.mainzelliste.de). All preceding API versions are still supported.

As this release contains various bug fixes, we recommend an upgrade to all users, regardless of whether they plan to use the new features or not. Upgrading is possible from every earlier release, and there are no steps necessary apart from replacing the binary. As noted above, the update is fully backwards compatible to all Mainzelliste versions including 1.0.

#### New features:

- The user forms have been internationalized and are now available in English and German. Contributions for other languages are welcome: just send us the `MessageBundle_xx.properties` file for your language. The language is selected according to the `Accept-Language` header sent by the browser, with English being the default language if the requested language is not supported. The English translation was kindly provided by the project “European Management Platform for Childhood Interstitial Lung Diseases” (chILD-EU).
- Contact information and logo do not show “Universitätsmedizin Mainz” anymore but can be configured to identify the operator of the Mainzelliste instance in the user forms (see configuration parameters `operator.contact` and `operator.logo`).
- Patient data can be edited by means of an `editPatient` token and a PUT request.
- Sessions and tokens can be explicitly deleted by means of a DELETE request.
- POST requests can be used to perform other HTTP methods (notably PUT and DELETE). This technique, frequently called “method override”, allows for the use of additional methods other than GET and POST from HTML forms.
- Cross origin resource sharing (CORS) for simple GET requests. This makes it possible, for instance, to retrieve patient data via an AJAX request from a server in a different domain than the server that serves the web page. This requires configuration of the permissible hosts (see configuration parameter `servers.allowedOrigins`).
- For use cases including the redirect functionality, the result page that appears after creating a patient can be disabled (see configuration parameter `result.show`).
- In addition to specific IP addresses, IPv4 address ranges can be included in the list of addresses from which another server can get access (see configuration parameter `servers.{n}.allowedRemoteAdresses`)
- For all successful requests to add a patient (POST /patients), the timestamp is logged in the database (for future use cases).
- When making requests via the HTML interface, error messages are formatted as web pages.
tact` and `operator.logo`).
- The result page that is shown after a patient has been added now lists all requested pseudonyms according to parameter `idTypes` in the token (contributed by Matthias Lemmer).

#### Bug fixes:

- The error message issued upon entering an ID of an unknown type stated the ID value instead of the ID type.
- The JSON output of requests to sessions and tokens did not match the API documentation.
- Token objects were not garbage collected, leading to memory problems in use cases where sessions exist for a long time and contain high numbers of tokens.
- Submitting the form to add a patient from Internet Explorer returned JSON data in some cases, causing an error message due to unrecognized content.
- The reserved word `value` appeared as a name in an SQL statement, which led to a database error when using Firebird.
- Various methods turned out to be not thread-safe, causing errors in use cases where concurrent requests occur.

#### Other changes:

- All Java classes now have complete Javadoc comments.
- The source code has been cleaned up; in particular unused code, obsolete comments and test methods have been removed.
- The filter name in the deployment descriptor (web.xml) has been changed from “Mainzer ID-Framework” to “Mainzelliste”.
- The error message upon a failed callback request (while adding a patient) has been made more concrete so that the specific reason, such as a mismatch between the declared and actually used API version, can be detected more easily (contributed by Matthias Lemmer).

### 1.3.2

#### Bugfix: 

- Creation of database schema failed with ReportingSQLException (reported by Matthias Lemmer).

### 1.3.1

#### Bug fixes and improvements:

- GET request to /patients with invalid `readPatients` token caused a NullPointerException.
- Creation of `readPatients` tokens has been accelerated by optimizing the database query and using an index.
- Corrections in the template configuration file:
	- A trailing space in `callback.allowedFormat` has been removed.
	- Token type `readPatient` has been corrected to `readPatients`.

### 1.3.0

As of release 1.3.0, a comprehensive API document exists. It is available as a PDF document from the project web page (see above). Software version 1.3 supports the former (1.0) as well as the current (2.0) API version.

#### New features and API changes:

- IDs and IDAT of existing patients can be read by means of a `readPatients` token and a GET request.
- The id of the `createPatient` token can be injected into the redirect URL by using a designated template parameter.
- When creating an `addPatient` token, specific ID types that should be created can be specified as an array in the token parameter `idTypes`. This replaces the former syntax (providing a single ID type).
- The callback request transmits all IDs according to the types defined in the `addPatient` token.
- The same holds for the response to POST /patients when requesting a JSON result (`Accept: application/json`).

#### Bug fixes:

- A bug in the record linkage algorithm led to the situation that two patient records were considered a definite match when the last name agreed, the first name disagreed and the birth name was empty in both cases, provided that all other fields agreed (reported by Benjamin Gathmann).
- The session timeout set in the configuration file was interpreted as seconds instead of minutes.
- The format of the callback request did not match the documentation (reported by Michael Storck).
- The template configuration file did not define “ß” as a valid character (reported by Benjamin Gathmann).
- Deletion of invalidated sessions caused a ConcurrentModificationException (reported by Stephan Rusch).

### 1.2

#### New features:
- Sessions expire after not having been accessed for a configured time (see configuration parameter `sessionTimeout`).
- Fields can be defined without being used in the record linkage algorithm. This allows saving additional fields in the database.

#### Bug fixes:

- On the result page, the month of birth was rendered incorrectly (fixed by Daniel Volk).
- Deleting a non-existent session caused a NullPointerException.
- A bug in the weight calculation of EpilinkMatcher lead to unreasonable high matching weights in some cases (reported by Dirk Langner).

### 1.1

- Conversion from Eclipse project to Maven (contributed by Jens Schwanke).
- Added information to `context.xml.default concerning running the application in Netbeans.

#### Bug fixes:
- Wrong characters in template configuration file (reported by Maximilian Ataian).
- Wrong default path for configuration file.
- A bug in IDGeneratorMemory caused a database error due to a duplicate primary key.
	
### 1.0
- Initial release 

### Contributions
As an open source project, Mainzelliste profits from contributions from the research community. We would like to thank the following colleagues for their code contributions:

- Maximilian Ataian, Universitätsmedizin Mainz
- Benjamin Gathmann, Universitätsklinikum Freiburg
- Stephan Rusch, Universitätsklinikum Freiburg
- Jens Schwanke, Universitätsmedizin Göttingen
- Daniel Volk, Universitätsmedizin Mainz
- Dirk Langner, Universitätsmedizin Greifswald
- Matthias Lemmer, Universität Marburg
- Project FP7-305653-chILD-EU
