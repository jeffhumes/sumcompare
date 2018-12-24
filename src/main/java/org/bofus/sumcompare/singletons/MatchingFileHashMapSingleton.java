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
public class MatchingFileHashMapSingleton
{
        private static final Logger                     logger  = LoggerFactory.getLogger(MatchingFileHashMapSingleton.class);
        private static MatchingFileHashMapSingleton matchingFileHashMapSingleton;
        private HashMap<String, String>       map    = null;

        private MatchingFileHashMapSingleton()
        {
                map = new HashMap<String, String>();
                
        }

        public static MatchingFileHashMapSingleton getInstance() throws IOException, SQLException, PropertyVetoException
        {
                if (matchingFileHashMapSingleton == null)
                {
                        matchingFileHashMapSingleton = new MatchingFileHashMapSingleton();
                        return matchingFileHashMapSingleton;
                }
                else
                {
                        return matchingFileHashMapSingleton;
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
