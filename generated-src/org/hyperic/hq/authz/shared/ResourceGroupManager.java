/*
 * Generated by XDoclet - Do not edit!
 */
package org.hyperic.hq.authz.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ejb.FinderException;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupSortField;
import org.hyperic.hq.authz.server.session.ResourceRelation;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.grouping.CritterList;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for ResourceGroupManager.
 */
public interface ResourceGroupManager
 
{
   /**
    * Create a resource group. Currently no permission checking.
    * @param roles List of {@link Role}s
    * @param resources List of {@link Resource}s
    */
   public ResourceGroup createResourceGroup( AuthzSubject whoami,ResourceGroup.ResourceGroupCreateInfo cInfo, Collection<Role> roles,Collection<Resource> resources ) throws GroupCreationException, GroupDuplicateNameException;

   /**
    * Find the group that has the given ID. Performs authz checking
    * @param whoami user requesting to find the group
    * @return {@link ResourceGroup} or null if it does not exist XXX scottmf, why is this method called find() but calls dao.get()???
    */
   public ResourceGroup findResourceGroupById( AuthzSubject whoami,Integer id ) throws PermissionException;

   /**
    * Find the group that has the given ID. Does not do any authz checking
    */
   public ResourceGroup findResourceGroupById( Integer id ) ;

   /**
    * Find the role that has the given name.
    * @param whoami user requesting to find the group
    * @param name The name of the role you're looking for.
    * @return The value-object of the role of the given name.
    * @throws PermissionException whoami does not have viewResourceGroup on the requested group
    */
   public ResourceGroup findResourceGroupByName( AuthzSubject whoami,String name ) throws PermissionException;

   public Collection<ResourceGroup> findDeletedGroups(  ) ;

   /**
    * Update some of the fundamentals of groups (name, description, location). If name, description or location are null, the associated properties of the passed group will not change.
    * @throws DuplicateObjectException if an attempt to rename the group would result in a group with the same name.
    */
   public void updateGroup( AuthzSubject whoami,ResourceGroup group,String name,String description,String location ) throws PermissionException, GroupDuplicateNameException;

   /**
    * Remove all groups compatible with the specified resource prototype.
    * @throws VetoException if another subsystem cannot allow it (for constraint reasons)
    */
   public void removeGroupsCompatibleWith( Resource proto ) throws VetoException;

   /**
    * Delete the specified ResourceGroup.
    * @param whoami The current running user.
    * @param group The group to delete.
    */
   public void removeResourceGroup( AuthzSubject whoami,ResourceGroup group ) throws PermissionException, VetoException;

   public void addResources( AuthzSubject subj,ResourceGroup group,List<Resource> resources ) throws PermissionException;

   /**
    * Add a resource to a group by resource id and resource type
    */
   public ResourceGroup addResource( AuthzSubject whoami,ResourceGroup group,Resource resource ) throws PermissionException;

   /**
    * RemoveResources from a group.
    * @param whoami The current running user.
    * @param group The group .
    */
   public void removeResources( AuthzSubject whoami,ResourceGroup group,Collection<Resource> resources ) throws PermissionException;

   /**
    * Sets the criteria list for this group.
    * @param whoami The current running user.
    * @param group This group.
    * @param critters List of critters to associate with this resource group.
    * @throws PermissionException whoami does not own the resource.
    * @throws GroupException critters is not a valid list of criteria.
    */
   public void setCriteria( AuthzSubject whoami,ResourceGroup group,CritterList critters ) throws PermissionException, GroupException;

   /**
    * Change the resource contents of a group to the specified list of resources.
    * @param resources A list of {@link Resource}s to be in the group
    */
   public void setResources( AuthzSubject whoami,ResourceGroup group,Collection<Resource> resources ) throws PermissionException;

   /**
    * List the resources in this group that the caller is authorized to see.
    * @param whoami The current running user.
    * @param groupValue This group.
    * @param pc Paging information for the request
    * @return list of authorized resources in this group.
    */
   public Collection<Resource> getResources( AuthzSubject whoami,Integer id ) ;

   /**
    * Get all the resource groups including the root resource group.
    */
   public List<ResourceGroupValue> getAllResourceGroups( AuthzSubject subject,PageControl pc ) throws PermissionException, javax.ejb.FinderException;

   /**
    * Get all the members of a group.
    * @return {@link Resource}s
    */
   public Collection<Resource> getMembers( ResourceGroup g ) ;

   /**
    * Get the member type counts of a group
    */
   public Map<String, Integer> getMemberTypes( ResourceGroup g ) ;

   /**
    * Get all the groups a resource belongs to
    * @return {@link ResourceGroup}s
    */
   public Collection<ResourceGroup> getGroups( Resource r ) ;

