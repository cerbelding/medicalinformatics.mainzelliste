<%@page import="de.pseudonymisierung.mainzelliste.Config"%>
<%@page import="java.util.ResourceBundle"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%	ResourceBundle bundle = Config.instance.getResourceBundle(request); %>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title><%=bundle.getString("error") %></title>
</head>

<body>
    <jsp:include page="header.jsp"></jsp:include>
    <div class="inhalt">
      <div class="formular">
        <h1><%=bundle.getString("errorHasOccured") %></h1>
        <h3 class="header_left"><%=bundle.getString("errorMessage") %>:</h3>
        <p>
          ${it.message}
        </p>
        <p>
          <input type="button" onclick="history.back()" value="<%=bundle.getString("back") %>" />
        </p>
      </div>
    </div>
    <jsp:include page="footer.jsp" />
  </body>
</html>
