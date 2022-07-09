/*******************************************************************************
 * Copyright (c) 2009 Alexander Koderman <ak[at]sernet[dot]de>.
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 *     You should have received a copy of the GNU Lesser General Public 
 * License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Alexander Koderman <ak[at]sernet[dot]de> - initial API and implementation
 *     Robert Schuster <r.schuster@tarent.de> - use custom SQL/HQL for query
 *     Sebastian Hagedorn <sh[at]sernet[dot]de> - Bug fixing
 ******************************************************************************/
package sernet.verinice.service.commands.task;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import sernet.hui.common.connect.Entity;
import sernet.hui.common.connect.URLUtil;
import sernet.verinice.interfaces.GenericCommand;
import sernet.verinice.interfaces.IBaseDao;
import sernet.verinice.model.bsi.BSIModel;
import sernet.verinice.model.bsi.DocumentLink;
import sernet.verinice.model.bsi.DocumentLinkRoot;
import sernet.verinice.model.bsi.DocumentReference;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.common.CnATreeElementLabelGenerator;

/**
 * Retrieves the properties which are URLs as a {@link DocumentLinkRoot}
 * structure.
 */
public class FindURLs extends GenericCommand {

    private static final long serialVersionUID = -7822865940991034389L;

    private static final Logger LOG = Logger.getLogger(FindURLs.class);

    private DocumentLinkRoot root = new DocumentLinkRoot();

    private final Set<String> allIDs;

    public FindURLs(Set<String> allIDs) {
        this.allIDs = Collections.unmodifiableSet(new HashSet<>(allIDs));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute() {
        HibernateCallback hcb = new FindURLsCallbackWithCnATreeElement(allIDs);
        IBaseDao<BSIModel, Serializable> dao = getDaoFactory().getDAO(BSIModel.class);

        /*
         * Requests the links and resolves the CnATreeElement instances which
         * use them. (This is needed for the DocumentView).
         * 
         * The result is a list of Object arrays where: The URL is at index 0
         * and the dbid of an CnATreeElement is at index 1.
         */
        List<Object[]> resultList = dao.findByCallback(hcb);

        // Creates a list of Integers from the second argument. This is needed
        // in the
        // FindCnATreeElementsCallback.
        List<Integer> treeElementIds = new ArrayList<>();
        for (Object[] result : resultList) {
            treeElementIds.add((Integer) result[1]);
        }

        // Retrieves all the CnATreeElement instances which have a document link
        // according to the query contained in
        // FindURLsCallbackWithCnATreeElement.
        List<Object[]> treeElements = dao
                .findByCallback(new FindCnATreeElementsCallback(treeElementIds));

        // Fills the DocumentRoot structure by iterating the URLs and preparing
        // the
        // individual DocumentReference instances.
        final int size = resultList.size();
        for (int i = 0; i < size; i++) {
            String rawURL = (String) resultList.get(i)[0];
            int entityID = ((Integer) resultList.get(i)[1]).intValue();

            String name = URLUtil.getName(rawURL);
            String url = URLUtil.getHref(rawURL);

            DocumentLink link = createLinkIfNecessary(name, url);
            addReferenceToLink(treeElements, entityID, link);
        }
    }

    private void addReferenceToLink(List<Object[]> treeElements, int entityID, DocumentLink link) {
        if (link != null && treeElements != null) {
            CnATreeElement element = findElement(treeElements, entityID);
            if (element != null) {
                addReferenceIfAllowed(link, element);
            }
        }
    }

    private void addReferenceIfAllowed(DocumentLink link, CnATreeElement element) {
        DocumentReference reference = new DocumentReference(element);
        CnATreeElementLabelGenerator.getElementTitle(element);
        link.addChild(reference);
    }

    private CnATreeElement findElement(List<Object[]> treeElements, int entityID) {
        for (Object[] arr : treeElements) {
            if (((Entity) arr[1]).getDbId().equals(entityID)) {
                return (CnATreeElement) arr[0];
            }
        }
        return null;
    }

    private DocumentLink createLinkIfNecessary(String name, String url) {
        DocumentLink link = root.getDocumentLink(name, url);
        if (link == null && (!name.isEmpty() || !url.isEmpty())) { // at least
                                                                   // name or
                                                                   // url have
                                                                   // to be not
                                                                   // empty
            link = new DocumentLink(name, url);
            root.addChild(link);
        }
        return link;
    }

    public DocumentLinkRoot getUrls() {
        filterNullReferences();
        return root;
    }

    private void filterNullReferences() {
        DocumentLinkRoot filtered = new DocumentLinkRoot();
        for (DocumentLink link : root.getChildren()) {
            if (!link.getChildren().isEmpty()) {
                filtered.addChild(link);
            }
        }
        root = filtered;
    }

    @SuppressWarnings("serial")
    private class FindURLsCallbackWithCnATreeElement implements HibernateCallback, Serializable {

        private Set<String> types;

        private static final String HQL = "SELECT p.propertyValue,pl.entityId FROM PropertyList as pl "
                + "INNER JOIN pl.properties as p WHERE p.propertyType IN (:types) "
                + "AND p.propertyValue IS NOT NULL AND p.propertyValue NOT LIKE ''";

        FindURLsCallbackWithCnATreeElement(Set<String> types) {
            this.types = types;
        }

        public Object doInHibernate(Session session) throws SQLException {

            /**
             * Retrieves the property elements which are URLs along with the
             * CnATreeElement id that uses it.
             */

            Query hqlQuery = session.createQuery(HQL);
            hqlQuery.setParameterList("types", types, sernet.gs.reveng.type.Types.STRING_TYPE);
            if (LOG.isDebugEnabled()) {
                LOG.debug("QueryString:\t" + hqlQuery.getQueryString());
            }
            return hqlQuery.list();
        }

    }

    @SuppressWarnings("serial")
    private class FindCnATreeElementsCallback implements HibernateCallback, Serializable {
        private List<Integer> treeElementIds;

        private static final String HQL = "from CnATreeElement elmt inner join elmt.entity as entity"
                + " where entity.dbId in (:ids)";

        public FindCnATreeElementsCallback(List<Integer> treeElementIds) {
            this.treeElementIds = treeElementIds;
        }

        public Object doInHibernate(Session session) throws SQLException {

            /*
             * Retrieves all the CnATreeElements whose ids are mentioned in the
             * treeElementIds list (changed to hql to consider rightmanagement)
             */

            if (treeElementIds != null && !treeElementIds.isEmpty()) {
                return getDaoFactory().getDAO(CnATreeElement.class).findByQuery(HQL,
                        new String[] { "ids" }, new Object[] { treeElementIds });
            } else {
                return Collections.emptyList();
            }
        }

    }
}
