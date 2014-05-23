/** _________
   /   _____/ ______________  __ ___________
   \_____  \_/ __ \_  __ \  \/ // __ \_  __ \
   /        \  ___/|  | \/\   /\  ___/|  | \/
  /_______  /\___  >__|    \_/  \___  >__|
          \/     \/                 \/

    Abstract class for supplying basic methods for networking with a simple HTTP webservice.
    Important: Derived class should be implemented as a singleton.

    You must provide the configuration in resource files.
    Needed values in the resource file:

    <!-- Flag for indicating if to use the production or development settings -->
    <bool name="is_production_server">false</bool>

    <!-- DEV Server -->
    <string name="server_dev_host">homage-server-app-dev.elasticbeanstalk.com</string>
    <integer name="server_dev_port">80</integer>
    <string name="server_dev_protocol">http</string>

    <!-- Production Server -->
    <string name="server_prod_host">homage-server-app-prod.elasticbeanstalk.com</string>
    <integer name="server_prod_port">80</integer>
    <string name="server_prod_protocol">http</string>
*/

package com.homage.networking.server;

import android.content.Context;
import android.content.res.Resources;

import com.homage.app.R;

abstract public class Server {
    private boolean isProductionServer;
    private String host;
    private int port;
    private String protocol;
    private boolean alreadyInitialized;

    public void init(Context context) {
        // Can be initialized only once!
        if (alreadyInitialized)
            throw new AssertionError("Tried to initialize Server more than once.");

        Resources res = context.getResources();
        isProductionServer = res.getBoolean(R.bool.is_production_server);
        if (isProductionServer) {
            host = res.getString(R.string.server_prod_host);
            port = res.getInteger(R.integer.server_prod_port);
            protocol = res.getString(R.string.server_prod_protocol);
        } else {
            host = res.getString(R.string.server_dev_host);
            port = res.getInteger(R.integer.server_dev_port);
            protocol = res.getString(R.string.server_dev_protocol);
        }
    }
    //endregion
}

