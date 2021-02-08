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

<link rel="stylesheet" type="text/css"
  href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title><%=bundle.getString("editPatientTitle") %></title>
</head>

<body>
  <jsp:include page="header.jsp"></jsp:include>
  <div class="inhalt">
    <div class="formular">
      <form method="post" action="<%=request.getContextPath() %>/patients/tokenId/${it.tokenId}?_method=PUT<%=languageInUrl %>" id="form_person">
        <h1><%=bundle.getString("editPatientTitle") %></h1>
        <jsp:include page="patientFormElements.jsp" />
        <p class="buttons">
          <input type="submit" value="<%=bundle.getString("save") %>" />
        </p>
      </form>
    </div>
  </div>
  <jsp:include page="footer.jsp" />
</body>
</html>
