<!-- Header with logo, if defined -->
<%@page import="de.pseudonymisierung.mainzelliste.Config"%>

    <div class="kopfzeile">
      <% if (Config.instance.getProperty("operator.logo") != null) {%>
      <div class="logo">
      <img src="<%=request.getContextPath() %>/html/logo" align="right"
        height="80%">
      </div>
      <% } %>
    </div>
