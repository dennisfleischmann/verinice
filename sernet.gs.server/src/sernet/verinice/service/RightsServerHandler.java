/*******************************************************************************
 * Copyright (c) 2012 Daniel Murygin.
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Daniel Murygin <dm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import sernet.verinice.interfaces.IRightsChangeListener;
import sernet.verinice.interfaces.IRightsServerHandler;
import sernet.verinice.interfaces.IRightsService;
import sernet.verinice.model.auth.Action;
import sernet.verinice.model.auth.Auth;
import sernet.verinice.model.auth.ConfigurationType;
import sernet.verinice.model.auth.Profile;
import sernet.verinice.model.auth.ProfileRef;
import sernet.verinice.model.auth.Profiles;
import sernet.verinice.model.auth.Userprofile;

/**
 * @see IRightsServerHandler
 * 
 *      This Class is {@ApplicationContextAware} so that it can access the bean
 *      context to get the 'rightsService' and add it self as change listeners
 *      to be notified when some user rights are changed. This is done in the
 *      init method which is called by spring after the context is created and
 *      fully populated.
 *
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public class RightsServerHandler
        implements IRightsServerHandler, IRightsChangeListener, ApplicationContextAware {

    private static final Logger LOG = Logger.getLogger(RightsServerHandler.class);

    private Map<String, Map<String, Action>> userActionMap;

    private Map<String, List<Userprofile>> userprofileMap;

    private Map<String, Profile> profileMap;

    private IRightsService rightsService;

    private ApplicationContext appContext;

    public RightsServerHandler() {
        super();
    }

    @Override
    public boolean isEnabled(String username, String actionId) {
        boolean returnValue = isBlacklist();
        Map<String, Action> actionMap = getUserActionMap().get(username);
        if (actionMap != null) {
            Action action = actionMap.get(actionId);
            returnValue = action != null && isWhitelist() || action == null && isBlacklist();
        }
        return returnValue;
    }

    private Map<String, Map<String, Action>> getUserActionMap() {
        if (userActionMap == null) {
            userActionMap = loadUserActionMap();
        }
        return userActionMap;
    }

    private Map<String, Map<String, Action>> loadUserActionMap() {
        userActionMap = new HashMap<>();
        for (String user : getUserprofileMap().keySet()) {
            userActionMap.put(user, loadActionMap(user));
        }
        return userActionMap;
    }

    private Map<String, Action> loadActionMap(String username) {
        HashMap<String, Action> actionMap = new HashMap<>();
        List<Userprofile> userprofileList = getUserprofileMap().get(username);
        if (userprofileList != null) {
            for (Userprofile userprofile : userprofileList) {
                List<ProfileRef> profileList = userprofile.getProfileRef();
                if (profileList != null) {
                    for (ProfileRef profileRef : profileList) {
                        Profile profileWithActions = getProfileMap().get(profileRef.getName());
                        if (profileWithActions != null) {
                            List<Action> actionList = profileWithActions.getAction();
                            for (Action action : actionList) {
                                actionMap.put(action.getId(), action);
                            }
                        } else {
                            LOG.error("Could not find profile " + profileRef.getName());
                        }
                    }
                }
            }
        }
        return actionMap;
    }

    public Map<String, List<Userprofile>> getUserprofileMap() {
        if (userprofileMap == null) {
            loadUserprofileMap();
        }
        return userprofileMap;
    }

    private Map<String, List<Userprofile>> loadUserprofileMap() {
        List<String> usernameList = rightsService.getUsernames();
        userprofileMap = rightsService.getUserprofileMap(Set.copyOf(usernameList));
        return userprofileMap;
    }

    public Map<String, Profile> getProfileMap() {
        if (profileMap == null) {
            loadProfileMap();
        }
        return profileMap;
    }

    private Profiles loadProfileMap() {
        Profiles profiles = rightsService.getProfiles();
        profileMap = new HashMap<>();
        for (Profile profile : profiles.getProfile()) {
            profileMap.put(profile.getName(), profile);
        }
        return profiles;
    }

    public boolean isBlacklist() {
        return ConfigurationType.BLACKLIST.equals(rightsService.getConfiguration().getType());
    }

    public boolean isWhitelist() {
        return ConfigurationType.WHITELIST.equals(rightsService.getConfiguration().getType());
    }

    @Override
    public void configurationChanged(Auth auth) {
        discardData();
    }

    @Override
    public void discardData() {
        profileMap = null;
        userActionMap = null;
        userprofileMap = null;
    }

    @Override
    protected void finalize() throws Throwable {
        rightsService.removeChangeListener(this);
        super.finalize();
    }

    /**
     * The init method is called by the spring framework after the application
     * context is completely bootstraped.
     */
    public void init() {
        registerIRightsService();
    }

    /**
     * Initialize the rightservice and register as change listener.
     */
    private void registerIRightsService() {
        if (rightsService == null) {
            rightsService = (IRightsService) appContext.getBean("rightsService");
            rightsService.addChangeListener(this);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        appContext = applicationContext;
    }
}
