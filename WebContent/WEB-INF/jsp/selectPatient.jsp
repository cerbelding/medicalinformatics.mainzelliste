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
<body>
	<div class="kopfzeile">
		<div class="logo">&nbsp;</div>
	</div>
	<div class="inhalt">
		<div>&nbsp;</div>
		<div class="formular">
			<h1>Patienten bearbeiten</h1>
			<form method="get">
				<fieldset class="patienten_daten">
					<div>&nbsp;</div>
					<div>&nbsp;</div>
					<table class="daten_tabelle">
						<tr>
							<td><label for="id">PID des Patienten:</label>
							</td>
							<td><input type="text" name="id">
							</td>
						</tr>
					</table>
				</fieldset>
				<input type="submit" value="Abschicken">
			</form>
			<div>&nbsp;</div>
		</div>
	</div>
</body>
</html>