<%@page import="de.pseudonymisierung.mainzelliste.Config" %>
<%@page import="java.util.ResourceBundle" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    ResourceBundle bundle = Config.instance.getResourceBundle(request);
    // pass "language" parameter from URL if given (included in form URL below)
    String languageInUrl = "";
    if (request.getParameter("language") != null) {
        languageInUrl = "&amp;language=" + request.getParameter("language");
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" type="text/css"
          href="<%=request.getContextPath() %>/static/css/patientenliste.css">
    <title><%=bundle.getString("createPatientTitle") %>
    </title>
</head>

<body>
<script>
  const UNSURE_CASE = "unsureCase";
  async function postData(url = '', headers, data = {}) {
    // Default options are marked with *
    const response = await fetch(url, {
      method: 'POST', // *GET, POST, PUT, DELETE, etc.
      mode: 'cors', // no-cors, *cors, same-origin
      cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
      credentials: 'omit', // include, *same-origin, omit
      headers: headers,
      redirect: 'follow', // manual, *follow, error
      referrerPolicy: 'no-referrer', // no-referrer, *client
      body: headers.get('Content-Type') === 'application/json' ? JSON.stringify(data) : data
    });

    if ( response.ok ) {
      if ( !response.redirected ) {
        return response.json();
      } else {
        return response.url;
      }
    } else if (response.redirected ) {
      window.location = response.url;
    } else if( response.status === 409) {
      return UNSURE_CASE;
    } else {
      throw new TypeError("Can't create PSD!");
    }
  }

  function createPSD() {
    // get data from form
    let jsonObject = {}, formData = new FormData(document.getElementById('form_person'));

    // validate form
    let isValid = true;
    formData.forEach(function (value, key) {
      if (!validate( key)) {
        isValid = false;
      }
    });
    if(!isValid) {
      return false;
    }

    //display loading box
    document.getElementById("create-box").style.display = "none";
    document.getElementById("loading-box").style.display = "";

    // convert form data to json and ignore empty string
    formData.forEach(function (value, key) {
      if (!!value || !!value.trim()) {
        jsonObject[key] = value;
      }
    });

    // create control numbers
    const cngHeader = new Headers();
    cngHeader.append('Content-Type', 'application/json');
    cngHeader.append('apiKey', '<%=  Config.instance.getGuiConfiguration().cgnApiKey %>');
    // add token id
    jsonObject['tokenId'] = '${it.tokenId}';
    postData('<%=  Config.instance.getGuiConfiguration().cngUrl %>', cngHeader, jsonObject)
    .then((data) => {
      if (!!data && !!data.idString) { // data should be a json
        showResult(data.idString, jsonObject);
      } else if (!!data && data === UNSURE_CASE) {
        showUnsureCase();
      } else { // data should be an url
        let createdId;
        try {
          createdId = new URL(!data.redirect ? data : data.redirect).searchParams.has("intid");
          if (!!createdId) {
            showResult(createdId, jsonObject);
          } else {
            window.location = data;
          }
        } catch {
          showError('<%= bundle.getString("failedRedirectionError") %>')
        }
      }
    }).catch((error) => showError('<%= bundle.getString("internalError") %>' + error) );
  }

  function showResult(createdId, jsonObject) {
    document.getElementById("result-content-id").innerHTML = createdId;
    Object.keys(jsonObject).map((key) => {
      let element = document.getElementById(key + "-result");
      if (!!element) {
        element.innerHTML = jsonObject[key];
      }
    });
    document.getElementById("loading-box").style.display = "none";
    document.getElementById("result-box").style.display = "";
  }

  function showUnsureCase(show = true) {
    changeCreateBoxToUnsureCase(show);
    document.getElementById("loading-box").style.display = "none";
    document.getElementById("create-box").style.display = "";
  }

  function changeCreateBoxToUnsureCase( show = false ) {
    document.getElementById("unsure-case-text").style.display = show ? "" : "none";
    document.getElementById("unsure-case-buttons").style.display = show ? "" : "none";
    document.getElementById("create-text").style.display = !show ? "" : "none";
    document.getElementById("create-buttons").style.display = !show ? "" : "none";
    document.getElementById("sureness").value = show
    //change input field to readOnly
    document.querySelectorAll("input[type=text]").forEach((node) => node.readOnly = show);
    document.querySelectorAll("select").forEach( node => {
      let hiddenField = document.getElementById(node.id + "-hidden");
      hiddenField.disabled = !show
      hiddenField.value = show ? node.value : "";
      node.disabled = show;
    })
  }

  function showError(error = "", show = true) {
    document.getElementById("error-content").innerHTML = show ? error : "";
    document.getElementById("loading-box").style.display = "none";
    document.getElementById("error-box").style.display = show ? "" : "none";
    // if show false go back to add patient
    if (!show) {
      document.getElementById("create-box").style.display = "";
    }
  }

  function validate( filedName ) {
    let inputElement = document.getElementById(filedName);
    // check if required
    if(!inputElement) {
      return true;
    }

    // find error message element
    let errorMessage = document.querySelector("#" + filedName + " ~ small")
    if(!errorMessage) {
      // Note: ugly workaround to find birthday error message
      if(!!inputElement.parentElement && !!inputElement.parentElement.nextElementSibling &&
          inputElement.parentElement.nextElementSibling.nodeName === 'SMALL') {
        errorMessage = inputElement.parentElement.nextElementSibling;
      }
    }

    // validate input and set error message
    if(inputElement.checkValidity()) {
      inputElement.style.borderColor = "";
      if(!!errorMessage) {
        errorMessage.style.display = "none";
      }
      return true;
    } else {
      if(!!errorMessage) {
        errorMessage.style.display = "";
        errorMessage.textContent = '<%= bundle.getString("fieldRequired") %>'
      }
      inputElement.style.borderColor = "#c64b4b";
      return false;
    }
  }
</script>

<jsp:include page="header.jsp"/>
<div class="inhalt">
    <div id="create-box" class="formular">
        <form method="post" id="form_person" novalidate>
            <div id="create-text">
                <h1><%= bundle.getString("createPatientTitle")%>
                </h1>
                <h3 class="header_left"><%=bundle.getString("entryNotesTitle") %>
                </h3>
                <p>
                    <%=bundle.getString("entryNotesText") %>
                </p>
                <ul class="hinweisen_liste">
                    <li>
                  <span class="blauer_text">
                    <%=bundle.getString("entryNotesFirstName") %>
                  </span>
                    </li>
                    <li>
                  <span class="blauer_text">
                    <%=bundle.getString("entryNotesDoubleName") %>
                  </span>
                    </li>
                    <li>
                  <span class="blauer_text">
                    <%=bundle.getString("entryNotesBirthName") %>
                  </span>
                    </li>
                    <li>
                  <span class="blauer_text">
                    <%=bundle.getString("entryNotesRequiredFields") %>
                  </span>
                    </li>
                </ul>
            </div>
            <div id="unsure-case-text" style="display: none;">
                <h1><%=bundle.getString("unsureCase") %></h1>
                <%=bundle.getString("unsureCaseText") %>
                <ul class="hinweisen_liste">
                    <li><span class="blauer_text"><%=bundle.getString("unsureCaseInfoRevise") %></span></li>
                    <li><span class="blauer_text"><%=bundle.getString("unsureCaseInfoConfirm") %></span></li>
                    <li><span class="blauer_text"><%=bundle.getString("unsureCaseInfoSupport") %></span></li>
                </ul>
            </div>
            <jsp:include page="addPatientFormElements.jsp">
                <jsp:param name="showPlaceholders" value="true"/>
            </jsp:include>
            <p id="create-buttons" class="buttons">
                <input class="submit_anlegen" type="button" onclick="createPSD()" value=" <%=bundle.getString("createPatientSubmit") %> "/>
            </p>
            <p id="unsure-case-buttons" class="buttons" style="display: none;">
                <input class="submit_korrigieren" type="button" name="korrigieren" value=" <%=bundle.getString("correct") %> " onclick="showUnsureCase(false)"/>
                <input type="hidden" name="sureness" id="sureness" value="false">
                <input class="submit_bestaetigen" type="button" name="bestaetigen" value=" <%=bundle.getString("confirm") %>" onclick="createPSD()" />
            </p>
        </form>
    </div>
    <div id="loading-box" class="formular" style="display: none;">
        <div style="display: flex;align-items: center;">
            <div class="loading"></div>
            <p style="padding-left: 10px;font-size: 18px;"><%= bundle.getString("loading") %></p>
        </div>
    </div>
    <div id="result-box" class="formular" style="display: none;">
        <h1><%=bundle.getString("result") %>
        </h1>
        <div style="text-align: center;">
            <p>
                <%=bundle.getString("yourRequestedPIDs") %>
            </p>
            <ul style="display: inline-block; text-align: left;">
                <li><span id="result-content-id" class="id"></span></li>
            </ul>
            <p>
                <%=bundle.getString("pleaseCopy") %>
            </p>
            <p>
                <%=bundle.getString("idTypeNote") %>
            </p>
        </div>

        <% if (Boolean.parseBoolean(Config.instance.getProperty("result.printIdat"))) { %>
        <h3><%=bundle.getString("enteredData") %>
        </h3>
        <table class="daten_tabelle" id="daten_tabelle">
            <tbody>
            <tr>
                <td><%=bundle.getString("firstName") %>:</td>
                <td id="vorname-result"></td>
            </tr>
            <tr>
                <td><%=bundle.getString("lastName") %> :</td>
                <td id="nachname-result"></td>
            </tr>
            <tr>
                <td>Geburtsort :</td>
                <td id="geburtsort-result"></td>
            </tr>
            <tr>
                <td><%=bundle.getString("dateOfBirth") %> :</td>
                <td class="geburtsdatum">
                    <div id="geburtsdatumDS"></div>
                </td>
            </tr>
            </tbody>
        </table>
        <% } %>
        <p class="buttons">
            <input type="button" onclick="window.location = window.location.href" value="<%=bundle.getString("back") %>" />
        </p>
    </div>
    <div id="error-box"  class="formular" style="display: none;">
        <h1><%=bundle.getString("errorHasOccured") %></h1>
        <h3 class="header_left"><%=bundle.getString("errorMessage") %>:</h3>
        <p id="error-content">
        </p>
        <p class="buttons">
            <input type="button" onclick="showError('', false)" value="<%=bundle.getString("back") %>" />
        </p>
    </div>
</div>
<jsp:include page="footer.jsp"/>
</body>
</html>
