<%@page import="java.util.ResourceBundle"%>
<%@page import="de.pseudonymisierung.mainzelliste.Config"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="ISO-8859-1"%>
<%
	ResourceBundle bundle = Config.instance.getResourceBunde(request);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title><%=bundle.getString("unsureCase") %></title>
</head>


<body>
		<jsp:include page="header.jsp"></jsp:include>
		<div class="inhalt">
			<div>&nbsp;</div>
			<div class="formular">
				<form action="<%=request.getContextPath() %>/patients?tokenId=${it.tokenId}&callback=${it.callback}" method="post" id="form_person">
				<h1><%=bundle.getString("unsureCase") %></h1>
				<%=bundle.getString("unsureCaseText") %>
				<ul class="hinweisen_liste">
					<li><span class="blauer_text"><%=bundle.getString("unsureCaseInfoRevise") %></span></li>
					<li><span class="blauer_text"><%=bundle.getString("unsureCaseInfoConfirm") %></span></li>
					<li><span class="blauer_text"><%=bundle.getString("unsureCaseInfoSupport") %></span></li>
				</ul>
				<p>&nbsp;</p>

				<jsp:include page="patientFormElements.jsp" />
			<div align="center">
				&nbsp;
			</div>
			<div align="center">
				<input class="submit_korrigieren" type="button" name="korrigieren" value=" <%=bundle.getString("correct") %> " onclick="history.back();"/>
				<input type="hidden" name="sureness" value="true">
				<input class="submit_bestaetigen" type="submit" name="bestaetigen" value=" <%=bundle.getString("confirm") %>" />
			</div>
			<div align="center">
				&nbsp;
			</div>
				</form>
			</div>
			<div align="center">&nbsp;</div>
		</div>
		<jsp:include page="footer.jsp" />
	</body>
</html>
