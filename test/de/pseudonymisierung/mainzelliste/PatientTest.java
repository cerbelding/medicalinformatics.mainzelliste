package de.pseudonymisierung.mainzelliste;

import org.testng.annotations.*;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;

public class PatientTest {

    @DataProvider(name="patient")
    public Object[][] createPatient() {
        Patient p = new Patient();
        Map<String, Field<?>> pat_input = new HashMap<String, Field<?>>();
        pat_input.put("vorname", new PlainTextField("Marcel"));
        pat_input.put("nachname", new PlainTextField("Parciak"));
        p.setFields(pat_input);
        return new Object[][] {
            { p }
        };
    }

    protected AssociatedIds createAssocId(String extType, String extString, String intType, String intString) {
        AssociatedIds assocId = new AssociatedIds("faelle");

        ExternalID extId = new ExternalID();
        extId.setType(extType);
        extId.setIdString(extString);
        IntegerID intId = new IntegerID();
        intId.setType(intType);
        intId.setIdString(intString);

        assocId.addIdentifier(extId);
        assocId.addIdentifier(intId);

        return assocId;
    }

    @Test(dataProvider = "patient")
    public void baseAssumptions(Patient patient) {
        Assert.assertNotNull(patient.getAssociatedIdentifiers());
        Assert.assertEquals(patient.getAssociatedIdentifiers().size(), 0);
        Assert.assertNull(patient.getAssociatedIdentifier(null));
    }

    @Test(dataProvider = "patient")
    public void addAssociatedIdentifier(Patient patient) {
        AssociatedIds assocId = this.createAssocId("fallnummer", "asdf1234", "fall_psn", "1");
        Assert.assertNotNull(patient.getAssociatedIdentifiers());
        Assert.assertEquals(patient.getAssociatedIdentifiers().size(), 0);

        patient.addAssociatedIdentifier(assocId);
        Assert.assertEquals(patient.getAssociatedIdentifier(assocId), assocId);
        Assert.assertEquals(patient.getAssociatedIdentifiers().size(), 1);
    }

    @Test(dataProvider = "patient")
    public void addAssociatedIdentifiers(Patient patient) {
        AssociatedIds assocId_1 = this.createAssocId("fallnummer", "asdf1234", "fall_psn", "1");
        patient.addAssociatedIdentifier(assocId_1);
        AssociatedIds assocId_2 = this.createAssocId("fallnummer", "fdsa4321", "fall_psn", "2");
        patient.addAssociatedIdentifier(assocId_2);

        Assert.assertEquals(patient.getAssociatedIdentifiers().size(), 2);
        Assert.assertTrue(patient.getAssociatedIdentifiers().contains(assocId_1));
        Assert.assertTrue(patient.getAssociatedIdentifiers().contains(assocId_2));
    }

    @Test(dataProvider = "patient")
    public void getAssociatedIdentifiers(Patient patient) {
        AssociatedIds assocId_1 = this.createAssocId("fallnummer", "asdf1234", "fall_psn", "1");
        patient.addAssociatedIdentifier(assocId_1);
        AssociatedIds assocId_2 = this.createAssocId("fallnummer", "fdsa4321", "fall_psn", "2");
        patient.addAssociatedIdentifier(assocId_2);

        Assert.assertEquals(patient.getAssociatedIdentifier(assocId_1), assocId_1);
        Assert.assertTrue(patient.getAssociatedIdentifiers().contains(assocId_1));

        Assert.assertEquals(patient.getAssociatedIdentifier(assocId_2), assocId_2);
        Assert.assertTrue(patient.getAssociatedIdentifiers().contains(assocId_2));
    }

    @Test(dataProvider = "patient")
    public void removeAssociatedIdentifier(Patient patient) {
        AssociatedIds assocId_1 = this.createAssocId("fallnummer", "asdf1234", "fall_psn", "1");
        patient.addAssociatedIdentifier(assocId_1);
        AssociatedIds assocId_2 = this.createAssocId("fallnummer", "fdsa4321", "fall_psn", "2");
        patient.addAssociatedIdentifier(assocId_2);

        Assert.assertEquals(patient.getAssociatedIdentifiers().size(), 2);
        patient.removeAssociatedIdentifier(assocId_2);

        Assert.assertEquals(patient.getAssociatedIdentifiers().size(), 1);
        Assert.assertNotNull(patient.getAssociatedIdentifier(assocId_1));
        Assert.assertTrue(patient.getAssociatedIdentifiers().contains(assocId_1));
        Assert.assertNull(patient.getAssociatedIdentifier(assocId_2));
        Assert.assertFalse(patient.getAssociatedIdentifiers().contains(assocId_2));
    }

}
