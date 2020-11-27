package de.pseudonymisierung.mainzelliste;

import java.util.Set;

import org.testng.annotations.*;

import org.testng.Assert;

public class AssociatedIdsTest {

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

    @DataProvider(name="assocs")
    protected Object[][] createAssociatedIds() {
        return new Object[][] {
            { new AssociatedIds("faelle") }
        };
    }

    /**
     * Test the base state of an AssociatedId instance.
     * @param assocId
     */
    @Test(dataProvider = "assocs")
    public void testBaseState(AssociatedIds assocId) {
        // associated Id has a name
        Assert.assertNotNull(assocId.getType());
        Assert.assertTrue(assocId.getType().length() > 0);

        // associated Id has a Set of identifiers
        Assert.assertNotNull(assocId.getIdentifiers());
        Assert.assertEquals(assocId.getIdentifiers().size(), 0);

        // non existent identifier return null
        Assert.assertNull(assocId.getIdentifier("anyName"));

        // non existent identifier removal returns false
        Assert.assertFalse(assocId.removeIdentifier("anyName"));
    }

    /**
     * Test adding identifiers to an AssociatedIds instance.
     * @param assocId
     */
    @Test(dataProvider = "assocs")
    public void testAddIdentifier(AssociatedIds assocId) {
        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        assocId.addIdentifier(intId);
        ExternalID extId = this.createExtId("fallnummer", "asdf1234");
        assocId.addIdentifier(extId);

        Assert.assertNotNull(assocId.getIdentifier("fallnummer_psn"));
        Assert.assertTrue(assocId.getIdentifiers().contains(intId));
        Assert.assertNotNull(assocId.getIdentifier("fallnummer"));
        Assert.assertTrue(assocId.getIdentifiers().contains(extId));
    }

    /**
     * Test getting an identifier by type String
     * @param extId
     */
    @Test(dataProvider = "assocs")
    public void testGetIdentifier(AssociatedIds assocId) {
        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        assocId.addIdentifier(intId);

        ID returnedId = assocId.getIdentifier("fallnummer_psn");
        Assert.assertEquals(returnedId, intId);
    }

    /**
     * Test getting the Set of identifiers
     * @param extId
     */
    @Test(dataProvider = "assocs")
    public void testGetIdentifiers(AssociatedIds assocId) {
        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        assocId.addIdentifier(intId);
        ExternalID extId = this.createExtId("fallnummer", "asdf1234");
        assocId.addIdentifier(extId);

        Set<ID> idList = assocId.getIdentifiers();
        Assert.assertEquals(idList.size(), 2);
        Assert.assertTrue(assocId.getIdentifiers().contains(intId));
        Assert.assertTrue(assocId.getIdentifiers().contains(extId));
    }

    /**
     * Test removing a single id from an AssociatedIds instance
     * @param assocId
     */
    @Test(dataProvider = "assocs")
    public void testRemoveIdentifier(AssociatedIds assocId) {
        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        assocId.addIdentifier(intId);
        ExternalID extId = this.createExtId("fallnummer", "asdf1234");
        assocId.addIdentifier(extId);
        Assert.assertTrue(assocId.getIdentifiers().contains(intId));
        Assert.assertTrue(assocId.getIdentifiers().contains(extId));

        assocId.removeIdentifier("fallnummer_psn");
        Assert.assertNull(assocId.getIdentifier("fallnummer_psn"));
        Assert.assertFalse(assocId.getIdentifiers().contains(intId));
        Assert.assertEquals(assocId.getIdentifier("fallnummer"), extId);
        Assert.assertTrue(assocId.getIdentifiers().contains(extId));
    }
}
