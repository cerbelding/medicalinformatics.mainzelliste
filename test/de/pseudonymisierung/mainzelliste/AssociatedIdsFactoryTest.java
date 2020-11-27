package de.pseudonymisierung.mainzelliste;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AssociatedIdsFactoryTest {

    @DataProvider(name="factory")
    public Object[][] createFactory() {
        Properties props = new Properties();
        props.setProperty("associatedids.faelle.idgenerators", "fallnummer, fall_psn");

        return new Object[][] {
            { new AssociatedIdsFactory(props) }
        };
    }

    /**
     * Check creating a new AssociatedIds instance with the factory.
     * @param assocFactory
     */
    @Test(dataProvider = "factory")
    public void getAssociatedIds(AssociatedIdsFactory assocFactory) {
        AssociatedIds assocId = assocFactory.createAssociatedIds("faelle");
        Assert.assertTrue(assocFactory.isAssociatedIdsType("faelle"));
        Assert.assertNotNull(assocId);
        Assert.assertNotNull(assocId.getIdentifiers());
        Assert.assertEquals(assocId.getIdentifiers().size(), 0);
    }

    /**
     * Make sure an Exception is thrown on invalid AssociatedIds types
     * @param assocFactory
     */
    @Test(dataProvider = "factory")
    public void wrongAssocTypeThrows(AssociatedIdsFactory assocFactory) {
        Assert.assertFalse(assocFactory.isAssociatedIdsType("someUnknownType"));
        Assert.assertThrows(IllegalArgumentException.class, () -> assocFactory.createAssociatedIds("someUnknownType"));
    }

    /**
     * Check creating a new AssociatedIds instance by ID.
     * @param assocFactory
     */
    @Test(dataProvider = "factory")
    public void getAssociatedIdsByID(AssociatedIdsFactory assocFactory) {
        ExternalID extId = new ExternalID();
        extId.setType("fallnummer");
        extId.setIdString("asdf1234");

        AssociatedIds assocId = assocFactory.createAssociatedIdsFor(extId);
        Assert.assertTrue(assocFactory.isAssociatedIdsID(extId));
        Assert.assertNotNull(assocId);
        Assert.assertNotNull(assocId.getIdentifiers());
        Assert.assertEquals(assocId.getIdentifiers().size(), 0);
    }

    /**
     * Make sure invalid idTypes throw an Exception when trying to create an AssociatedIds instance
     * for it.
     * @param assocFactory
     */
    @Test(dataProvider = "factory")
    public void wrongIdTypeThrowsWhileGetting(AssociatedIdsFactory assocFactory) {
        ExternalID extId = new ExternalID();
        extId.setType("unknown");
        extId.setIdString("anystring");

        Assert.assertFalse(assocFactory.isAssociatedIdsID(extId));
        Assert.assertThrows(IllegalArgumentException.class, () -> assocFactory.createAssociatedIdsFor(extId));
    }

    /**
     * Check that two IDs configured for the same AssociatedIds type yield AssociatedIds instances
     * of the same type.
     * @param assocFactory
     */
    @Test(dataProvider = "factory")
    public void sameIdTypesGetSameAssocType(AssociatedIdsFactory assocFactory) {
        ExternalID extId = new ExternalID();
        extId.setType("fallnummer");
        extId.setIdString("asdf1234");

        IntegerID intId = new IntegerID();
        intId.setType("fall_psn");
        intId.setIdString("1");

        AssociatedIds assocId_ext = assocFactory.createAssociatedIdsFor(extId);
        Assert.assertNotNull(assocId_ext);
        AssociatedIds assocId_int = assocFactory.createAssociatedIdsFor(intId);
        Assert.assertNotNull(assocId_int);

        Assert.assertEquals(assocId_ext.getType(), assocId_int.getType());
    }

    /**
     * Preferred way to create or retrieve an AssociatedIds instance
     * @param assocFactory
     */
    // Deactivated: integration test
    //@Test(dataProvider = "factory")
    public void getAssociatedIdsFor(AssociatedIdsFactory assocFactory) {
        ExternalID extId = new ExternalID();
        extId.setType("fallnummer");
        extId.setIdString("asdf1234");

        AssociatedIds assocId = assocFactory.getAssociatedIdsFor(extId);
        Assert.assertNotNull(assocId);
        Assert.assertTrue(assocId.getIdentifiers().size() >= 1);
        Assert.assertEquals(assocId.getIdentifier(extId.getType()), extId);
    }

    /**
     * Convenience method testing: creating all IDs for an AssociatedIds instance.
     * @param assocFactory
     */
    // Deactivated: integration test
    //@Test(dataProvider = "factory")
    public void createAllIdentifiersForAssocId(AssociatedIdsFactory assocFactory) {
        ExternalID extId = new ExternalID();
        extId.setType("fallnummer");
        extId.setIdString("asdf1234");

        AssociatedIds assocId = new AssociatedIds("faelle");
        assocId.addIdentifier(extId);
        assocId = assocFactory.createAllIdentifiers(assocId);

        Assert.assertEquals(assocId.getIdentifiers().size(), 2);
        Assert.assertNotNull(assocId.getIdentifier("fall_psn"));
        Assert.assertTrue(assocId.getIdentifier("fall_psn").getIdString().length() > 0);
    }
}
