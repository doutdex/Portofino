/*
 * Copyright (C) 2005-2011 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */

package com.manydesigns.portofino.database.platforms;

import com.manydesigns.elements.util.ReflectionUtil;
import com.manydesigns.portofino.PortofinoProperties;
import com.manydesigns.portofino.connections.ConnectionProvider;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
public class DatabasePlatformsManager {
    public static final String copyright =
            "Copyright (c) 2005-2011, ManyDesigns srl";

    //**************************************************************************
    // Fields
    //**************************************************************************

    protected final Configuration portofinoConfiguration;
    protected ArrayList<DatabasePlatform> databasePlatformList;

    //**************************************************************************
    // Logging
    //**************************************************************************

    public static final Logger logger =
            LoggerFactory.getLogger(DatabasePlatformsManager.class);

    //**************************************************************************
    // Constructors / building
    //**************************************************************************

    public DatabasePlatformsManager(Configuration portofinoConfiguration) {
        this.portofinoConfiguration = portofinoConfiguration;
        databasePlatformList = new ArrayList<DatabasePlatform>();
        String[] platformNames = portofinoConfiguration.getStringArray(
                PortofinoProperties.DATABASE_PLATFORMS_LIST);
        if (platformNames == null) {
            logger.debug("Empty list");
            return;
        }

        for (String current : platformNames) {
            addDatabasePlatform(current);
        }
    }

    protected void addDatabasePlatform(String databasePlatformClassName) {
        databasePlatformClassName = databasePlatformClassName.trim();
        logger.debug("Adding database platform: {}", databasePlatformClassName);
        DatabasePlatform databasePlatform =
                (DatabasePlatform)ReflectionUtil
                        .newInstance(databasePlatformClassName);
        if (databasePlatform == null) {
            logger.warn("Cannot load or instanciate: {}",
                    databasePlatformClassName);
        } else {
            databasePlatform.test();
            databasePlatformList.add(databasePlatform);
        }
    }

    //**************************************************************************
    // Methods
    //**************************************************************************

    public DatabasePlatform findApplicableAbstraction(
            ConnectionProvider connectionProvider) {
        for (DatabasePlatform current : databasePlatformList) {
            if (current.isApplicable(connectionProvider)) {
                return current;
            }
        }
        return null;
    }

    public DatabasePlatform[] getDatabasePlatforms() {
        DatabasePlatform[] result =
                new DatabasePlatform[databasePlatformList.size()];
        databasePlatformList.toArray(result);
        return result;
    }

    //**************************************************************************
    // Gettes/setters
    //**************************************************************************


    public Configuration getPortofinoConfiguration() {
        return portofinoConfiguration;
    }
}