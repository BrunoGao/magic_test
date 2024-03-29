/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.myob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.client.application.ApplicationUtils;
import org.openbravo.client.kernel.SessionDynamicTemplateComponent;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Creates the Workspace properties list which is initially loaded in the client.
 * 
 * @author mtaal
 */
public class MyOpenbravoComponent extends SessionDynamicTemplateComponent {

  static final String COMPONENT_ID = "MyOpenbravo";
  private static final String TEMPLATEID = "CA8047B522B44F61831A8CAA3AE2A7CD";

  private Logger log = Logger.getLogger(MyOpenbravoComponent.class);

  @Inject
  private MyOBUtils myOBUtils;

  /**
   * (non-Javadoc)
   * 
   * @see org.openbravo.client.kernel.BaseTemplateComponent#getComponentTemplate()
   */
  @Override
  protected String getTemplateId() {
    return TEMPLATEID;
  }

  /**
   * (non-Javadoc)
   * 
   * @see org.openbravo.client.kernel.BaseComponent#getId()
   */
  public String getId() {
    return COMPONENT_ID;
  }

  List<String> getAvailableWorkspaceWidgetClasses(String roleId) throws Exception {
    List<String> filters = new ArrayList<String>();
    filters.add("widgetClassAccess.widgetClass.availableInWorkspace IS true");
    filters.add("widgetClassAccess.widgetClass.superclass IS false");
    return getAccessibleWidgetClasses(roleId, filters);
  }

  List<String> getAvailableWidgetClasses(String roleId) throws Exception {
    List<String> filters = new ArrayList<String>();
    filters.add("widgetClassAccess.widgetClass.superclass IS false");
    return getAccessibleWidgetClasses(roleId, filters);
  }

