package org.bofus.sumcompare.singletons;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bofus.sumcompare.model.ExistingTargetFileObject;

/**
 * @author Jeff Humes
 *
 */
public class ExistingTargetFileObjectArraySingleton {
        private static ExistingTargetFileObjectArraySingleton existingTargetFileObjectArraySingleton;
        private ArrayList<ExistingTargetFileObject> list = null;

        private ExistingTargetFileObjectArraySingleton() {
                list = new ArrayList<ExistingTargetFileObject>();
        }

        public static ExistingTargetFileObjectArraySingleton getInstance()
                        throws IOException, SQLException, PropertyVetoException {
                if (existingTargetFileObjectArraySingleton == null) {
                        existingTargetFileObjectArraySingleton = new ExistingTargetFileObjectArraySingleton();
                        return existingTargetFileObjectArraySingleton;
                } else {
                        return existingTargetFileObjectArraySingleton;
                }
        }

        // retrieve array from anywhere
        public ArrayList<ExistingTargetFileObject> getArray() {
                return this.list;
        }

        // Add element to array
        public void addToArray(ExistingTargetFileObject thisObject) {
                list.add(thisObject);
        }

}
