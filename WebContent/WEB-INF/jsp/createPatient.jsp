<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/static/css/patientenliste.css">

<title>PID anfordern</title>
</head>

<body>
		<div class="kopfzeile">
			<div class="logo">
			<img src="<%=request.getContextPath() %>/static/media/JGU_Uni_medizin_Logo_4c_Internet.jpg" align="right"
				height="80%">
			</div>
		</div>
		<div class="inhalt">
			<div>&nbsp;</div>
			<div class="formular">
				<form action="<%=request.getContextPath() %>/patients?tokenId=${it.tokenId}&callback=${it.callback}" method="post" id="form_person"
					onsubmit="return validateForm();">
					<h1>PID anfordern</h1>
					<h3 class="header_left">Hinweise zur Eingabe</h3>
					<p>
						Diese Anwendung gibt für die von Ihnen im Folgenden einzugebenden Stammdaten einen Personenidentifikator (PID) zurück. 
						Dabei wird der bekannte Patientenbestand durchsucht; bei einem Treffer wird der bestehende PID zurückgegeben. 
						Bitte beachten Sie bei Ihrer Eingabe die folgenden Punkte:
					</p>
					<ul class="hinweisen_liste">
						<li>
							<span class="blauer_text">
								Geben Sie alle Ihnen bekannten Vornamen an, getrennt durch Leerzeichen. 
							</span>
						</li>
						<li>
							<span class="blauer_text">
								Achten Sie bei Doppelnamen darauf, ob sie mit Bindestrich oder zusammen geschrieben werden (z.B. &quot;Annalena&quot; oder &quot;Anna-Lena&quot;).
							</span>
						</li>
						<li>
							<span class="blauer_text">
								Geben Sie den Geburtsnamen nur an, falls er vom aktuellen Nachnamen abweicht (z.B. bei Namenswechsel durch Heirat).
							</span>
						</li>
						<li>
							<span class="blauer_text">
								Die mit <font color="red">*</font> markierten Felder sind Pflichtfelder.
							</span>
						</li>
					</ul>
					<div>&nbsp;</div>
					<p></p>


			<%@ include file="patientFormElements.jsp" %>
			<div align="center">
				<td>&nbsp;</td>
			</div>
			<div align="center">
				<input class="submit_anlegen" type="submit" name="anlegen" value=" PID anfordern "/>
			</div>
			<div align="center">
				<td>&nbsp;</td>
			</div>
			</form>
			</div>
		</div>
		<%@ include file="footer.jsp" %>
	</body>
</html>
