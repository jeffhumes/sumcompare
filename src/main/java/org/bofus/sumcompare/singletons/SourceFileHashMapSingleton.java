package org.bofus.sumcompare.singletons;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author a0868055
 *
 */
public class SourceFileHashMapSingleton
{
        private static final Logger                     logger  = LoggerFactory.getLogger(SourceFileHashMapSingleton.class);
        private static SourceFileHashMapSingleton sourceFileHashMapSingleton;
        private HashMap<String, String>       map    = null;

        private SourceFileHashMapSingleton()
        {
                map = new HashMap<String, String>();
                
        }

        public static SourceFileHashMapSingleton getInstance() throws IOException, SQLException, PropertyVetoException
        {
                if (sourceFileHashMapSingleton == null)
                {
                        sourceFileHashMapSingleton = new SourceFileHashMapSingleton();
                        return sourceFileHashMapSingleton;
                }
                else
                {
                        return sourceFileHashMapSingleton;
                }
        }

        // retrieve array from anywhere
        public HashMap<String, String> getMap()
        {
                return this.map;
        }

        //Add element to array
        public void addToMap(String key, String value)
        {
        	map.put(key, value);
        }

}
