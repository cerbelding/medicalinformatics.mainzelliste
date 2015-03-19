<%@page import="java.text.DateFormatSymbols"%>
<%@page import="de.pseudonymisierung.mainzelliste.Config"%>
<%@page import="java.util.ResourceBundle"%>
<%@page import="javax.ws.rs.core.MultivaluedMap"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.Set"%>
<%@ page import="de.pseudonymisierung.mainzelliste.ID"%>
<%
	@SuppressWarnings("unchecked")
	Map<String, Object> map = (Map<String, Object>) request
			.getAttribute("it");
	Set<ID> ids = (Set<ID>) map.get("ids");
	ResourceBundle bundle = Config.instance.getResourceBunde(request);
	DateFormatSymbols dfs = DateFormatSymbols.getInstance(bundle
			.getLocale());
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<link rel="stylesheet" type="text/css"
	href="<%=request.getContextPath()%>/static/css/patientenliste.css">

<title>Ergebnis</title>
</head>

<body>
	<jsp:include page="header.jsp"></jsp:include>
	<div class="inhalt">
		<div class="formular">
			<div>&nbsp;</div>
			<h1>Ergebnis</h1>
                        <div align="center">
                            <p>
								<%=bundle.getString("yourRequestedPIDs") %>
                            </p>
							<ul style="display: inline-block; text-align: left;">
							<% for (ID id : ids) { 
							if (id != null) {
							%>
								<li><tt><big><%=id.getType() %>: <%=id.getIdString() %></big></tt></li>                                  	
							<% 
								}
							}
							%>
							</ul>
                            <p>
                            	<%=bundle.getString("pleaseCopy") %> 
                            </p>
                            <p>
                            	<%=bundle.getString("idTypeNote") %>
                            </p>
                        </div>
                                        
			<% if (map.containsKey("printIdat") && (Boolean) map.get("printIdat")) { %>
			<h3><%=bundle.getString("enteredData") %></h3>
			<p>
			<table class="daten_tabelle">
				<tbody>
					<tr>
						<td><%=bundle.getString("firstName") %>:</td>
						<td>${it.vorname}</td>
					</tr>
					<tr>
						<td><%=bundle.getString("lastName") %> :</td>
						<td>${it.nachname}</td>
					</tr>
					<tr>
						<td><%=bundle.getString("birthName") %> :</td>
						<td>${it.geburtsname}</td>
					</tr>
					<tr>
						<td><%=bundle.getString("dateOfBirth") %> :</td>
						<td class="geburtsdatum">
							<div>
								<%
										out.print(String.format("%02d",
												Integer.parseInt(map.get("geburtstag").toString()))
												+ ". ");
										String months[] = dfs.getMonths();
										out.print(months[Integer.parseInt(map.get("geburtsmonat")
												.toString()) - 1] + " ");
										out.print(String.format("%02d",
												Integer.parseInt(map.get("geburtsjahr").toString())));
									%>
							</div>
						</td>
					</tr>
					<tr>
						<td><%=bundle.getString("cityOfResidence") %> :</td>
						<td>${it.plz} ${it.ort}</td>
					</tr>
				</tbody>
			</table>
			</p>
			<% } %>
			<%
				//Map<String, Object> map = (Map<String,Object>)request.getAttribute("it");
				if (map.containsKey("redirect")) {
			%>
			<p>
			<form action="<%=map.get("redirect")%>" target="_top" method="get">
				<%  
				@SuppressWarnings("unchecked")
				MultivaluedMap<String, String> redirectParams = (MultivaluedMap<String, String>) map.get("redirectParams"); 
				for (String key : redirectParams.keySet()) {
					String value = redirectParams.getFirst(key);
					%>
				<input type="hidden" name="<%=key %>" value="<%=value %>" />
				<% 
				}
				%>
				<div style="text-align: center">
					<% if (map.containsKey("printIdat") && (Boolean) map.get("printIdat")) { %>
						<input type="submit" onclick="window.print();"
						value="Drucken und Patient anlegen" />
					<% } else { %>
						<input type="submit" value="Patient anlegen" />
					<% } %>
				</div>
			</form>
			</p>
			<%
	}
%>
			<%
	if (map.containsKey("debug")) {
%>
			<h3><%=bundle.getString("mostSimilar") %></h3>
			<table>
				<%
		@SuppressWarnings("unchecked")
		Map<String, String> fields = (Map<String, String>) map
					.get("bestMatch");
			for (String key : fields.keySet()) {
	%>
				<tr>
					<td><%=key%></td>
					<td><%=fields.get(key)%></td>
				</tr>
				<%
			}
		%>
				<tr>
					<td><%=bundle.getString("matchingWeight") %>:</td>
					<td><%=map.get("weight")%></td>
				</tr>
			</table>
			<%
	}
%>
			<div>&nbsp;</div>
		</div>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>