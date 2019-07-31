<%@page import="java.util.List"%>
<%@page import="de.pseudonymisierung.mainzelliste.dto.Persistor"%>
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
  pageEncoding="UTF-8"%>
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
 	List<String> duplicates = (List<String>) map.get("duplicates");
 	List<String> possibleDuplicates = (List<String>) map.get("possibleDuplicates");
%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css"
  href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title>Patienten bearbeiten</title>

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
</head>

<body>
  <jsp:include page="header.jsp"></jsp:include>
  <div class="inhalt">
    <div>&nbsp;</div>
    <div class="formular">
      <form method="post" id="form_person">
        <h1>Patienten bearbeiten</h1>
        <jsp:include page="patientFormElements.jsp"></jsp:include>
        <div id ="form_elements_admin">
		<fieldset class="patienten_daten">
        <table class="daten_tabelle">
          <tr>
            <td><label for="tentative">Vorläufig</label></td>
            <td colspan="2"><input type="checkbox" id="tentative" name="tentative"
              <% if (map.get("tentative").equals(true)) {%>
              checked="${it.tentative}" <% } %>
              /></td>
          </tr>
          <tr>
            <td rowspan="2">Duplikat von:</td>
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
            <td><label for="idStringOriginal">ID-Wert:</label>
            </td>
            <td><input type="text" name="idStringOriginal" id="idStringOriginal"
              value="<%= originalIds.has(defaultIdType) ? originalIds.get(defaultIdType) : "" %>">
            </td>
          </tr>
        </table>
        </fieldset>
        <fieldset class="patienten_daten">
        <table class="daten_tabelle">
        	<tr>
        		<td>Duplikate:</td>
        		<td>
					<ul>
						<% for (String id : duplicates) { %>
						<li>
							<a href="<%=request.getContextPath()%>/html/admin/editPatient?idType=<%=defaultIdType %>&amp;idString=<%=id %>">
								<%=id %>
							</a>
						<% } %>
					</ul>
        		</td>
        	</tr>
        	<tr>
        		<td>Mögliche Duplikate:</td>
        		<td>
					<ul>
						<% for (String id : possibleDuplicates) { %>
						<li>
							<a href="<%=request.getContextPath()%>/html/admin/editPatient?idType=<%=defaultIdType %>&amp;idString=<%=id %>">
								<%=id %>
							</a>
						<% } %>
					</ul>
        		</td>
        	</tr>
        </table>
        </fieldset>	
        </div>
        <p class="buttons">
          <input type="submit" value="Speichern">
        </p>
      </form>
      <form method="POST" onsubmit="return confirm('Patienten wirklich löschen?');">
        <p class="buttons">
          <input type="submit" value="Löschen" name="delete"/>
        </p>
      </form>
    </div>
    <div>&nbsp;</div>
  </div>
  <jsp:include page="footer.jsp" />
</body>
</html>
