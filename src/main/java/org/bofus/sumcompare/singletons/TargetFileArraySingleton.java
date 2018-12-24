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
public class TargetFileArraySingleton
{
        private static final Logger                     logger  = LoggerFactory.getLogger(TargetFileArraySingleton.class);
        private static TargetFileArraySingleton targetFileListSingleton;
        private ArrayList<String>       list    = null;

        private TargetFileArraySingleton()
        {
                list = new ArrayList<String>();
        }

        public static TargetFileArraySingleton getInstance() throws IOException, SQLException, PropertyVetoException
        {
                if (targetFileListSingleton == null)
                {
                	targetFileListSingleton = new TargetFileArraySingleton();
                        return targetFileListSingleton;
                }
                else
                {
                        return targetFileListSingleton;
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
