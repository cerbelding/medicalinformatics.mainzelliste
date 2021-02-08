<%@page import="de.pseudonymisierung.mainzelliste.Config"%>
<%@page import="java.util.ResourceBundle"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
  pageEncoding="UTF-8"%>
<%
  ResourceBundle bundle = Config.instance.getResourceBundle(request);
%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css"
  href="<%=request.getContextPath()%>/static/css/patientenliste.css">

<title>Bearbeitung abgeschlosen</title>
</head>

<body>
  <jsp:include page="header.jsp"></jsp:include>
  <div class="inhalt">
    <div class="formular">
      <h1><%=bundle.getString("editCompletedTitle") %></h1>
      <p style="text-align: center;">
        <%=bundle.getString("editCompletedText") %>
      </p>
    </div>
  </div>
  <jsp:include page="footer.jsp" />
</body>
</html>
