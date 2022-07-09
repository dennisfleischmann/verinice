package sernet.verinice.server.ldap;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.ldap.SizeLimitExceededException;

import sernet.verinice.interfaces.ldap.ILdapService;
import sernet.verinice.interfaces.ldap.IPersonDao;
import sernet.verinice.interfaces.ldap.PersonParameter;
import sernet.verinice.service.ldap.PersonInfo;

public class LdapService implements ILdapService {

    private static final Logger LOG = Logger.getLogger(LdapService.class);

    private IPersonDao personDao;

    @Override
    public List<PersonInfo> getPersonList(PersonParameter parameter, String password) {
        try {
            return getPersonDao().getPersonList(parameter, password);
        } catch (SizeLimitExceededException sizeLimitException) {
            LOG.warn("Too many results when searching for LDAP users.");
            if (LOG.isDebugEnabled()) {
                LOG.debug("stacktrace: ", sizeLimitException);
            }
            throw new sernet.verinice.interfaces.ldap.SizeLimitExceededException();
        }
    }

    public IPersonDao getPersonDao() {
        return personDao;
    }

    public void setPersonDao(IPersonDao personDao) {
        this.personDao = personDao;
    }

    @Override
    public boolean isUsePasswordFromClient() {
        return personDao.isUsePasswordFromClient();
    }
}