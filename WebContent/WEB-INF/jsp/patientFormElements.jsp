<!-- Form elements for patient data, to be included in other pages
  The use of placeholders for input fields can be activated by setting
  request parameter "showPlaceholders" to "true".
-->
<%@page import="java.text.DateFormatSymbols"%>
<%@page import="de.pseudonymisierung.mainzelliste.Config"%>
<%@page import="java.util.ResourceBundle"%>
<%@page import="java.util.Calendar"%>
<%@ page import="java.util.Map"%>
<%
  @SuppressWarnings("unchecked")
  Map<String, Object> map = (Map<String, Object>) request
      .getAttribute("it");
  ResourceBundle bundle = Config.instance.getResourceBundle(request);
  Boolean showPlaceholders = Boolean.parseBoolean(request.getParameter("showPlaceholders"));
%>
<h3><%=bundle.getString("patientData")%></h3>
<fieldset class="patienten_daten">
  <table class="daten_tabelle">
    <tbody>
      <tr>
        <td><label for="vorname"><%=bundle.getString("firstName")%>:
        </label></td>
        <td><input type="text" id="vorname" name="vorname" size="50"
          value="${it.vorname}" <% if (map.containsKey("readonly")) { %>
          readonly="readonly" <% } else if (showPlaceholders) { %>
          placeholder="<%=bundle.getString("placeholderFName") %>" <% } %> />
          <span class="mandatory">*</span></td>
      </tr>
      <tr>
        <td><label for="nachname"><%=bundle.getString("lastName")%>
            : </label></td>
        <td><input type="text" id="nachname" name="nachname" size="50"
          value="${it.nachname}" <% if (map.containsKey("readonly")) { %>
          readonly="readonly" <% } else if (showPlaceholders) { %>
          placeholder="<%=bundle.getString("placeholderLName") %>" <% } %>/>
          <span class="mandatory">*</span></td>
      </tr>
      <tr>
        <td><label for="geburtsname"><%=bundle.getString("birthName")%>
            : </label></td>
        <td><input type="text" id="geburtsname" name="geburtsname"
          size="50" value="${it.geburtsname}"
          <% if (map.containsKey("readonly")) { %> readonly="readonly"
          <% } else if (showPlaceholders) { %>
          placeholder="<%=bundle.getString("placeholderBName") %>" <% } %> />
          <span class="mandatory">*</span> <small> (<%=bundle.getString("ifDifferent")%>)
        </small></td>
      </tr>
      <tr>
        <td colspan="2"><small>&nbsp;</small></td>
      </tr>
      <tr>
        <td><label<% if (!map.containsKey("readonly")) { %> for="geburtstag"<% } %>><%=bundle.getString("dateOfBirth")%>
            :</label></td>
        <td class="geburtsdatum" id="geburtsdatum">
          <div>
            <%
              if (map.containsKey("readonly")) {
                if (map != null && map.get("geburtstag") != null) {
                  out.print(String.format("%02d",
                      Integer.parseInt(map.get("geburtstag").toString())));
            %>
            <input type="hidden" name="geburtstag"
              value="<%=map.get("geburtstag")%>" />
            <%
              }
              } else {
            %>
            <select class="geburtstag" name="geburtstag" id="geburtstag">
              <option value="-1"><%=bundle.getString("day")%></option>
              <%
                for (int i = 1; i <= 31; i++) {
              %>
              <option value="<%if (i < 10) {%>0<%}%><%=i%>"
                <%if (map != null
              && map.get("geburtstag") != null
              && i == Integer.parseInt(map.get("geburtstag")
                  .toString())) {%>
                selected="selected" <%}%>>
                <%=String.format("%02d", i)%>
              </option>
              <%
                }
              %>
            </select><span class="mandatory">*</span>
            <%
              }
            %>
            <%
              if (map.containsKey("readonly")) {
                if (map != null && map.get("geburtsmonat") != null) {
                  out.print(String.format("%02d", Integer.parseInt(map.get(
                      "geburtsmonat").toString()))
                      + ".");
            %>
            <input type="hidden" name="geburtsmonat"
              value="<%=map.get("geburtsmonat")%>" />
            <%
              }
              } else {
            %>
            <select class="geburtsmonat" name="geburtsmonat" id="geburtsmonat">
              <option value="-1"><%=bundle.getString("month")%>:
              </option>
              <%
                DateFormatSymbols dfs = DateFormatSymbols.getInstance(bundle
                      .getLocale());
                  String months[] = dfs.getMonths();
                  for (int i = 1; i <= 12; i++) {
              %>
              <option value="<%if (i < 10) {%>0<%}%><%=i%>"
                <%if (map != null
              && map.get("geburtsmonat") != null
              && i == Integer.parseInt(map.get("geburtsmonat")
                  .toString())) {%>
                selected="selected" <%}%>>
                <%=months[i - 1]%>
              </option>
              <%
                }
              %>
            </select><span class="mandatory">*</span>
            <%
              }
            %>

            <%
              if (map.containsKey("readonly")) {
                if (map != null && map.get("geburtsjahr") != null) {
                  out.print(String.format("%02d",
                      Integer.parseInt(map.get("geburtsjahr").toString())));
            %>
            <input type="hidden" name="geburtsjahr"
              value="<%=map.get("geburtsjahr")%>" />
            <%
              }
              } else {
            %>
            <select class="geburtsjahr" name="geburtsjahr" id="geburtsjahr">
              <option value="-1"><%=bundle.getString("year")%>:
              </option>
              <%
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);

                  for (int i = currentYear; i >= currentYear - 130; i--) {
              %>
              <option value="<%=i%>"
                <%if (map != null
              && map.get("geburtsjahr") != null
              && i == Integer.parseInt(map.get("geburtsjahr")
                  .toString())) {%>
                selected="selected" <%}%>>
                <%=String.format("%04d", i)%>
              </option>
              <%
                }
              %>
            </select><span class="mandatory">*</span>
            <%
              }
            %>
          </div>
        </td>
      </tr>
      <tr>
        <td rowspan="2"><label for="plz"><%=bundle.getString("cityOfResidence")%>
            : <br />(<%=bundle.getString("postalCode")%> / <%=bundle.getString("city")%>)
        </label></td>
        <td><input type="text" id="plz" name="plz" size="5"
          maxlength="5" value="${it.plz}"
          <% if (map.containsKey("readonly")) { %> readonly="readonly"
          <% } else if (showPlaceholders) { %> placeholder="#####" <% } %> /> <input type="text"
          id="ort" name="ort" size="40" value="${it.ort}"
          <% if (map.containsKey("readonly")) { %> readonly="readonly"
          <% } else if (showPlaceholders) {%> placeholder="<%=bundle.getString("placeholderCity") %>" <% } %> /></td>
      </tr>
      <tr>
        <td>&nbsp;</td>
      </tr>
    </tbody>
  </table>
</fieldset>
