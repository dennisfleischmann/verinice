/*******************************************************************************
 * Copyright (c) 2010 Daniel Murygin.
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
package sernet.verinice.bp.rcp.action;

import org.eclipse.osgi.util.NLS;

/**
 * @author Daniel Murygin <dm@sernet.de>
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "sernet.verinice.bp.rcp.action.messages"; //$NON-NLS-1$
	
	public static String AddITNetworkDelegate_0;
	public static String AddRoomDelegate_0;
	public static String AddApplicationDelegate_0;
	public static String AddOtherSystemDelegate_0;
	public static String AddNetworkDelegate_0;
	public static String AddPersonDelegate_0;
	public static String AddITSystemDelegate_0;
	public static String AddICSSystemDelegate_0;
	public static String AddBusinessprocessDelegate_0;
    public static String AddBpDocumentDelegate_0;
    public static String AddBpIncidentDelegate_0;
    public static String AddBpRecordDelegate_0;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
