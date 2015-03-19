<%@page import="de.pseudonymisierung.mainzelliste.Config"%>
<%@page import="java.util.ResourceBundle"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="ISO-8859-1"%>
    
<% ResourceBundle bundle = Config.instance.getResourceBunde(request); %>
    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title><%=bundle.getString("error") %></title>
</head>

<!-- JQuery -->
<script type="text/javascript" src="<%=request.getContextPath() %>/static/jslib/jquery/jquery-1.7.2.js"></script>

<script type="text/javascript" src="<%=request.getContextPath() %>/static/jslib/validation.js"></script>

<body>
		<jsp:include page="header.jsp"></jsp:include>
		<div class="inhalt">
			<div>&nbsp;</div>
			<div class="formular">
				<h1><%=bundle.getString("errorHasOccured") %></h1>
				<h3 class="header_left"><%=bundle.getString("errorMessage") %>:</h3>
				<p>
					${it.message}
				</p>
				<p>
					<input type="button" onclick="history.back()" value="<%=bundle.getString("back") %>" />
				</p>
				<p>
				  &nbsp;
				</p>
			</div>
		</div>
		<jsp:include page="footer.jsp" />
	</body>
</html>
