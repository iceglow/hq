<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="struts-html-el" prefix="html" %>
<%@ taglib uri="struts-tiles" prefix="tiles" %>
<%@ taglib uri="jstl-fmt" prefix="fmt" %>
<%@ taglib uri="jstl-c" prefix="c" %>
<%@ taglib uri="display" prefix="display" %>
<%@ taglib uri="hq" prefix="hq" %>
<%--
  NOTE: This copyright does *not* cover user programs that use HQ
  program services by normal system calls through the application
  program interfaces provided as part of the Hyperic Plug-in Development
  Kit or the Hyperic Client Development Kit - this is merely considered
  normal use of the program, and does *not* fall under the heading of
  "derived work".

  Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
  This file is part of HQ.

  HQ is free software; you can redistribute it and/or modify
  it under the terms version 2 of the GNU General Public License as
  published by the Free Software Foundation. This program is distributed
  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE. See the GNU General Public License for more
  details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
  USA.
 --%>


<hq:pageSize var="pageSize"/>
<c:set var="widgetInstanceName" value="resources"/>
<c:url var="selfAction" value="/dashboard/Admin.do?mode=metricViewer"/>

<script language="JavaScript" src="<html:rewrite page="/js/listWidget.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var pageData = new Array();
initializeWidgetProperties('<c:out value="${widgetInstanceName}"/>');
widgetProperties = getWidgetProperties('<c:out value="${widgetInstanceName}"/>');
var help = '<hq:help/>';
</script>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr class="PageTitle">
    <td rowspan="99"><html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/></td>
    <td><html:img page="/images/spacer.gif" width="15" height="1" alt="" border="0"/></td>
    <td width="34%" class="PageTitle"><fmt:message key="dash.home.MetricViewer.Title"/></td>
    <td width="33%"><html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/></td>
    <td width="32%"><html:img page="/images/spacer.gif" width="202" height="32" alt="" border="0"/></td>
    <td width="1%"><html:link href="" onclick="window.open(help,'help','width=800,height=650,scrollbars=yes,toolbar=yes,left=80,top=80,resizable=yes'); return false;"><html:img page="/images/title_pagehelp.gif" width="20" height="20" alt="" border="0" hspace="10"/></html:link></td>
  </tr>
  <tr>
    <td valign="top" align="left" rowspan="99"><html:img page="/images/title_TLcorner.gif" width="8" height="8" alt="" border="0"/></td>
    <td ><html:img page="/images/spacer.gif" width="1" height="10" alt="" border="0"/></td>
  </tr>
  <tr valign="top">
    <td colspan='3'>
      <html:form action="/dashboard/ModifyMetricViewer" >

      <tiles:insert definition=".header.tab">
        <tiles:put name="tabKey" value="dash.settings.DisplaySettings"/>
      </tiles:insert>

      <tiles:insert definition=".dashContent.admin.generalSettings">
        <tiles:put name="portletName" beanName="portletName" />
      </tiles:insert>
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td colspan="4" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
         <tr valign="top">
          <td width="20%" class="BlockLabel" valign="center"><fmt:message key="dash.settings.FormLabel.MetricViewerRange"/></td>
          <td width="80%" class="BlockContent" colspan="3" valign="center">
              
            <fmt:message key="dash.settings.metricViewer.top"/>&nbsp;
            <html:select property="numberToShow">
              <html:option value="5"/>
              <html:option value="10"/>
              <html:option value="20"/>
              <html:option value="30"/>
            </html:select>
            &nbsp;<fmt:message key="dash.settings.metricViewer.resources"/>
     
          </td>
        </tr>
        <tr valign="top">
          <td width="20%" class="BlockLabel" valign="center"><fmt:message key="dash.settings.FormLabel.ResourceType"/></td>
          <td width="80%" class="BlockContent" colspan="3" valign="center">
              <html:select property="resourceType" onclick="MetricViewerForm.submit()">
              <html:option value="-1" disabled="true"><fmt:message key="dash.settings.metricViewer.selectResourceType"/></html:option>
              <html:option value="-1" disabled="true"><fmt:message key="dash.settings.metricViewer.platformTypes"/></html:option>
              <c:forEach var="type" items="${platformTypes}">
                  <html:option value="${type.appdefTypeKey}"> - <c:out value="${type.name}"/></html:option>
              </c:forEach>
              <html:option value="" disabled="true"></html:option>
              <html:option value="-1" disabled="true"><fmt:message key="dash.settings.metricViewer.serverTypes"/></html:option>
              <c:forEach var="type" items="${serverTypes}">
                  <html:option value="${type.appdefTypeKey}"> - <c:out value="${type.name}"/></html:option>
              </c:forEach>
              <html:option value="" disabled="true"></html:option>
              <html:option value="-1" disabled="true"><fmt:message key="dash.settings.metricViewer.serviceTypes"/></html:option>
              <c:forEach var="type" items="${serviceTypes}">
                  <html:option value="${type.appdefTypeKey}"> - <c:out value="${type.name}"/></html:option>
              </c:forEach>
              </html:select>
          </td>
        </tr>
        <tr valign="top">
          <td width="20%" class="BlockLabel" valign="center"><fmt:message key="dash.settings.FormLabel.Metric"/></td>
          <td width="80%" class="BlockContent" colspan="3" valign="center">
             <html:select property="metric" onclick="MetricViewerForm.submit()">
             <html:option value="-1" disabled="true"><fmt:message key="dash.settings.metricViewer.selectMetric"/></html:option>
             <c:forEach var="metric" items="${metrics}">
                 <c:if test="${metric.defaultOn}">
                 <html:option value="${metric.id}"> - <c:out value="${metric.name}"/></html:option>
                 </c:if>
             </c:forEach>    
             </html:select>
          </td>
        </tr>
        <tr>
          <td colspan="4" class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
        <tr>
          <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
        </tr>
      </table>
      &nbsp;<br>
      <tiles:insert definition=".header.tab">
        <tiles:put name="tabKey" value="dash.settings.SelectedResources"/>
      </tiles:insert>

      <display:table cellspacing="0" cellpadding="0" width="100%" action="${selfAction}"
                     pageSize="${pageSize}" items="${metricViewerList}" var="resource"  >

        <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAll(this, widgetProperties, true)\" name=\"listToggleAll\">"  isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
          <display:checkboxdecorator name="ids" value="${resource.entityId.type}:${resource.id}" onclick="ToggleSelection(this, widgetProperties, true)" styleClass="listMember"/>
        </display:column>

        <display:column width="50%" property="name" sort="true" sortAttr="7"
                  defaultSort="true" title="dash.settings.ListHeader.Resource" />

        <display:column width="50%" property="description"
                        title="common.header.Description" />

      </display:table>


      <c:url var="addToListUrl" value="/Admin.do" context="/dashboard">
          <c:param name="mode" value="metricViewerAddResources"/>
          <c:param name="key" value=".dashContent.metricviewer.resources"/>
          <c:param name="ff" value="${MetricViewerForm.appdefType}"/>
          <c:param name="ft" value="${MetricViewerForm.appdefTypeID}"/>
      </c:url>
      <tiles:insert definition=".toolbar.addToList">
        <tiles:put name="addToListUrl" beanName="addToListUrl"/>
        <tiles:put name="listItems" beanName="metricViewerList"/>
        <tiles:put name="listSize" beanName="metricViewerTotalSize"/>
        <tiles:put name="widgetInstanceName" beanName="widgetInstanceName"/>
        <tiles:put name="pageSizeAction" beanName="selfAction" />
        <tiles:put name="pageNumAction" beanName="selfAction"/>
        <tiles:put name="defaultSortColumn" value="1"/>
      </tiles:insert>

      <tiles:insert definition=".form.buttons"/>
      </html:form>

    </td>
  </tr>
  <tr>
    <td colspan="4"><html:img page="/images/spacer.gif" width="1" height="13" alt="" border="0"/></td>
  </tr>
</table>

