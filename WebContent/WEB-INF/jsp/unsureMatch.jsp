<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title>Unsicherer Fall</title>
</head>


<body>
		<div class="kopfzeile">
			<div class="logo">&nbsp;</div>
		</div>
		<div class="inhalt">
			<div>&nbsp;</div>
			<div class="formular">
				<form action="<%=request.getContextPath() %>/patients?mainzellisteApiVersion=${it.mainzellisteApiVersion}&tokenId=${it.tokenId}&callback=${it.callback}" method="post" id="form_person"
					onsubmit="return validateForm();">
				<h1>Unsicherer Fall</h1>
				<p>Zu den eingegeben Daten wurde ein ähnlicher Patient gefunden,
					der aber nicht mit hinreichender Sicherheit zugeordnet werden kann.</p>
				<p>Um eine Verwechslung auszuschließen, überprüfen Sie bitte nochmals Ihre 
					Eingabe! Dann:</p>
				<ul class="blauer_text">
					<li>Falls Sie einen Fehler festgestellt haben, wählen Sie "Korrigieren". 
						Sie bekommen dann das Eingabeformular mit den eingegebenen Daten zur 
						Korrektur erneut angezeigt.</li>
					<li>Falls Sie sicher sind, dass die von Ihnen eingegebenen Daten
						stimmen, wählen Sie "Bestätigen". Es wird dann ein neuer Patient
						mit diesen Daten angelegt.</li>
					<li>Falls Sie sicher sind, dass die eingegebenen Daten stimmen, aber
						der Patient schon früher einmal eingegeben wurde, melden Sie sich 
						bitte beim Administrator der Patientenliste (siehe Fußzeile).</li>
				</ul>
				<p>&nbsp;</p>

				<%@ include file="patientFormElements.jsp" %>
			<div align="center">
				<td>&nbsp;</td>
			</div>
			<div align="center">
				<input class="submit_korrigieren" type="button" name="korrigieren" value=" Korrigieren " onclick="history.back();"/>
				<input type="hidden" name="sureness" value="true">
				<input class="submit_bestaetigen" type="submit" name="bestaetigen" value=" Bestätigen "/>
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