   /**
    * Get the # of groups within HQ inventory
    */
   public Number getGroupCount(  ) ;

   /**
    * Returns true if the passed resource is a member of the given group.
    */
   public boolean isMember( ResourceGroup group,Resource resource ) ;

   /**
    * Get the # of members in a group
    */
   public int getNumMembers( ResourceGroup g ) ;

   /**
    * Temporary method to convert a ResourceGroup into an AppdefGroupValue
    */
   public AppdefGroupValue getGroupConvert( AuthzSubject subj,ResourceGroup g ) ;

   public AppdefResourceType getAppdefResourceType( AuthzSubject subject,ResourceGroup group ) ;

   /**
    * Get a list of {@link ResourceGroup}s which are compatible with the specified prototype. Do not return any groups contained within 'excludeGroups' (a list of {@link ResourceGroup}s
    * @param prototype If specified, the resulting groups must be compatible with the prototype.
    * @param pInfo Pageinfo with a sort field of type {@link ResourceGroupSortField}
    */
   public PageList<ResourceGroup> findGroupsNotContaining( AuthzSubject subject,Resource member,Resource prototype,Collection<ResourceGroup> excGrps,org.hyperic.hibernate.PageInfo pInfo ) ;

   /**
    * Get a list of {@link ResourceGroup}s which are compatible with the specified prototype. Do not return any groups contained within 'excludeGroups' (a list of {@link ResourceGroup}s
    * @param prototype If specified, the resulting groups must be compatible with the prototype.
    * @param pInfo Pageinfo with a sort field of type {@link ResourceGroupSortField}
    */
   public PageList<ResourceGroup> findGroupsContaining( AuthzSubject subject,Resource member,Collection<ResourceGroup> excludeGroups,PageInfo pInfo ) ;

   /**
    * Get all the resource groups excluding the root resource group.
    */
   public Collection<ResourceGroup> getAllResourceGroups( AuthzSubject subject,boolean excludeRoot ) throws PermissionException;

   /**
    * Get all {@link ResourceGroup}s
    */
   public Collection<ResourceGroup> getAllResourceGroups(  ) ;

   /**
    * Get all compatible resource groups of the given entity type and resource type.
    */
   public Collection<ResourceGroup> getCompatibleResourceGroups( AuthzSubject subject,Resource resProto ) throws FinderException, PermissionException;

   /**
    * Get the resource groups with the specified ids
    * @param ids the resource group ids
    * @param pc Paging information for the request
    */
   public PageList<ResourceGroupValue> getResourceGroupsById( AuthzSubject whoami,Integer[] ids,PageControl pc ) throws PermissionException, FinderException;

   /**
    * Change owner of a group.
    */
   public void changeGroupOwner( AuthzSubject subject,ResourceGroup group,AuthzSubject newOwner ) throws PermissionException;

   /**
    * Get a ResourceGroup owner's AuthzSubjectValue
    * @param gid The group id
    * @exception FinderException Unable to find a group by id
    */
   public AuthzSubject getResourceGroupOwner( Integer gid ) throws FinderException;

   public ResourceGroup getResourceGroupByResource( Resource resource ) ;

   /**
    * Set a ResourceGroup modifiedBy attribute
    * @param whoami user requesting to find the group
    * @param id The ID of the role you're looking for.
    */
   public void setGroupModifiedBy( AuthzSubject whoami,Integer id ) ;

   public void updateGroupType( AuthzSubject subject,ResourceGroup g,int groupType,int groupEntType,int groupEntResType ) throws PermissionException;

   /**
    * Get the maximum collection interval for a scheduled metric within a compatible group of resources.
    * @return The maximum collection time in milliseconds. TODO: This does not belong here. Evict, evict! -- JMT 04/01/08
    */
   public long getMaxCollectionInterval( ResourceGroup g,Integer templateId ) ;

   /**
    * Return a List of Measurements that are collecting for the given template ID and group.
    * @param g The group in question.
    * @param templateId The measurement template to query.
    * @return templateId A list of Measurement objects with the given template id in the group that are set to be collected. TODO: This does not belong here. Evict, evict! -- JMT 04/01/08
    */
   public List<Measurement> getMetricsCollecting( ResourceGroup g,Integer templateId ) ;

   /**
    * Find the subject that has the given name and authentication source.
    * @param name Name of the subject.
    * @param authDsn DSN of the authentication source. Authentication sources are defined externally.
    * @return The value-object of the subject of the given name and authenticating source.
    */
   public AuthzSubject findSubjectByAuth( String name,String authDsn ) throws SubjectNotFoundException;

   public ResourceRelation getContainmentRelation(  ) ;

   public ResourceRelation getNetworkRelation(  ) ;

}
