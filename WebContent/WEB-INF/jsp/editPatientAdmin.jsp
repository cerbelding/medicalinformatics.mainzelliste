<%@page import="javax.ws.rs.core.Response.Status"%>
<%@page import="javax.ws.rs.core.Response"%>
<%@page import="javax.ws.rs.WebApplicationException"%>
<%@page import="org.codehaus.jettison.json.JSONException"%>
<%@page import="java.util.Map"%>
<%@page import="org.codehaus.jettison.json.JSONObject"%>
<%@page import="de.pseudonymisierung.mainzelliste.ID"%>
<%@page import="de.pseudonymisierung.mainzelliste.Patient"%>
<%@page import="de.pseudonymisierung.mainzelliste.IDGeneratorFactory"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="ISO-8859-1"%>
<%
	String idTypes[] = IDGeneratorFactory.instance.getIDTypes();
	String defaultIdType = IDGeneratorFactory.instance.getDefaultIDType();
	JSONObject originalIds;
	@SuppressWarnings("unchecked")
	Map<String, Object> map = (Map<String, Object>) request
		.getAttribute("it");
	
	Patient original = (Patient) map.get("original");
 	try {
 		originalIds = new JSONObject();
 		if (original != null) {
 			for (ID thisId : original.getIds())
 				originalIds.put(thisId.getType(), thisId.getIdString());
 		}
 	} catch (JSONException e) {
 		throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
 				.entity("An internal error has occured: JSONException while collecting IDs. " + e.getMessage())
 				.build());
 	}
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<link rel="stylesheet" type="text/css"
	href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title>Patienten bearbeiten</title>
</head>


<script type="text/javascript">

var originalIds = <%=originalIds.toString() %>;

function fillOriginalId() {
	var idType = document.getElementById("idTypeOriginal").value;
	var idString = originalIds[idType];
	if (idString === undefined)
		idString = "";
	
	document.getElementById("idStringOriginal").value = idString;
}

</script>

<body>
	<jsp:include page="header.jsp"></jsp:include>
	<div class="inhalt">
		<div>&nbsp;</div>
		<div class="formular">
			<form method="post" id="form_person">
				<h1>Patienten bearbeiten</h1>
				<jsp:include page="patientFormElements.jsp"></jsp:include>
				<div id ="form_elements_admin">
				<table class="daten_tabelle">
					<tr>
						<td><label for="tentative">Vorl�ufig</label></td>
						<td><input type="checkbox" id="tentative" name="tentative"
							<% if (map.get("tentative").equals(true)) {%>
							checked="${it.tentative}" <% } %>
							/></td>
					</tr>
					<tr>
						<td rowspan="2"><label for="original">Duplikat von:</label></td>
						<td><label for="idTypeOriginal">ID-Typ:</label></td>
						<td>
							<select name="idTypeOriginal" id="idTypeOriginal" onchange="fillOriginalId();s">
								<%
								for (String idType : idTypes)
								{
									String selected = idType.equals(defaultIdType) ?
											"selected=\"selected\"" : "";
								%>
									<option value="<%=idType %>" <%=selected %>>
										<%=idType %>
									</option>
								<%
								}
								%>
							</select>
						</td>
					</tr>
					<tr>
						<td><label for="idString">ID-Wert:</label>
						</td>
						<td><input type="text" name="idStringOriginal" id="idStringOriginal"
							value="<%= originalIds.has(defaultIdType) ? originalIds.get(defaultIdType) : "" %>">
						</td>
					</tr>
				</table>
				</div>
				<div align="center">
					&nbsp;
				</div>
				<div align="center">
					<input type="submit" value="Speichern">
				</div>				
			</form>
			<div align="center">
				&nbsp;
			</div>
			<form method="POST" onsubmit="return confirm('Patienten wirklich l�schen?');">
				<div align="center">
					<input type="submit" value="L�schen" name="delete"/>
				</div>
			</form>
		</div>
		<div>&nbsp;</div>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>