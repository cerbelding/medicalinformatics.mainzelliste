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
        Assert.assertNotNull(assocId.getIds());
        Assert.assertEquals(assocId.getIds().size(), 0);

        // non existent identifier return null
        Assert.assertNull(assocId.getId("anyName"));

        // non existent identifier removal returns false
        Assert.assertFalse(assocId.removeId("anyName"));
    }

    /**
     * Test adding identifiers to an AssociatedIds instance.
     * @param assocId
     */
    @Test(dataProvider = "assocs")
    public void testAddIdentifier(AssociatedIds assocId) {
        IntegerID intId = this.createIntId("intVisitId", "123456");
        assocId.addId(intId);
        ExternalID extId = this.createExtId("extVisitId", "asdf1234");
        assocId.addId(extId);

        Assert.assertNotNull(assocId.getId("intVisitId"));
        Assert.assertTrue(assocId.getIds().contains(intId));
        Assert.assertNotNull(assocId.getId("extVisitId"));
        Assert.assertTrue(assocId.getIds().contains(extId));
    }

    /**
     * Test getting an identifier by type String
     * @param extId
     */
    @Test(dataProvider = "assocs")
    public void testGetIdentifier(AssociatedIds assocId) {
        IntegerID intId = this.createIntId("intVisitId", "123456");
        assocId.addId(intId);

        ID returnedId = assocId.getId("intVisitId");
        Assert.assertEquals(returnedId, intId);
    }

    /**
     * Test getting the Set of identifiers
     * @param extId
     */
    @Test(dataProvider = "assocs")
    public void testGetIdentifiers(AssociatedIds assocId) {
        IntegerID intId = this.createIntId("intVisitId", "123456");
        assocId.addId(intId);
        ExternalID extId = this.createExtId("extVisitId", "asdf1234");
        assocId.addId(extId);

        Set<ID> idList = assocId.getIds();
        Assert.assertEquals(idList.size(), 2);
        Assert.assertTrue(assocId.getIds().contains(intId));
        Assert.assertTrue(assocId.getIds().contains(extId));
    }

    /**
     * Test removing a single id from an AssociatedIds instance
     * @param assocId
     */
    @Test(dataProvider = "assocs")
    public void testRemoveIdentifier(AssociatedIds assocId) {
        IntegerID intId = this.createIntId("intVisitId", "123456");
        assocId.addId(intId);
        ExternalID extId = this.createExtId("extVisitId", "asdf1234");
        assocId.addId(extId);
        Assert.assertTrue(assocId.getIds().contains(intId));
        Assert.assertTrue(assocId.getIds().contains(extId));

        assocId.removeId("intVisitId");
        Assert.assertNull(assocId.getId("intVisitId"));
        Assert.assertFalse(assocId.getIds().contains(intId));
        Assert.assertEquals(assocId.getId("extVisitId"), extId);
        Assert.assertTrue(assocId.getIds().contains(extId));
    }
}
