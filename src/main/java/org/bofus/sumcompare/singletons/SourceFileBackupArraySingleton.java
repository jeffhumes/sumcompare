package org.bofus.sumcompare.singletons;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author a0868055
 *
 */
public class SourceFileBackupArraySingleton
{
        private static final Logger                     logger  = LoggerFactory.getLogger(SourceFileBackupArraySingleton.class);
        private static SourceFileBackupArraySingleton sourceBackupFileListSingleton;
        private ArrayList<File>       list    = null;

        private SourceFileBackupArraySingleton()
        {
                list = new ArrayList<File>();
        }

        public static SourceFileBackupArraySingleton getInstance() throws IOException, SQLException, PropertyVetoException
        {
                if (sourceBackupFileListSingleton == null)
                {
                	sourceBackupFileListSingleton = new SourceFileBackupArraySingleton();
                        return sourceBackupFileListSingleton;
                }
                else
                {
                        return sourceBackupFileListSingleton;
                }
        }

        // retrieve array from anywhere
        public ArrayList<File> getArray()
        {
                return this.list;
        }

        //Add element to array
        public void addToArray(File file)
        {
                list.add(file);
        }

}
