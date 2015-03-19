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
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title><%=bundle.getString("createPatientTitle") %></title>
</head>

<body>
		<jsp:include page="header.jsp"></jsp:include>
		<div class="inhalt">
			<div>&nbsp;</div>
			<div class="formular">
				<form action="<%=request.getContextPath() %>/patients?mainzellisteApiVersion=${it.mainzellisteApiVersion}&tokenId=${it.tokenId}" method="post" id="form_person">
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
					<div>&nbsp;</div>
					<p></p>


			<jsp:include page="patientFormElements.jsp">
				<jsp:param name="showPlaceholders" value="true"/>
			</jsp:include>
			
			<div align="center">
				<td>&nbsp;</td>
			</div>
			<div align="center">
				<input class="submit_anlegen" type="submit" name="anlegen" value=" <%=bundle.getString("createPatientSubmit") %> "/>
			</div>
			<div align="center">
				<td>&nbsp;</td>
			</div>
			</form>
			</div>
		</div>
		<jsp:include page="footer.jsp" />
	</body>
</html>
