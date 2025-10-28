package org.bofus.sumcompare.singletons;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author a0868055
 *
 */
public class CopiedFileHashMapSingleton {
        private static final Logger logger = LoggerFactory.getLogger(CopiedFileHashMapSingleton.class);
        private static CopiedFileHashMapSingleton copiedFileHashMapSingleton;
        private ConcurrentHashMap<String, String> map = null;

        private CopiedFileHashMapSingleton() {
                // Use ConcurrentHashMap for thread-safe operations
                map = new ConcurrentHashMap<String, String>();

        }

        public static CopiedFileHashMapSingleton getInstance() throws IOException, SQLException, PropertyVetoException {
                if (copiedFileHashMapSingleton == null) {
                        copiedFileHashMapSingleton = new CopiedFileHashMapSingleton();
                        return copiedFileHashMapSingleton;
                } else {
                        return copiedFileHashMapSingleton;
                }
        }

        // retrieve array from anywhere
        public ConcurrentHashMap<String, String> getMap() {
                return this.map;
        }

        // Add element to array
        public void addToMap(String key, String value) {
                map.put(key, value);
        }

}
