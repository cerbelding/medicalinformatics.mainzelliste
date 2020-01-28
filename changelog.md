### rc 1.8.0 - 2019-08-06

New features:

- Privacy-preserving record linkage using Bloom filter instead of plain text
- Deployment via Docker (BETA)
- Development via Docker (BETA)
- Matching with external IDs and/or IDATs (identifying data)
- Optional fields can now added or edited later
- Editing of external generated IDs
- Request all patient Id types (readAllPatientIdTypes)
- Request all patient ids (readAllPatientIds)
- ElasticIDGenerator - Create IDs with given ID length and vocabulary (formerly MasterIDGenerator)
- Delete Patient via REST-API
- Soundex-like-blocking - Many thanks to [Ziad Sehili](https://bitbucket.org/%7B087d1ddc-2490-4c54-a353-a5aaf2d0cfed%7D/) from the Leipzig University

Bug fixes:

- Correction in token handling
- Use correct NULL representation of JSONObject

### 1.7.0

This release introduces support for externally generated IDs. This feature introduces an incompatible API change; therefore the API version is bumped to 3.0. However, old API versions are still supported by means of the `mainzellisteApiVersion` header, i.e. this software release is fully backwards compatible. 

New features:

- Support for externally generated IDs. See the [Wiki page on this topic](https://bitbucket.org/medicalinformatics/mainzelliste/wiki/External-IDs) for further information on how to use this feature.
- The admin form for editing patient data now shows duplicates and potential duplicates of the patient.

Bug fixes:

- Trying to edit a field that is not listed as editable in the `editPatient` token lead to HTTP status code 400 (Bad Request) instead of the more suitable 401 (Unauthorized).
- Edit requests sometimes lead to an error message saying that date fields are missing even when the date was not be edited at all.
- Some API methods returned inappropriate status codes for certain error conditions. 
- All writing API methods are now synchronized to prevent race conditions.

### 1.6.2

Adds `<attachClasses>` configuration parameter to Maven WAR Plugin in order to make classes accessible as a library. 

###1.6.1

This is a bugfix release for restoring compatibility with Java 7. Version 1.6.0 does not compile with Java versions <8 due to an unimplemented interface method, which has a default implementation in Java 8.

Fix submitted by Stephan Rusch (Universitätsklinikum Freiburg).

###1.6.0

This release further enhances the mechanism to choose the UI language and includes some important fixes, notably the contribution of changes to prevent memory leaks contributed by Daniel Volk. Again, upgrading is possible from all previous releases by replacing the binary.   

#### New features:

- The language of the user forms can be set in the configuration file (e.g. `language = en`). This setting overrides all other means of setting the language.
- The default language is now always English (it used to be the server's system language).
- If the language is induced from the `Accept-Language` header, all languages listed therein are tried, respecting the preference order. Previously, if for example Albanian (or any other language for which no localization file is included) and German were listed in the header in this order, Mainzelliste did not consider the second choice (German) but used the server language.
- When a cross origin (CORS) request issues an origin domain not listed as acceptable source, Mainzelliste now cancels the request. Previously, it processed the request nevertheless and relied on the web browser blocking the response. 
- Trailing whitespace is now trimmed from configuration parameters, as this was a common source of errors.
- When printing the result of an ID request, the page order is omitted for a better print layout.
- The logo can now also be read from a relative path within the .war file or from the directory `META-INF/resources` within a .jar file on the class path (contributed by Daniel Volk, see pull request #32).
  

#### Bug fixes:

- When a text field was set to `null` via a PUT request in JSON format, the string `"null"` was saved in the database and returned upon reading the field. To fix this, `null` values are now converted to empty strings.
- Setting an empty value for field of type `IntegerField` failed with an exception.
- Fixed memory leaks caused by the Java preferences system and improper shutdown of resources (contributed by Daniel Volk, see pull request #36).
- Fixed logging during token creation and dependency errors in some IDEs (contributed by Daniel Volk, see pull request
  #34).  

#### Other changes:

- Added debug logging about weight generation during record linkage. 

###1.5.0

This release introduces a couple of new features and bug fixes, the addition of language selection via URL parameter being the only (backward compatible) change to the public API (now version 2.2). We recommend an upgrade to all users, which is possible from any earlier release without any steps necessary other than replacing the binary. The update is fully backwards compatible to all Mainzelliste versions down to 1.0, except for use cases with the requirement that future dates can be entered (these are now rejected by date validation).

####New features:

- A new field transformation, `StringTrimmer`, can be used to delete leading and trailing whitespace from a `PlainTextField`.
- The language of user forms can be set by providing the language code as URL parameter `language` (currently `de` and `en` are supported). If provided, this parameter overrides the choice of preferred languages in the `Accept-Language` header.
- Date validation rejects dates in the future.
- Application name and version are provided in responses and callback requests as HTTP header `Server` and `User-Agent`, respectively, in the format `Mainzelliste/x.y.z`.
- The implementation of the callback request ensures the use of state-of-the-art transport layer security (TLS) (contributed by Matthias Lemmer, see pull request #26).
- When configuration parameter `callback.allowSelfsigned` is set to `true`, self-signed certificates on the target host are accepted when making callback requests (contributed by Matthias Lemmer, see pull request #26).

####Bug fixes:

- The host name provided by the `Origin` header was checked against the configured list of hosts (configuration parameter `servers.allowedOrigins`) even if equal to the host of the Mainzelliste instance itself, i.e. treating a same-origin request like a cross-origin request (reported by Benjamin Gathmann).
- Requests with an invalid token (i.e. non-existent or of wrong type) lead to status code 400 (Bad Request), now 401 (Unauthorized) is returned.
- Fixed internationalization of title in result page shown after a patient has been created (patientCreated.jsp).

####Other changes:

- Changed data type annotation for `Patient#fieldsString` and `Patient#inputFieldsString` to `@Lob` for portable mapping of unbounded character strings to appropriate database types.
- Internal improvements in class `Persistor`.

###1.4.3

This is a backward compatible maintenance release and we recommend an upgrade to all users. Also, we strongly recommend to incorporate the changes in the configuration template into actual configuration files (see comments below).

####Changes in matching configuration:
- Fixes a typo in the proposed matching configuration. In existing configuration files based on the template (`mainzelliste.conf.default`), the value for `matcher.epilink.geburtsmonat.frequency` should be changed from `0.833` to `0.0833`. This change has to be made manually because the actually used configuration file resides outside of the distributed package. The location of the configuration file is defined in the context descriptor by parameter `de.pseudonymisierung.mainzelliste.ConfigurationFile` (see installation manual for details).
- Also, the proposed weight threshold for matches (`matcher.epilink.threshold_match`) has been raised from `0.9` to `0.95`. We also recommend to adopt this change in existing configuration files.

These changes prevent that a definitive match occurs for two records that differ in one component (day, month, year) of the date of birth, all other fields being equal (reported by Matthias Lemmer).

####Bug fixes:
- When creating an `addPatient` token, ID types were not checked when using data item `idTypes` (API version 2.x syntax) with declared API version missing or < 2.0.
- The configuration parameter for cross origin resource sharing and its explanation were missing in the configuration template.

###1.4.2

Fixes an encoding error in German language properties file. This version can be skipped by users who do not use the HTML interface or use their own JSP files.

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