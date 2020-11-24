package de.pseudonymisierung.mainzelliste;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.*;

import org.testng.Assert;

public class ListIDTest {

    protected ExternalID createExtId() {
        ExternalID extId = new ExternalID();
        extId.setType("fallnummer");
        extId.setIdString("abcd1234");
        return extId;
    }

    protected IntegerID createIntId(String idType, String idString) {
        IntegerID intId = new IntegerID();
        intId.setType(idType);
        intId.setIdString(idString);
        return intId;
    }

    @DataProvider(name="listId")
    protected Object[][] createListId() {
        return new Object[][] {
            { new ListID("faelle") }
        };
    }

    @Test(dataProvider = "listId")
    public void addAnIdentifier(ListID faelle) {
        Assert.assertEquals(faelle.getIdList().size(), 0);
        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        faelle.addToIdList(intId);

        List<ID> list = faelle.getIdList();
        Assert.assertEquals(list.size(), 1);
        Assert.assertTrue(list.contains(intId));
    }

    @Test(dataProvider = "listId")
    public void addMultipleIdentifiers(ListID faelle) {
        Assert.assertEquals(faelle.getIdList().size(), 0);

        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        faelle.addToIdList(intId);
        IntegerID intId_2 = this.createIntId("fallnummer_psn", "654321");
        faelle.addToIdList(intId_2);

        Assert.assertEquals(faelle.getIdList().size(), 2);
        Assert.assertTrue(faelle.getIdList().contains(intId));
        Assert.assertTrue(faelle.getIdList().contains(intId_2));
    }

    @Test(dataProvider = "listId")
    public void getIdentifierList(ListID faelle) {
        Assert.assertEquals(faelle.getIdList().size(), 0);

        List<String> lookForIdValues = new ArrayList<String>();
        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        faelle.addToIdList(intId);
        lookForIdValues.add("123456");
        IntegerID intId_2 = this.createIntId("fallnummer_psn", "654321");
        faelle.addToIdList(intId_2);
        lookForIdValues.add("654321");

        Assert.assertEquals(faelle.getIdList().size(), 2);
        for(ID identifier: faelle.getIdList()) {
            lookForIdValues.remove(identifier.getIdString());
        }
        Assert.assertTrue(lookForIdValues.isEmpty());
    }

    @Test(dataProvider = "listId")
    public void testRemoveIdentifier(ListID faelle) {
        IntegerID intId = this.createIntId("fallnummer_psn", "123456");
        faelle.addToIdList(intId);
        IntegerID intId_2 = this.createIntId("fallnummer_psn", "654321");
        faelle.addToIdList(intId_2);

        Assert.assertTrue(faelle.getIdList().contains(intId));
        Assert.assertTrue(faelle.getIdList().contains(intId_2));

        faelle.removeFromIdList(intId);
        Assert.assertFalse(faelle.getIdList().contains(intId));
        Assert.assertTrue(faelle.getIdList().contains(intId_2));
    }
}