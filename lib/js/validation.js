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
	for (var i = 0; i < requiredFields.length; i++) {
		if ($(requiredFields[i]).val().length == 0) {
			$(requiredFields[i]).focus();
			alert('Bitte f�llen Sie alle Pflichtfelder aus!');
			return false;
		}
	}
	
	// Geburtsjahr pr�fen
	if (!validateDate())
	{
		alert("Das eingegebene Datum ist ung�ltig!");
		return false;
	}

	// Pr�fen, ob Geburtsname verschieden von Nachnamen ist
	
	if ($('#nachname').val() == $('#geburtsname').val()) {
		alert('Bitte geben Sie den Geburtsnamen nur an, ' +
			'wenn er sich vom aktuellen Nachnamen unterscheidet!');
		return false;		
	}
	
	return true;	
}
