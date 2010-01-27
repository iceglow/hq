/*
 * Generated by XDoclet - Do not edit!
 */
package org.hyperic.hq.ui.shared;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Dashboard;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.server.session.DashboardManagerImpl;
import org.hyperic.hq.ui.server.session.RoleDashboardConfig;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.util.config.ConfigResponse;

/**
 * Local interface for DashboardManager.
 */
public interface DashboardManager {

    public UserDashboardConfig getUserDashboard(AuthzSubject me, AuthzSubject user) throws PermissionException;

    public RoleDashboardConfig getRoleDashboard(AuthzSubject me, Role r) throws PermissionException;

    public UserDashboardConfig createUserDashboard(AuthzSubject me, AuthzSubject user, String name)
        throws PermissionException;

    public RoleDashboardConfig createRoleDashboard(AuthzSubject me, Role r, String name) throws PermissionException;

    /**
     * Reconfigure a user's dashboard
     */
    public void configureDashboard(AuthzSubject me, DashboardConfig cfg, ConfigResponse newCfg)
        throws PermissionException;

    public void renameDashboard(AuthzSubject me, DashboardConfig cfg, String name) throws PermissionException;

    /**
     * Determine if a dashboard is editable by the passed user
     */
    public boolean isEditable(AuthzSubject me, DashboardConfig dash);

    public Collection<DashboardConfig> getDashboards(AuthzSubject me) throws PermissionException;

    /**
     * Update dashboard and user configs to account for resource deletion
     * @param ids An array of ID's of removed resources
     */
    public void handleResourceDelete(AppdefEntityID[] ids);

    public ConfigResponse getRssUserPreferences(String user, String token) throws LoginException;

    public void startup();

    public List<DashboardConfig> findEditableDashboardConfigs(WebUser user, AuthzBoss boss) 
        throws SessionNotFoundException, SessionTimeoutException, PermissionException, RemoteException;
    
    public List<Dashboard> findEditableDashboards(WebUser user, AuthzBoss boss)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException, RemoteException;

    public DashboardConfig findDashboard(Integer id, WebUser user, AuthzBoss boss);

}
