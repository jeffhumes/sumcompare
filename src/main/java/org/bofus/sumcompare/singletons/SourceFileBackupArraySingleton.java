package org.bofus.sumcompare.singletons;

import java.beans.PropertyVetoException;
import java.io.File;
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
public class SourceFileBackupArraySingleton {
        private static SourceFileBackupArraySingleton sourceBackupFileListSingleton;
        private ArrayList<File> list = null;

        private SourceFileBackupArraySingleton() {
                list = new ArrayList<File>();
        }

        public static SourceFileBackupArraySingleton getInstance()
                        throws IOException, SQLException, PropertyVetoException {
                if (sourceBackupFileListSingleton == null) {
                        sourceBackupFileListSingleton = new SourceFileBackupArraySingleton();
                        return sourceBackupFileListSingleton;
                } else {
                        return sourceBackupFileListSingleton;
                }
        }

        // retrieve array from anywhere
        public ArrayList<File> getArray() {
                return this.list;
        }

        // Add element to array
        public void addToArray(File file) {
                list.add(file);
        }

}
