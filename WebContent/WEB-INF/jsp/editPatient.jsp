<%@page import="de.pseudonymisierung.mainzelliste.Config"%>
<%@page import="java.util.ResourceBundle"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="ISO-8859-1"%>

<%
	ResourceBundle bundle = Config.instance.getResourceBunde(request);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<link rel="stylesheet" type="text/css"
	href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title><%=bundle.getString("editPatientTitle") %></title>
</head>

<body>
	<jsp:include page="header.jsp"></jsp:include>
	<div class="inhalt">
		<div>&nbsp;</div>
		<div class="formular">
			<form method="post" action="<%=request.getContextPath() %>/patients/tokenId/${it.tokenId}?_method=PUT" id="form_person">
				<h1><%=bundle.getString("editPatientTitle") %></h1>
				<jsp:include page="patientFormElements.jsp" />
				<div align="center">
					<td>&nbsp;</td>
				</div>
				<div align="center">
					<input type="submit" value="<%=bundle.getString("save") %>" />
				</div>
			</form>
		</div>
		<div>&nbsp;</div>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
