/*******************************************************************************
 * Copyright (c) 2019 Jochen Kemnade.
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
 ******************************************************************************/
package sernet.verinice.service.test;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.junit.Test;

import sernet.gs.service.CsvFile;
import sernet.verinice.model.bp.elements.Application;
import sernet.verinice.model.bp.elements.ItNetwork;
import sernet.verinice.model.bp.groups.ApplicationGroup;
import sernet.verinice.model.common.CnALink;
import sernet.verinice.model.iso27k.Audit;
import sernet.verinice.model.iso27k.ISO27KModel;
import sernet.verinice.model.iso27k.Organization;
import sernet.verinice.samt.service.CreateSelfAssessment;
import sernet.verinice.service.commands.RemoveElement;
import sernet.verinice.service.commands.ValidateLinksInSubtrees;

public class ValidateLinksInSubtreesTest extends AbstractModernizedBaseProtection {

    @Test
    public void invalid_link_is_found() throws Exception {
        ItNetwork network = createNewBPOrganization();
        ApplicationGroup applications = createBpApplicationGroup(network);
        Application app1 = createBpApplication(applications);
        Application app2 = createBpApplication(applications);
        CnALink link1 = createLink(app1, app2, "rel_bp_application_bp_application");
        elementDao.executeCallback(session -> {
            Query query = session.createQuery(
                    "update CnALink set id.typeId = 'invalid_link_type' where id = :id");
            query.setParameter("id", link1.getId());
            int updated = query.executeUpdate();
            assertEquals(1, updated);
            return null;
        });
        CnALink link2 = createLink(app1, app2, "rel_bp_application_bp_application");

        ValidateLinksInSubtrees validateLinksInSubtrees = new ValidateLinksInSubtrees(
                Collections.singleton(applications.getUuid()));
        validateLinksInSubtrees = commandService.executeCommand(validateLinksInSubtrees);
        Set<CnALink> invalidLinks = validateLinksInSubtrees.getInvalidLinks();
        assertEquals(1, invalidLinks.size());
        CnALink invalidLink = invalidLinks.iterator().next();
        assertEquals("invalid_link_type", invalidLink.getRelationId());
        commandService.executeCommand(new RemoveElement(network));
    }

    @Test
    public void link_from_audit_to_control_is_valid() throws Exception {

        ISO27KModel model = (ISO27KModel) elementDao
                .findByCriteria(DetachedCriteria.forClass(ISO27KModel.class)).get(0);
        CreateSelfAssessment createSelfAssessment = new CreateSelfAssessment(model, "org",
                "assessment");
        CsvFile samtCatalog = new CsvFile(
                this.getClass().getResourceAsStream("/samt-catalog-5.0.3_de.csv"));
        createSelfAssessment.setCsvFile(samtCatalog);
        createSelfAssessment = commandService.executeCommand(createSelfAssessment);
        Organization org = createSelfAssessment.getOrganization();
        Audit audit = createSelfAssessment.getIsaAudit();

        ValidateLinksInSubtrees validateLinksInSubtrees = new ValidateLinksInSubtrees(
                Collections.singleton(audit.getUuid()));
        validateLinksInSubtrees = commandService.executeCommand(validateLinksInSubtrees);
        Set<CnALink> invalidLinks = validateLinksInSubtrees.getInvalidLinks();
        System.err.println(invalidLinks);
        assertEquals(0, invalidLinks.size());

        commandService.executeCommand(new RemoveElement(org));
    }

}