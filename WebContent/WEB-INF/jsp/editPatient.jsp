<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">

<link rel="stylesheet" type="text/css"
	href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title>Patienten bearbeiten</title>
</head>

<!-- JQuery -->
<script type="text/javascript"
	src="<%=request.getContextPath() %>/static/jslib/jquery/jquery-1.7.2.js"></script>

<script type="text/javascript">

function validateDate()
{
	if ($('#geburtsjahr').val().length != 4)
	{
		return false;
	}
	var geburtstag = parseInt($('#geburtstag').val(), 10);
	var geburtsmonat = parseInt($('#geburtsmonat').val(), 10);
	var geburtsjahr = parseInt($('#geburtsjahr').val(), 10);

	switch (geburtsmonat) {
		case 1:
		case 3:
		case 5:
		case 7:
		case 8:
		case 10:
		case 12:
			if (geburtstag > 31) 
			{
				return false;
			} else {
				return true;
			}
		case 4:
		case 6:
		case 9:
		case 11:
			if (geburtstag > 30) {
				return false; 
			} else {
				return true;
			}
		case 2:
			if (((geburtsjahr % 400 == 0) || (geburtsjahr % 4 == 0 && geburtsjahr % 100 != 0))
					&& geburtstag <= 29) 
				return true;
			else if (geburtstag <= 28) 
				return true; 
			else {
				return false;
			}
		default :
			return false;
	}
	
}
function validateForm()
{
	// define required fields (without date, which is checked separately)
	requiredFields = ['#vorname', '#nachname'];
	for (i = 0; i < requiredFields.length; i++) {
		if ($(requiredFields[i]).val().length == 0) {
			$(requiredFields[i]).focus();
			alert('Bitte füllen Sie alle Pflichtfelder aus!');
			return false;
		}
	}
	
	// Geburtsjahr prüfen
	if (!validateDate())
	{
		alert("Das eingegebene Datum ist ungültig!");
		return false;
	}

	// Prüfen, ob Geburtsname verschieden von Nachnamen ist
	
	if ($('#nachname').val() == $('#geburtsname').val()) {
		alert('Bitte geben Sie den Geburtsnamen nur an, ' +
			'wenn er sich vom aktuellen Nachnamen unterscheidet!');
		return false;		
	}
	
	return true;	
}
</script>

<body>
	<div class="kopfzeile">
		<div class="logo">&nbsp;</div>
	</div>
	<div class="inhalt">
		<div>&nbsp;</div>
		<div class="formular">
			<form method="post" id="form_person"
				onsubmit="return validateForm();">
				<h1>Patienten bearbeiten</h1>
				<%@ include file="patientFormElements.jsp"%>
				<div id ="form_elements_admin">
				<table class="daten_tabelle">
					<tr>
						<td><label for="tentative">Vorläufig</label></td>
						<td><input type="checkbox" id="tentative" name="tentative" 
							<% if (map.get("tentative").equals(true)) {%>
							checked="${it.tentative}" <% } %>
							/></td>
					</tr>
					<tr>
						<td><label for="original">Duplikat von:</label>
						<td><input type="text" id="original" name="original" value="${it.original}"/></td>
					</tr>
				</table>
				
				</div>
			</form>
		</div>
		<div>&nbsp;</div>
	</div>
	<%@include file="footer.jsp" %>
</body>
</html>
