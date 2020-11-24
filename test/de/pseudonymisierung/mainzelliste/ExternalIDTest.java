package de.pseudonymisierung.mainzelliste;

import java.util.Set;

import org.testng.annotations.*;

import org.testng.Assert;

public class ExternalIDTest {

    protected ExternalID createExtId(String idType, String idString) {
        ExternalID extId = new ExternalID();
        extId.setType(idType);
        extId.setIdString(idString);
        return extId;
    }

    protected IntegerID createIntId(String idType, String idString) {
        IntegerID intId = new IntegerID();
        intId.setType(idType);
        intId.setIdString(idString);
        return intId;
    }

    @DataProvider(name="extId")
    protected Object[][] createExternalID() {
        return new Object[][] {
            { this.createExtId("fallnummer", "abcd1234") },
            { this.createExtId("fallnummer", "1234") },
            { this.createExtId("fallnummer", "🍌") }
        };
    }


    // Integration Test, not possible here
    //@Test
    public void testCreateIdentifier() {
        ExternalID visit = new ExternalID();
        visit.setType("fallnummer");
        ID newId = visit.createIdentifier("fallnummer_psn");

        Assert.assertTrue(visit.getIdentifiers().contains(newId));
    }

    @Test(dataProvider = "extId")
    public void testGetIdentifier(ExternalID extId) {
        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        extId.addIdentifier(intId);

        ID returnedId = extId.getIdentifier("fallnummer_psn");
        Assert.assertEquals(returnedId, intId);
    }

    @Test(dataProvider = "extId")
    public void testAddIdentifier(ExternalID extId) {
        Assert.assertNull(extId.getIdentifier("fallnummer_psn"));
        Assert.assertNull(extId.getIdentifier("fallnummer_psn_2"));

        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        extId.addIdentifier(intId);
        IntegerID intId_2 = this.createIntId("fallnummer_psn_2", "654321");
        extId.addIdentifier(intId_2);

        Assert.assertNotNull(extId.getIdentifier("fallnummer_psn"));
        Assert.assertTrue(extId.getIdentifiers().contains(intId));
        Assert.assertNotNull(extId.getIdentifier("fallnummer_psn_2"));
        Assert.assertTrue(extId.getIdentifiers().contains(intId_2));
    }

    @Test(dataProvider = "extId")
    public void testGetIdentifiers(ExternalID extId) {
        Set<ID> emptyList = extId.getIdentifiers();
        Assert.assertNotNull(emptyList);
        Assert.assertEquals(emptyList.size(), 0);

        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        extId.addIdentifier(intId);
        IntegerID intId_2 = this.createIntId("fallnummer_psn_2", "654321");
        extId.addIdentifier(intId_2);

        Set<ID> idList = extId.getIdentifiers();
        Assert.assertEquals(idList.size(), 2);
        Assert.assertTrue(extId.getIdentifiers().contains(intId));
        Assert.assertTrue(extId.getIdentifiers().contains(intId_2));
    }

    @Test(dataProvider = "extId")
    public void testRemoveIdentifier(ExternalID extId) {
        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        extId.addIdentifier(intId);
        IntegerID intId_2 = this.createIntId("fallnummer_psn_2", "654321");
        extId.addIdentifier(intId_2);
        Assert.assertTrue(extId.getIdentifiers().contains(intId));
        Assert.assertTrue(extId.getIdentifiers().contains(intId_2));

        extId.removeIdentifier("fallnummer_psn");
        Assert.assertNull(extId.getIdentifier("fallnummer_psn"));
        Assert.assertFalse(extId.getIdentifiers().contains(intId));
        Assert.assertEquals(extId.getIdentifier("fallnummer_psn_2"), intId_2);
        Assert.assertTrue(extId.getIdentifiers().contains(intId_2));
    }
}
