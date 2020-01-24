<%@page import="de.pseudonymisierung.mainzelliste.IDGeneratorFactory"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
  pageEncoding="UTF-8"%>
<%
  String idTypes[] = IDGeneratorFactory.instance.getIDTypes();
  String defaultIdType = IDGeneratorFactory.instance.getDefaultIDType();
%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css"
  href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title>Patienten bearbeiten</title>
</head>
<body>
  <jsp:include page="header.jsp"></jsp:include>
  <div class="inhalt">
    <div class="formular">
      <h1>Patienten bearbeiten</h1>
      <form method="get">
        <fieldset class="patienten_daten">
          <table class="daten_tabelle">
            <tr>
              <td><label for="idType">ID-Typ:</label>
              </td>
              <td>
                <select name="idType" id="idType">
                  <%
                  for (String idType : idTypes)
                  {
                  %>
                    <option value="<%=idType %>"><%=idType %></option>
                  <%
                  }
                  %>
                </select>
              </td>
            </tr>
            <tr>
              <td><label for="idString">ID-Wert:</label>
              </td>
              <td><input type="text" name="idString" id="idString">
              </td>
            </tr>
          </table>
        </fieldset>
        <p class="buttons">
          <input type="submit" value="Abschicken">
        </p>
      </form>
    </div>
  </div>
</body>
</html>
