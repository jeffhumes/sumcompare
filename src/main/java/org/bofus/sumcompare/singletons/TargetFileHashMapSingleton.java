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
public class TargetFileHashMapSingleton {
        private static final Logger logger = LoggerFactory.getLogger(TargetFileHashMapSingleton.class);
        private static TargetFileHashMapSingleton targetFileHashMapSingleton;
        private ConcurrentHashMap<String, String> map = null;

        private TargetFileHashMapSingleton() {
                // Use ConcurrentHashMap for thread-safe operations without explicit
                // synchronization
                map = new ConcurrentHashMap<String, String>();

        }

        public static TargetFileHashMapSingleton getInstance() throws IOException, SQLException, PropertyVetoException {
                if (targetFileHashMapSingleton == null) {
                        targetFileHashMapSingleton = new TargetFileHashMapSingleton();
                        return targetFileHashMapSingleton;
                } else {
                        return targetFileHashMapSingleton;
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
