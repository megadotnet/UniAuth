package com.dianrong.common.techops.action;

import com.dianrong.common.uniauth.common.bean.Response;
import com.dianrong.common.uniauth.common.bean.dto.PageDto;
import com.dianrong.common.uniauth.common.bean.dto.TagDto;
import com.dianrong.common.uniauth.common.bean.dto.TagTypeDto;
import com.dianrong.common.uniauth.common.bean.dto.UserDto;
import com.dianrong.common.uniauth.common.bean.request.*;
import com.dianrong.common.uniauth.sharerw.facade.UARWFacade;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Arc on 8/4/2016.
 */
@RestController
@RequestMapping("tag")
public class TagAction {

    @Resource
    private UARWFacade uARWFacade;

    @RequestMapping(value = "query", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("(hasRole('ROLE_SUPER_ADMIN') and principal.hasDomain('techops')) or " +
            "(hasRole('ROLE_ADMIN') and principal.domainIdSet.contains(#tagQuery.domainId))")
    public Response<PageDto<TagDto>> queryTags(@RequestBody TagQuery tagQuery) {
        return uARWFacade.getTagRWResource().searchTags(tagQuery);
    }

    @RequestMapping(value = "add", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("(hasRole('ROLE_SUPER_ADMIN') and principal.hasDomain('techops')) or " +
            "(hasRole('ROLE_ADMIN') and principal.domainIdSet.contains(#tagParam.domainId))")
    public Response<TagDto> addNewTag(@RequestBody TagParam tagParam) {
        return uARWFacade.getTagRWResource().addNewTag(tagParam);
    }

    @RequestMapping(value = "update", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("(hasRole('ROLE_SUPER_ADMIN') and principal.hasDomain('techops')) or " +
            "(hasRole('ROLE_ADMIN') and principal.domainIdSet.contains(#tagParam.domainId))")
    public Response<TagDto> updateTag(@RequestBody TagParam tagParam) {
        return uARWFacade.getTagRWResource().updateTag(tagParam);
    }

    @RequestMapping(value = "/replace-grps-users-to-tag" , method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("(hasRole('ROLE_SUPER_ADMIN') and principal.hasDomain('techops')) "
            + "or (hasRole('ROLE_ADMIN') and hasPermission(#tagParam, 'PERM_TAGID_CHECK'))")
    public Response<Void> replaceGroupsAndUsersToRole(@RequestBody TagParam tagParam) {
        return uARWFacade.getTagRWResource().replaceGroupsAndUsersToTag(tagParam);
    }

    @RequestMapping(value = "types", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("(hasRole('ROLE_SUPER_ADMIN') and principal.hasDomain('techops')) or " +
            "(hasRole('ROLE_ADMIN') and principal.domainIdSet.contains(#tagTypeQuery.domainId))")
    public Response<List<TagTypeDto>> queryTagTypes(@RequestBody TagTypeQuery tagTypeQuery) {
        return uARWFacade.getTagRWResource().searchTagTypes(tagTypeQuery);
    }

    @RequestMapping(value = "add-new-tag-type", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("(hasRole('ROLE_SUPER_ADMIN') and principal.hasDomain('techops')) or " +
            "(hasRole('ROLE_ADMIN') and principal.domainIdSet.contains(#tagTypeParam.domainId))")
    public Response<TagTypeDto> addNewTagType(@RequestBody TagTypeParam tagTypeParam) {
        return uARWFacade.getTagRWResource().addNewTagType(tagTypeParam);
    }

    @RequestMapping(value = "update-tag-type", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("(hasRole('ROLE_SUPER_ADMIN') and principal.hasDomain('techops')) or " +
            "(hasRole('ROLE_ADMIN') and principal.domainIdSet.contains(#tagTypeParam.domainId))")
    public Response<TagTypeDto> updateTagType(@RequestBody TagTypeParam tagTypeParam) {
        return uARWFacade.getTagRWResource().updateTagType(tagTypeParam);
    }

    @RequestMapping(value = "delete-tag-type", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("(hasRole('ROLE_SUPER_ADMIN') and principal.hasDomain('techops')) or " +
            "(hasRole('ROLE_ADMIN') and hasPermission(#tagTypeParam, 'PERM_TAGTYPEID_CHECK'))")
    public Response<Void> deleteTagType(@RequestBody TagTypeParam tagTypeParam) {
        return uARWFacade.getTagRWResource().deleteTagType(tagTypeParam);
    }

    @RequestMapping(value = "/query-tag-user" , method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public Response<List<UserDto>> getTagUser(@RequestBody TagParam tagParam) {
    	UserParam param = new UserParam();
    	List<Integer> tagIds = new ArrayList<Integer>();
    	tagIds.add(tagParam.getId());
    	param.setTagIds(tagIds);
        return uARWFacade.getUserRWResource().searchUserByTagId(param);
    }
}
