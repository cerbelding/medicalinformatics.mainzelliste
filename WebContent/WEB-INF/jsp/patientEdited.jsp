<%@page import="de.pseudonymisierung.mainzelliste.Config"%>
<%@page import="java.util.ResourceBundle"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%
	ResourceBundle bundle = Config.instance.getResourceBunde(request);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<link rel="stylesheet" type="text/css"
	href="<%=request.getContextPath()%>/static/css/patientenliste.css">

<title>Bearbeitung abgeschlosen</title>
</head>

<body>
	<jsp:include page="header.jsp"></jsp:include>
	<div class="inhalt">
		<div class="formular">
			<div>&nbsp;</div>
			<h1><%=bundle.getString("editCompletedTitle") %></h1>
			<p align="center">
				<%=bundle.getString("editCompletedText") %>								
			</p>
		</div>
		<div>&nbsp;</div>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>