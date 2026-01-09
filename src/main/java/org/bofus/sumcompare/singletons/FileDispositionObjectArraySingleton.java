package org.bofus.sumcompare.singletons;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import org.bofus.sumcompare.model.FileDispositionObject;

/**
 * @author Jeff Humes
 *
 */
public class FileDispositionObjectArraySingleton {
        private static FileDispositionObjectArraySingleton fileDispositionObjectArraySingleton;
        private ArrayList<FileDispositionObject> list = null;

        private FileDispositionObjectArraySingleton() {
                list = new ArrayList<FileDispositionObject>();
        }

        public static FileDispositionObjectArraySingleton getInstance()
                        throws IOException, SQLException, PropertyVetoException {
                if (fileDispositionObjectArraySingleton == null) {
                        fileDispositionObjectArraySingleton = new FileDispositionObjectArraySingleton();
                        return fileDispositionObjectArraySingleton;
                } else {
                        return fileDispositionObjectArraySingleton;
                }
        }

        // retrieve array from anywhere
        public ArrayList<FileDispositionObject> getArray() {
                return this.list;
        }

        // Add element to array
        public void addToArray(FileDispositionObject thisObject) {
                list.add(thisObject);
        }

}
