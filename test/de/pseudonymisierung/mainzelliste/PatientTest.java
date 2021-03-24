package de.pseudonymisierung.mainzelliste;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PatientTest {
  Patient originalPatientA = createPatientA(); // "Max", "Mustermann", null

  /**
   * T1: update patient with the same fields
   */
  @Test
  public void testUpdateFrom_samePatient() {
    Patient patientA = createPatientA(); // "Max", "Mustermann"
    Patient patientB = createPatient("Max", "Mustermann", null);
    // execute test
    patientA.updateFrom(patientB, new HashSet<>());
    // fields of patientA should remain unchanged
    for (Entry<String, Field<?>> entry : originalPatientA.getFields().entrySet()) {
      Assert.assertEquals(patientA.getFields().get(entry.getKey()), entry.getValue());
    }
    Assert.assertEquals(patientA.getFields().size(), patientB.getFields().size());
  }

  /**
   * T2: update patient with a different field
   */
  @Test
  public void testUpdateFrom_differentFieldValues() {
    Patient patientA = createPatientA(); // "Max", "Mustermann"
    Patient patientB = createPatient("Max", "Peter", null);
    // execute test
    patientA.updateFrom(patientB, new HashSet<>());
    // fields of patientA should remain unchanged
    for (Entry<String, Field<?>> entry : originalPatientA.getFields().entrySet()) {
      Assert.assertEquals(patientA.getFields().get(entry.getKey()), entry.getValue());
    }
    Assert.assertEquals(patientA.getFields().size(), patientB.getFields().size());
  }

  /**
   * T3: update patient with new field
   */
  @Test
  public void testUpdateFrom_addNewField() {
    Patient patientA = createPatientA(); // "Max", "Mustermann"
    Patient patientB = createPatient("Max", "Mustermann", "01.02.1912");
    // execute test
    patientA.updateFrom(patientB, new HashSet<>());
    // fields of patientA should remain unchanged
    for (Entry<String, Field<?>> entry : originalPatientA.getFields().entrySet()) {
      Assert.assertEquals(patientA.getFields().get(entry.getKey()), entry.getValue());
    }
    // new field should be add
    Assert.assertEquals(patientA.getFields().get("birthday").getValue(), "01.02.1912");
    Assert.assertEquals(patientA.getFields().size(), patientB.getFields().size());
  }

  /**
   * T4: update patient with missed field
   */
  @Test
  public void testUpdateFrom_missedField() {
    Patient patientA = createPatientA(); // "Max", "Mustermann", null
    Patient patientB = createPatient("Max", null, null);
    // execute test
    patientA.updateFrom(patientB, new HashSet<>());
    // fields of patientA should remain unchanged
    for (Entry<String, Field<?>> entry : originalPatientA.getFields().entrySet()) {
      Assert.assertEquals(patientA.getFields().get(entry.getKey()), entry.getValue());
    }
    Assert.assertEquals(patientA.getFields().size(), originalPatientA.getFields().size());
  }

  // Utils
  ///////////////

  private Patient createPatientA() {
    return createPatient("Max", "Mustermann", null);
  }

  private Patient createPatient(String name, String lastname, String birthday) {
    Patient patient = new Patient();
    Map<String, Field<?>> fields = new HashMap<>();
    if (name != null) {
      fields.put("name", new PlainTextField(name));
    }
    if (lastname != null) {
      fields.put("lastname", new PlainTextField(lastname));
    }
    if (birthday != null) {
      fields.put("birthday", new PlainTextField(birthday));
    }
    patient.setFields(fields);
    return patient;
  }
}
