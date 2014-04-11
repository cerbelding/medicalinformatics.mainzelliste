<%@page import="javax.ws.rs.core.MultivaluedMap"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.Map"%>
<%
	Map<String, Object> map = (Map<String, Object>) request
			.getAttribute("it");
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
	<div class="kopfzeile">
		<div class="logo">&nbsp;</div>
	</div>
	<div class="inhalt">
		<div class="formular">
			<div>&nbsp;</div>
			<h1>Ergebnis</h1>

			<p>
				Ihr angeforderter PID lautet
				<tt>
					<big>${it.id}</big>
				</tt>
				. Bitte übernehmen Sie ihn in Ihre Unterlagen.
			</p>

			<% if (map.containsKey("printIdat") && (Boolean) map.get("printIdat")) { %>
			<h3>Eingegebene Daten</h3>
			<p>
			<table class="daten_tabelle">
				<tbody>
					<tr>
						<td>Vorname :</td>
						<td>${it.vorname}</td>
					</tr>
					<tr>
						<td>Nachname :</td>
						<td>${it.nachname}</td>
					</tr>
					<tr>
						<td>Geburtsname :</td>
						<td>${it.geburtsname}</td>
					</tr>
					<tr>
						<td>Geburtsdatum :</td>
						<td class="geburtsdatum">
							<div>
								<%
										out.print(String.format("%02d",
												Integer.parseInt(map.get("geburtstag").toString()))
												+ ". ");
										String months[] = { "Januar", "Februar", "März", "April", "Mai",
												"Juni", "Juli", "August", "September", "Oktober",
												"November", "Dezember" };
										out.print(months[Integer.parseInt(map.get("geburtsmonat")
												.toString()) - 1] + " ");
										out.print(String.format("%02d",
												Integer.parseInt(map.get("geburtsjahr").toString())));
									%>
							</div>
						</td>
					</tr>
					<tr>
						<td>PLZ / Wohnort :</td>
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
			<h3>Ähnlichster Eintrag:</h3>
			<table>
				<%
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
					<td>Matchgewicht:</td>
					<td><%=map.get("weight")%></td>
				</tr>
			</table>
			<%
	}
%>
			<%-- 	<% --%>
			<!-- 		Map<String, Object> map = (Map<String,Object>)request.getAttribute("it"); -->
			<!-- 		boolean tentative = ((Boolean) map.get("tentative")); -->
			<!-- 		if (tentative) -->
			<!-- 		{ -->
			<!-- 	%> -->
			<!-- 		<p> -->
			<!-- 			Zu den eingegebenen Daten wurde ein ähnlicher Patient gefunden, der nicht -->
			<!-- 			mit hinreichender Sicherheit zugeordnet werden kann. Der angezeigte PID -->
			<!-- 			ist als vorläufig zu betrachten. Das bedeutet, dass der PID zwar verwendet  -->
			<!-- 			werden kann, aber zukünftige Abfragen mit den gleichen Daten können einen  -->
			<!-- 			anderen	PID liefern. -->
			<!-- 		<p> -->
			<!-- 		<div> -->
			<!-- 			<form> -->
			<!-- 				<input type="button" value="Fenster schließen" onClick="window.close()"> -->
			<!-- 			</form> -->
			<!-- 		</div> -->
			<%-- 	<% --%>
			<!-- 		} -->
			<!-- 	%> -->
			<div>&nbsp;</div>
		</div>
	</div>
	<%@ include file="footer.jsp"%>
</body>
</html>