package de.pseudonymisierung.mainzelliste.matcher.hasher;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;

/**
 * Allows to formate the specified input data for generate hashes.
 */
@Entity
public class HashFormatter {

    /**
     * Instance of a HashFormatter
     */
    private static HashFormatter instance;

    /**
     * List which represents a format
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "Hashformatter", joinColumns = @JoinColumn(name = "format_id"))
    @Column(name = "format", table = "HashFormatterList")
    private List<String> format;

    /**  */
    @Transient
    private boolean formatChanged;

    /** Database id */
    @Id
    private final Long id = 1L;

    /**
     * Generates a HashFormatter. A format is created based on the specified 
     * input data in the configuration file. Input data can be contain names of 
     * fields or any combinations of characters. If a specified value is empty, 
     * it is interpreted as a space.
     * <br>
     * <br>
     * Example:<br>
     * <code>
     * Hash.input.field.0 = vorname    -> Field name
     * Hash.input.field.1 =            -> Space
     * Hash.input.field.2 = nachname   -> Field name
     * Hash.input.field.3 = _          -> _
     * Hash.input.field.4 = 1A         -> 1A (character combination)
     *
     * Resulting format: 'vorname nachname_1A'
     * </code>
     *
     * If a value is recognized as a field name, the value of the field is set.<br>
     * Example: 'HANS MEIER_1A'
     */
    private HashFormatter() {
        formatChanged = true;
    }

    /**
     * Read the specified input data and generate a list, which represents the
     * format. The number in the key is the position in the format. The first
     * position is 0. If a number is forgotten, all following positions are not
     * considered.<br>
     * <br>
     * Example:<br>
     * <code>
     * hash.input.field.0 = vorname
     * hash.input.field.1 =
     * hash.input.field.2 = nachname
     * Resulting format: 'vorname nachname'
     *
     * # Position 1 was forgotten
     * hash.input.field.0 = vorname
     * hash.input.field.2 =
     * hash.input.field.3 = nachname
     * Resulting format: 'vorname'
     * </code>
     *
     * @return List, which represents the read format.
     */
    private List<String> readFormat() {
               
        List<String> tmpFormat = new ArrayList<String>();

        for (int i = 0;; ++i) {
            String currentVal = Config.instance.getProperty("hash.input.field." + i);

            if (currentVal != null) {
                if (currentVal.isEmpty()) {
                    currentVal = " ";
                }

                tmpFormat.add(currentVal);
            } else {
                break;
            }
        }

        return tmpFormat;
    }

    /**
     * Returns an instance of the HashFormatter. If an instance is existed, this
     * instance is returned, otherwise a new instance is created. (Singleton)
     *
     * @return Instance of the HashFormatter
     */
    public static HashFormatter getInstance() {
        if (instance == null) {
            instance = new HashFormatter();
        }

        return instance;
    }

    /**
     * Initilize the attributes for checking whether the HashFormatter format
     * was changing after a restart of Mainzelliste.
     * 
     * This call is only needed once.
     */
    public void initialize() {
        List<String> currentFormat = readFormat();
        
        if (!currentFormat.equals(format)) {
            formatChanged = true;
            format = currentFormat;
        } else {
            formatChanged = false;
        }
    }

    /**
     * Generate a string based of the created format. Names of fields are
     * replaced by the respective value. The values are set based on the given
     * fields.<br>
     * <br>
     * Example:<br>
     * Fields: {vorname:HANS, nachname:MEIER}<br>
     * Format: 'vorname nachname' Resulting string: 'HANS MEIER'
     *
     * @param fields Fields with values.
     *
     * @return Generated string based on the given fields.
     */
    public String getHasherInput(Map<String, Field<?>> fields) {
        String inputStr = "";

        for (String key : format) {
            Field<?> tmpField = fields.get(key);

            if (tmpField == null) {
                inputStr += key;
            } else {
                inputStr += tmpField.getValue();
            }
        }

        return inputStr;
    }

    /**
     * Returns whether the format of hashing was changing after the last start.
     *
     * @return true if it was changed, false if it was not.
     */
    public boolean isHashFormatChanged() {
        return formatChanged;
    }
}
