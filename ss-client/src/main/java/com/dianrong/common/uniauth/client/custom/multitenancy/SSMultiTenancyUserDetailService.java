package com.dianrong.common.uniauth.client.custom.multitenancy;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.dianrong.common.uniauth.client.custom.UserExtInfo;
import com.dianrong.common.uniauth.client.custom.UserInfoCallBack;
import com.dianrong.common.uniauth.client.custom.callback.LoadUserFailedCallBack;
import com.dianrong.common.uniauth.client.custom.callback.LoadUserSuccessCallBack;
import com.dianrong.common.uniauth.client.custom.callback.support.MultipleLoadUserFailedCallBackDelegate;
import com.dianrong.common.uniauth.client.custom.callback.support.MultipleLoadUserSuccessCallBackDelegate;
import com.dianrong.common.uniauth.client.custom.model.UserExtInfoParam;
import com.dianrong.common.uniauth.client.support.CheckDomainDefine;
import com.dianrong.common.uniauth.common.bean.Response;
import com.dianrong.common.uniauth.common.bean.dto.DomainDto;
import com.dianrong.common.uniauth.common.bean.dto.PermissionDto;
import com.dianrong.common.uniauth.common.bean.dto.RoleDto;
import com.dianrong.common.uniauth.common.bean.dto.UserDetailDto;
import com.dianrong.common.uniauth.common.bean.dto.UserDto;
import com.dianrong.common.uniauth.common.bean.request.LoginParam;
import com.dianrong.common.uniauth.common.client.DomainDefine;
import com.dianrong.common.uniauth.common.client.UniClientFacade;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SSMultiTenancyUserDetailService implements MultiTenancyUserDetailsService, ApplicationContextAware, InitializingBean {

    @Autowired
    private UniClientFacade uniClientFacade;

    @Autowired
    private DomainDefine domainDefine;

    @Autowired(required = false)
    private UserInfoCallBack userInfoCallBack;

    /**
     * spring的applicationContext引用
     */
    private ApplicationContext applicationContext;

    /**
     * load用户信息失败的回调函数
     */
    private LoadUserFailedCallBack loadUserFailedCallBack;

    /**
     * load用户信息成功的回调函数
     */
    private LoadUserSuccessCallBack loadUserSuccessCallBack;

    @Override
    public UserDetails loadUserByUsername(String userName, long tenancyId) throws UsernameNotFoundException, DataAccessException {
        String currentDomainCode = domainDefine.getDomainCode();
        String userInfoClass = domainDefine.getUserInfoClass();
        CheckDomainDefine.checkDomainDefine(currentDomainCode);
        if (userName == null || "".equals(userName.toString())) {
            UsernameNotFoundException t = new UsernameNotFoundException(userName + " not found");
            loadUserFailedCallBack.loadUserFailed(userName, tenancyId, t);
            throw t;
        } else {
            LoginParam loginParam = new LoginParam();
            loginParam.setAccount(userName);
            loginParam.setTenancyId(tenancyId);
            // not login, need set tenancyId manually
            Response<UserDetailDto> response = uniClientFacade.getUserResource().getUserDetailInfo(loginParam);
            UserDetailDto userDetailDto = response.getData();

            if (userDetailDto == null) {
                UsernameNotFoundException t = new UsernameNotFoundException(userName + " not found");
                loadUserFailedCallBack.loadUserFailed(userName, tenancyId, t);
                throw t;
            } else {
                UserDto userDto = userDetailDto.getUserDto();
                Long id = userDto.getId();
                List<DomainDto> domainDtoList = userDetailDto.getDomainList();
                // DomainDto currentDomainDto = null;
                Map<String, UserExtInfoParam> userExtInfos = new HashMap<>();
                if (domainDtoList != null && !domainDtoList.isEmpty()) {
                    List<DomainDto> tempDomainDtoList = null;
                    if (!domainDefine.isUseAllDomainUserInfoShareMode()) {
                        tempDomainDtoList = new ArrayList<DomainDto>();
                        for (DomainDto domainDto : domainDtoList) {
                            String domainCode = domainDto.getCode();
                            if (currentDomainCode.equals(domainCode)) {
                                tempDomainDtoList.add(domainDto);
                                break;
                            }
                        }
                    } else {
                        tempDomainDtoList = domainDtoList;
                    }
                    for (DomainDto domainDto : tempDomainDtoList) {
                        Map<String, Set<String>> permMap = new HashMap<String, Set<String>>();
                        Map<String, Set<PermissionDto>> permDtoMap = new HashMap<>();
                        Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
                        List<RoleDto> roleDtoList = domainDto.getRoleList();
                        if (roleDtoList != null && !roleDtoList.isEmpty()) {
                            for (RoleDto roleDto : roleDtoList) {
                                String roleCode = roleDto.getRoleCode();
                                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleCode);
                                authorities.add(authority);
                                mergePermMap(permMap, roleDto.getPermMap());
                                mergePermMap(permDtoMap, roleDto.getPermDtoMap());
                            }
                        }
                        UserExtInfoParam userExtInfoParam = new UserExtInfoParam();
                        userExtInfoParam.setUsername(userName).setPassword("fake_password").setEnabled(true).setAccountNonExpired(true).setCredentialsNonExpired(true)
                                .setAccountNonLocked(true).setAuthorities(authorities).setId(id).setUserDto(userDto).setDomainDto(domainDto).setPermMap(permMap)
                                .setPermDtoMap(permDtoMap);
                        userExtInfos.put(domainDto.getCode(), userExtInfoParam);
                    }
                }
                UserExtInfoParam currentDomainUserInfo = userExtInfos.get(currentDomainCode);
                if (currentDomainUserInfo == null) {
                    UserExtInfoParam userExtInfoParam = new UserExtInfoParam();
                    userExtInfoParam.setUsername(userName).setPassword("fake_password").setEnabled(true).setAccountNonExpired(true).setCredentialsNonExpired(true)
                            .setAccountNonLocked(true).setAuthorities(new ArrayList<GrantedAuthority>()).setId(id).setUserDto(userDto)
                            .setDomainDto(new DomainDto().setCode(DomainDefine.getStaticDomainCode())).setPermMap(new HashMap<String, Set<String>>())
                            .setPermDtoMap(new HashMap<String, Set<PermissionDto>>());
                    userExtInfos.put(currentDomainCode, userExtInfoParam);
                    currentDomainUserInfo = userExtInfos.get(currentDomainCode);
                }

                UserExtInfo userExtInfo;

                if (userInfoClass == null || "".equals(userInfoClass.trim())) {
                    userExtInfo = UserExtInfo.build(currentDomainUserInfo, userExtInfos);
                } else {
                    try {
                        Class<?> clazz = Class.forName(userInfoClass);
                        Constructor<?> construct = clazz.getConstructor(String.class, String.class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Collection.class,
                                Long.class, UserDto.class, DomainDto.class, Map.class, Map.class);
                        UserExtInfo customeDefineUserExtInfo =
                                (UserExtInfo) construct.newInstance(currentDomainUserInfo.getUsername(), currentDomainUserInfo.getPassword(), currentDomainUserInfo.isEnabled(),
                                        currentDomainUserInfo.isAccountNonExpired(), currentDomainUserInfo.isCredentialsNonExpired(), currentDomainUserInfo.isAccountNonLocked(),
                                        currentDomainUserInfo.getAuthorities(), currentDomainUserInfo.getId(), currentDomainUserInfo.getUserDto(),
                                        currentDomainUserInfo.getDomainDto(), currentDomainUserInfo.getPermMap(), currentDomainUserInfo.getPermDtoMap());
                        if (userInfoCallBack != null) {
                            userInfoCallBack.fill(customeDefineUserExtInfo);
                        }
                        userExtInfo = customeDefineUserExtInfo;
                    } catch (Exception e) {
                        log.error("Prepare to use ss-client's UserExtInfo, not the subsystem's customized one, possible reasons:\n (1) " + userInfoClass + " not found. \n (2) "
                                + userInfoClass + " is not a instance of UserExtInfo.\n (3) userInfoCallBack.fill(userExtInfo) error.", e);
                        userExtInfo = UserExtInfo.build(currentDomainUserInfo, userExtInfos);
                    }
                }

                // 登陆成功之后, 回调
                loadUserSuccessCallBack.loadUserSuccess(userExtInfo);
                return userExtInfo;
            }
        }
    }

    private <T> void mergePermMap(Map<String, Set<T>> permMap, Map<String, Set<T>> subPermMap) {
        Set<Entry<String, Set<T>>> subEntrySet = subPermMap.entrySet();
        Iterator<Entry<String, Set<T>>> subEntryIterator = subEntrySet.iterator();
        while (subEntryIterator.hasNext()) {
            Entry<String, Set<T>> subEntry = subEntryIterator.next();
            String permTypeName = subEntry.getKey();
            Set<T> permValueSet = subEntry.getValue();

            if (permMap.containsKey(permTypeName)) {
                permMap.get(permTypeName).addAll(permValueSet);
            } else {
                permMap.put(permTypeName, permValueSet);
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 加载loadUserFailedCallBack和loadUserSuccessCallBack
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Comparator<Object> comparator = OrderComparator.INSTANCE;
        
        String[] loadUserFailedCallBackBeanNames = this.applicationContext.getBeanNamesForType(LoadUserFailedCallBack.class);
        List<LoadUserFailedCallBack> loadUserFailedCallBackList = Lists.newArrayList();
        for (String failedCallBackName: loadUserFailedCallBackBeanNames) {
            loadUserFailedCallBackList.add(this.applicationContext.getBean(failedCallBackName, LoadUserFailedCallBack.class));
        }
        Collections.sort(loadUserFailedCallBackList, comparator);
        this.loadUserFailedCallBack = new MultipleLoadUserFailedCallBackDelegate(loadUserFailedCallBackList);
        
        // success callback
        String[] loadUserSuccessCallBackBeanNames = this.applicationContext.getBeanNamesForType(LoadUserSuccessCallBack.class);
        List<LoadUserSuccessCallBack> loadUserSuccessCallBackList = Lists.newArrayList();
        for (String successCallBackName: loadUserSuccessCallBackBeanNames) {
            loadUserSuccessCallBackList.add(this.applicationContext.getBean(successCallBackName, LoadUserSuccessCallBack.class));
        }
        Collections.sort(loadUserSuccessCallBackList, comparator);
        this.loadUserSuccessCallBack = new MultipleLoadUserSuccessCallBackDelegate(loadUserSuccessCallBackList);
    }
}
