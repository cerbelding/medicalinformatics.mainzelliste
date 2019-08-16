<%--
    Document   : viewAuditTrail
    Created on : 27.11.2017, 12:07:25
    Author     : lemmer
--%>
<%@page import="java.util.List"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@page import="de.pseudonymisierung.mainzelliste.AuditTrail"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.text.DateFormatSymbols"%>
<%@page import="de.pseudonymisierung.mainzelliste.Config"%>
<%@page import="java.util.ResourceBundle"%>
<%@page import="java.util.Calendar"%>
<%
    @SuppressWarnings(  "unchecked")
    ResourceBundle bundle = Config.instance.getResourceBundle(request);
    Boolean showPlaceholders = Boolean.parseBoolean(request.getParameter("showPlaceholders"));
%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" type="text/css"
          href="<%=request.getContextPath()%>/static/css/patientenliste.css">
    <title>JSP Page</title>
</head>
<body>
<h1>Audit Trail</h1>
<h3 style="text-transform: uppercase;"><%= request.getParameter("idType") %>: <%= request.getParameter("idString") %></h3>
<!--        <div class="audit_container">-->
<fieldset class="patienten_daten">
    <table class="tabelle_audit">
        <tr>
            <th><%=bundle.getString("timestamp")%></th>
            <th><%=bundle.getString("idString")%></th>
            <th><%=bundle.getString("idType")%></th>
            <th><%=bundle.getString("remoteUser")%></th>
            <th><%=bundle.getString("remoteSystem")%></th>
            <th><%=bundle.getString("remoteIP")%></th>
            <th><%=bundle.getString("typeOfChange")%></th>
            <th><%=bundle.getString("reasonForChange")%></th>
            <th><%=bundle.getString("oldValue")%></th>
            <th><%=bundle.getString("newValue")%></th>
        </tr>
        <c:forEach items="${it}" var="item">
            <tr>
                <td><fmt:formatDate value="${item.timestamp}" type="both" dateStyle = "long" timeStyle = "long" /></td>
                <td><c:out value="${item.idValue}" /></td>
                <td><c:out value="${item.idType}" /></td>
                <td><c:out value="${item.username}" /></td>
                <td><c:out value="${item.remoteSystem}" /></td>
                <td><c:out value="${item.remoteIp}" /></td>
                <td><c:out value="${item.typeOfChange}" /></td>
                <td><c:out value="${item.reasonForChange}" /></td>
                <td class="json_content"><c:out value="${item.oldValue}"/></td>
                <td class="json_content"><c:out value="${item.newValue}" /></td>
            </tr>
        </c:forEach>
    </table>
</fieldset>
<!--        </div>-->
</body>
</html>