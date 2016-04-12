package de.pseudonymisierung.mainzelliste.matcher.hasher;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Allows to formate the specified input data for generate hashes.
 * 
 * @author Christopher Hampf
 */
public class HashFormatter
{
    /** Instance of a HashFormatter */
    private static HashFormatter instance;
    
    /** List which represent a format */
    private List<String> format;
    
    /**
     * Generates a HashFormatter. Based on the specified input data in the
     * configuration file, a format is created. Input data can be names of fields
     * or any combinations of character. If a specified value is empty, it is 
     * interpreted as a space.
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
     * If a value is recognized as a field name, the value of the field is set.
     * Wird eine Angabe als Feld erkannt, so wird dessen Wert eingetragen.<br>
     * Example: 'HANS MEIER_1A'
     */
    private HashFormatter()
    {
        format = getFormat();
    }
    
    /**
     * Read the specified input data and generate a list, which represent the format.
     * The number in the key is the position in the format. The first position is 0.
     * If a number is forgotten, all following positions are not considered.<br>
     * <br>
     * Example:<br>
     * <code>
     * Hash.input.field.0 = vorname
     * Hash.input.field.1 = 
     * Hash.input.field.2 = nachname
     * Resulting format: 'vorname nachname'
     * 
     * # forget Position 1
     * Hash.input.field.0 = vorname
     * Hash.input.field.2 = 
     * Hash.input.field.3 = nachname
     * Resulting format: 'vorname'
     * </code>
     * 
     * @return List, which represent the read format.
     */
    private List<String> getFormat()
    {
        List<String> tmpFormat = new ArrayList<String>();
        
        for (int i = 0; ; ++i)
        {
            String currentVal = Config.instance.getProperty("Hash.input.field." + i);
                    
            if (currentVal != null && !currentVal.isEmpty())
            {
                if (currentVal.isEmpty())
                    currentVal = " ";
                
                tmpFormat.add(currentVal);
            }
            else
                break;
        }
        
        return tmpFormat;
    }
    
    /**
     * Returns an instance of the HashFormatter. If an instance is existed,
     * this instance is returned, otherwise a new instance is created. (Singleton)
     * 
     * @return Instance of the HashFormatter
     */
    public static HashFormatter getInstance()
    {
        if (instance == null)
            instance = new HashFormatter();
        
        return instance;
    }
    
    /**
     * Generate a string based of the created format. Names of fields are replaced
     * by the respective value. The values are set based on the given fields.<br>
     * <br>
     * Example:<br>
     * Fields: {vorname:HANS, nachname:MEIER}<br>
     * Format: 'vorname nachname'
     * Resulting string: 'HANS MEIER'
     * 
     * @param fields Fields with values.
     * 
     * @return Generated string based on the given fields.
     */
    public String getHasherInput(Map<String, Field<?>> fields)
    {
        String inputStr = "";
        
        for (String key : format)
        {
            Field<?> tmpField = fields.get(key);
            
            if (tmpField == null)
                inputStr += key;
            else
                inputStr += tmpField.getValue();
        }
        
        return inputStr;
    }
}
