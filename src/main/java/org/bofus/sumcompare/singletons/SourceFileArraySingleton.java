package org.bofus.sumcompare.singletons;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeff Humes
 *
 */
@Slf4j
public class SourceFileArraySingleton {
        private static SourceFileArraySingleton sourceFileListSingleton;
        private ArrayList<String> list = null;

        private SourceFileArraySingleton() {
                list = new ArrayList<String>();
        }

        public static SourceFileArraySingleton getInstance() throws IOException, SQLException, PropertyVetoException {
                if (sourceFileListSingleton == null) {
                        sourceFileListSingleton = new SourceFileArraySingleton();
                        return sourceFileListSingleton;
                } else {
                        return sourceFileListSingleton;
                }
        }

        // retrieve array from anywhere
        public ArrayList<String> getArray() {
                return this.list;
        }

        // Add element to array
        public void addToArray(String errorObject) {
                list.add(errorObject);
        }

}
