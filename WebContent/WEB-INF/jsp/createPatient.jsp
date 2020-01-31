<%@page import="de.pseudonymisierung.mainzelliste.Config"%>
<%@page import="java.util.ResourceBundle"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
  ResourceBundle bundle = Config.instance.getResourceBundle(request);
  // pass "language" parameter from URL if given (included in form URL below)
  String languageInUrl ="";
  if (request.getParameter("language") != null)
    languageInUrl = "&amp;language=" + request.getParameter("language");
%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title><%=bundle.getString("createPatientTitle") %></title>
</head>

<body>
    <jsp:include page="header.jsp"></jsp:include>
    <div class="inhalt">
      <div class="formular">
        <form action="<%=request.getContextPath() %>/patients?mainzellisteApiVersion=${it.mainzellisteApiVersion}&amp;tokenId=${it.tokenId}<%=languageInUrl %>" method="post" id="form_person">
          <h1><%= bundle.getString("createPatientTitle")%></h1>
          <h3 class="header_left"><%=bundle.getString("entryNotesTitle") %></h3>
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


      <jsp:include page="patientFormElements.jsp">
        <jsp:param name="showPlaceholders" value="true"/>
      </jsp:include>

      <p class="buttons">
        <input class="submit_anlegen" type="submit" name="anlegen" value=" <%=bundle.getString("createPatientSubmit") %> "/>
      </p>
      </form>
      </div>
    </div>
    <jsp:include page="footer.jsp" />
  </body>
</html>
