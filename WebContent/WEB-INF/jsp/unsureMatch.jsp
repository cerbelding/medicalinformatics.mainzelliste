<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title>Unsicherer Fall</title>
</head>

<!-- JQuery -->
<script type="text/javascript" src="<%=request.getContextPath() %>/static/jslib/jquery/jquery-1.7.2.js"></script>
<!-- Validierungsfunktionen -->
<script type="text/javascript" src="<%=request.getContextPath() %>/static/jslib/validation.js"></script>


<body>
		<div class="kopfzeile">
			<div class="logo">&nbsp;</div>
		</div>
		<div class="inhalt">
			<div>&nbsp;</div>
			<div class="formular">
				<form action="<%=request.getContextPath() %>/patients?tokenId=${it.tokenId}&callback=${it.callback}" method="post" id="form_person"
					onsubmit="return validateForm();">
				<h1>Unsicherer Fall</h1>
				<p>Zu den eingegeben Daten wurde ein �hnlicher Patient gefunden,
					der aber nicht mit hinreichender Sicherheit zugeordnet werden kann.</p>
				<p>Um eine Verwechslung auszuschlie�en, �berpr�fen Sie bitte nochmals Ihre 
					Eingabe! Dann:</p>
				<ul class="blauer_text">
					<li>Falls Sie einen Fehler festgestellt haben, w�hlen Sie "Korrigieren". 
						Sie bekommen dann das Eingabeformular mit den eingegebenen Daten zur 
						Korrektur erneut angezeigt.</li>
					<li>Falls Sie sicher sind, dass die von Ihnen eingegebenen Daten
						stimmen, w�hlen Sie "Best�tigen". Es wird dann ein neuer Patient
						mit diesen Daten angelegt.</li>
					<li>Falls Sie sicher sind, dass die eingegebenen Daten stimmen, aber
						der Patient schon fr�her einmal eingegeben wurde, melden Sie sich 
						bitte beim Administrator der Patientenliste (siehe Fu�zeile).</li>
				</ul>
				<p>&nbsp;</p>

				<%@ include file="patientFormElements.jsp" %>
			<div align="center">
				<td>&nbsp;</td>
			</div>
			<div align="center">
				<input class="submit_korrigieren" type="button" name="korrigieren" value=" Korrigieren " onclick="history.back();"/>
				<input type="hidden" name="sureness" value="true">
				<input class="submit_bestaetigen" type="submit" name="bestaetigen" value=" Best�tigen "/>
			</div>
			<div align="center">
				<td>&nbsp;</td>
			</div>
				</form>
			</div>
			<div align="center">&nbsp;</div>
		</div>
		<%@ include file="footer.jsp" %>
	</body>
</html>