  /**
   * @param roleId
   *          The id of the role whose available widget classes will be retrieved.
   * @param additionalFilters
   *          A list of filters to be applied on the base query to retrieve the accessible widget
   *          classes of a role.
   * @return the list of available widget classes for the role whose id is passed as parameter.
   */
  private List<String> getAccessibleWidgetClasses(String roleId, List<String> additionalFilters)
      throws Exception {
    OBContext.setAdminMode();
    try {
      List<String> widgetClassDefinitions = new ArrayList<String>();
      StringBuilder whereClause = new StringBuilder();
      if (additionalFilters != null) {
        for (String filter : additionalFilters) {
          whereClause.append("AND " + filter + " ");
        }
      }
      for (String widgetClassId : getAccessibleWidgetClassIds(roleId, whereClause.toString())) {
        WidgetClass widgetClass = OBDal.getInstance().getProxy(WidgetClass.class, widgetClassId);
        WidgetClassInfo widgetClassInfo;
        if (isInDevelopment()) {
          widgetClassInfo = myOBUtils.getWidgetClassInfoFromDatabase(widgetClass);
        } else {
          widgetClassInfo = myOBUtils.getWidgetClassInfo(widgetClass);
        }
        if (widgetClassInfo == null) {
          log.debug("Not found information for widget class with id " + widgetClass.getId());
          continue;
        }
        widgetClassDefinitions.add(widgetClassInfo.getWidgetClassProperties());
        if (!StringUtils.isEmpty(widgetClassInfo.getWidgetClassDefinition())) {
          widgetClassDefinitions.add(widgetClassInfo.getWidgetClassDefinition());
        }
      }
      log.debug("Available Widget Classes: " + widgetClassDefinitions.size());
      return widgetClassDefinitions;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> getAccessibleWidgetClassIds(String roleId, String additionalWhereClause) {
    final StringBuilder hql = new StringBuilder();
    hql.append("SELECT widgetClassAccess.widgetClass.id ");
    hql.append("FROM OBKMO_WidgetClassAccess widgetClassAccess ");
    hql.append("WHERE widgetClassAccess.role.id=:roleId ");
    hql.append("AND widgetClassAccess.active=true ");
    if (!StringUtils.isEmpty(additionalWhereClause)) {
      hql.append(additionalWhereClause);
    }
    Query query = OBDal.getInstance().getSession().createQuery(hql.toString());
    if (StringUtils.isEmpty(roleId)) {
      query.setString("roleId", OBContext.getOBContext().getRole().getId());
    } else {
      query.setString("roleId", roleId);
    }
    List<String> widgetClassIds = query.list();
    List<String> anonymousWidgetClasses;
    if (isInDevelopment()) {
      anonymousWidgetClasses = myOBUtils.getAnonymousAccessibleWidgetClassesFromDatabase();
    } else {
      anonymousWidgetClasses = myOBUtils.getAnonymousAccessibleWidgetClasses();
    }
    // Include the widget classes which allow anonymous access
    for (String anonymousWidgetClass : anonymousWidgetClasses) {
      if (!widgetClassIds.contains(anonymousWidgetClass)) {
        widgetClassIds.add(anonymousWidgetClass);
      }
    }
    return widgetClassIds;
  }

  /**
   * @return the list of available widget instances in the current context.
   */
  public List<String> getWidgetInstanceDefinitions() {
    OBContext.setAdminMode();
    try {
      final List<String> result = new ArrayList<String>();
      for (WidgetInstance widget : retrieveContextWidgetInstances()) {
        final JSONObject jsonObject = myOBUtils.getWidgetProvider(widget.getWidgetClass())
            .getWidgetInstanceDefinition(widget);
        result.add(jsonObject.toString());
      }
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public String getEnableAdminMode() {
    if (ApplicationUtils.isClientAdmin() || ApplicationUtils.isOrgAdmin()
        || ApplicationUtils.isRoleAdmin()) {
      return "true";
    }
    return "false";
  }

  // when changing code, check the ApplicationUtils.getAdminFormSettings
  // method also
  public String getAdminModeValueMap() {
    if (getEnableAdminMode().equals("false")) {
      return "{}";
    }

    try {

      final JSONObject valueMap = new JSONObject();
      final JSONObject jsonLevels = new JSONObject();

      final Role currentRole = OBDal.getInstance().get(Role.class,
          OBContext.getOBContext().getRole().getId());

      if (currentRole.getId().equals("0")) {
        Map<String, String> systemLevel = new HashMap<String, String>();
        systemLevel.put("system", "OBKMO_AdminLevelSystem");
        valueMap.put("level", systemLevel);
        valueMap.put("levelValue", JSONObject.NULL);
        return valueMap.toString();
      }

      final List<RoleOrganization> adminOrgs = ApplicationUtils.getAdminOrgs();
      final List<UserRoles> adminRoles = ApplicationUtils.getAdminRoles();

      if (ApplicationUtils.isClientAdmin()) {
        jsonLevels.put("client", "OBKMO_AdminLevelClient");
      }

      if (adminOrgs.size() > 0) {
        jsonLevels.put("org", "OBKMO_AdminLevelOrg");
      }

      if (adminRoles.size() > 0) {
        jsonLevels.put("role", "OBKMO_AdminLevelRole");
      }

      valueMap.put("level", jsonLevels);

      final Map<String, String> client = new HashMap<String, String>();
      client.put(OBContext.getOBContext().getCurrentClient().getId(), OBContext.getOBContext()
          .getCurrentClient().getName());

      final Map<String, String> org = new HashMap<String, String>();
      for (RoleOrganization currentRoleOrg : adminOrgs) {
        org.put(currentRoleOrg.getOrganization().getId(), currentRoleOrg.getOrganization()
            .getName());
      }

      final Map<String, String> role = new HashMap<String, String>();
      for (UserRoles currentUserRole : adminRoles) {
        role.put(currentUserRole.getRole().getId(), currentUserRole.getRole().getName());
      }

      final JSONObject levelValueMap = new JSONObject();
      levelValueMap.put("client", client);
      levelValueMap.put("org", org);
      levelValueMap.put("role", role);

      valueMap.put("levelValue", levelValueMap);

      return valueMap.toString();
    } catch (JSONException e) {
      log.error("Error building 'Admin Mode' value map: " + e.getMessage(), e);
    }
    return "{}";
  }

  private List<WidgetInstance> retrieveContextWidgetInstances() {
    final User user = OBContext.getOBContext().getUser();
    final Role role = OBContext.getOBContext().getRole();
    final Client client = OBContext.getOBContext().getCurrentClient();
    final List<String> accessibleWidgetClasses = getAccessibleWidgetClassIds(role.getId(), null);
    final List<WidgetInstance> userWidgets = getWidgetInstances(client, role, user,
        accessibleWidgetClasses);
    final List<WidgetInstance> defaultWidgets = getRoleDefaultWidgets(OBContext.getOBContext()
        .getRole(), client.getId(), OBContext.getOBContext().getWritableOrganizations());

    final List<WidgetInstance> contextWidgets = new ArrayList<WidgetInstance>();
    final List<WidgetInstance> copiedWidgets = new ArrayList<WidgetInstance>();
    for (WidgetInstance userWidget : userWidgets) {
      if (userWidget.isActive()) {
        contextWidgets.add(userWidget);
      }
      if (userWidget.getCopiedFrom() != null) {
        copiedWidgets.add(userWidget);
      }
    }

    log.debug("Copying new widget instances on user: " + user.getId() + " role: " + role.getId());
    final Organization orgZero = OBDal.getInstance().get(Organization.class, "0");
    boolean copyDone = false;
    for (WidgetInstance defaultWidget : defaultWidgets) {
      boolean defaultWidgetPresent = false;
      for (WidgetInstance copiedWidget : copiedWidgets) {
        if (copiedWidget.getCopiedFrom().getId().equals(defaultWidget.getId())) {
          defaultWidgetPresent = true;
          break;
        }
      }
      if (defaultWidgetPresent) {
        // do not copy the default widgets which are already defined on the user
        continue;
      }
      final WidgetInstance copy = (WidgetInstance) DalUtil.copy(defaultWidget);
      copy.setClient(client);
      copy.setOrganization(orgZero);
      copy.setVisibleAtRole(role);
      copy.setVisibleAtUser(user);
      copy.setCopiedFrom(defaultWidget);
      OBDal.getInstance().save(copy);
      log.debug("Copied widget instance: " + copy.getId() + " of Widget Class: "
          + copy.getWidgetClass().getWidgetTitle());
      copyDone = true;
      if (accessibleWidgetClasses.contains(copy.getWidgetClass().getId())) {
        contextWidgets.add(copy);
      }
    }
    if (copyDone) {
      OBDal.getInstance().flush();
    }
    log.debug("Available User widgets:" + contextWidgets.size());
    return contextWidgets;
  }

  private List<WidgetInstance> getWidgetInstances(Client client, Role visibleAtRole,
      User visibleAtUser, List<String> widgetClasses) {
    OBCriteria<WidgetInstance> obc = OBDal.getInstance().createCriteria(WidgetInstance.class);
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnActive(false);
    obc.add(Restrictions.eq(WidgetInstance.PROPERTY_CLIENT, client));
    obc.add(Restrictions.eq(WidgetInstance.PROPERTY_VISIBLEATROLE, visibleAtRole));
    obc.add(Restrictions.eq(WidgetInstance.PROPERTY_VISIBLEATUSER, visibleAtUser));
    obc.add(Restrictions.in(WidgetInstance.PROPERTY_WIDGETCLASS + "." + WidgetClass.PROPERTY_ID,
        widgetClasses));
    return obc.list();
  }

  private List<WidgetInstance> getRoleDefaultWidgets(Role role, String clientId, Set<String> orgs) {
    final List<WidgetInstance> defaultWidgets = new ArrayList<WidgetInstance>();

    if (!role.isForPortalUsers()) {
      // do not include global widgets in portal roles
      defaultWidgets.addAll(MyOBUtils.getDefaultWidgetInstancesAtOBLevel());
      defaultWidgets.addAll(MyOBUtils.getDefaultWidgetInstancesAtSystemLevel());
    }

    defaultWidgets.addAll(MyOBUtils.getDefaultWidgetInstancesAtClientLevel(clientId));
    defaultWidgets.addAll(MyOBUtils.getDefaultWidgetInstancesAtOrgLevel(orgs));
    defaultWidgets.addAll(MyOBUtils.getDefaultWidgetInstancesAtRoleLevel(role));

    return defaultWidgets;
  }
}
