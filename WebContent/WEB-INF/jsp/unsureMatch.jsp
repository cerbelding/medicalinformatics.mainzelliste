<%@page import="java.util.ResourceBundle"%>
<%@page import="de.pseudonymisierung.mainzelliste.Config"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
  ResourceBundle bundle = Config.instance.getResourceBundle(request);
  // pass "language" parameter from URL if given (included in form URL below)
  String languageInUrl ="";
  if (request.getParameter("language") != null)
    languageInUrl = "&amp;language=" + request.getParameter("language");
%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title><%=bundle.getString("unsureCase") %></title>
</head>


<body>
    <jsp:include page="header.jsp"></jsp:include>
    <div class="inhalt">
      <div class="formular">
        <form action="<%=request.getContextPath() %>/patients?mainzellisteApiVersion=${it.mainzellisteApiVersion}&amp;tokenId=${it.tokenId}<%=languageInUrl %>" method="post" id="form_person">
        <h1><%=bundle.getString("unsureCase") %></h1>
        <%=bundle.getString("unsureCaseText") %>
        <ul class="hinweisen_liste">
          <li><span class="blauer_text"><%=bundle.getString("unsureCaseInfoRevise") %></span></li>
          <li><span class="blauer_text"><%=bundle.getString("unsureCaseInfoConfirm") %></span></li>
          <li><span class="blauer_text"><%=bundle.getString("unsureCaseInfoSupport") %></span></li>
        </ul>

        <jsp:include page="patientFormElements.jsp" />
      <p class="buttons">
        <input class="submit_korrigieren" type="button" name="korrigieren" value=" <%=bundle.getString("correct") %> " onclick="history.back();"/>
        <input type="hidden" name="sureness" value="true">
        <input class="submit_bestaetigen" type="submit" name="bestaetigen" value=" <%=bundle.getString("confirm") %>" />
      </p>
        </form>
      </div>
    </div>
    <jsp:include page="footer.jsp" />
  </body>
</html>
