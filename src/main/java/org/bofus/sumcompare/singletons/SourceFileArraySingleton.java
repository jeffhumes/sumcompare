package org.bofus.sumcompare.singletons;

import java.beans.PropertyVetoException;
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
public class SourceFileArraySingleton
{
        private static final Logger                     logger  = LoggerFactory.getLogger(SourceFileArraySingleton.class);
        private static SourceFileArraySingleton sourceFileListSingleton;
        private ArrayList<String>       list    = null;

        private SourceFileArraySingleton()
        {
                list = new ArrayList<String>();
        }

        public static SourceFileArraySingleton getInstance() throws IOException, SQLException, PropertyVetoException
        {
                if (sourceFileListSingleton == null)
                {
                	sourceFileListSingleton = new SourceFileArraySingleton();
                        return sourceFileListSingleton;
                }
                else
                {
                        return sourceFileListSingleton;
                }
        }

        // retrieve array from anywhere
        public ArrayList<String> getArray()
        {
                return this.list;
        }

        //Add element to array
        public void addToArray(String errorObject)
        {
                list.add(errorObject);
        }

}
