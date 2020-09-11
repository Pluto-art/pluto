package com.lyp.config;

import com.lyp.shiro.UserRealm;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @ClassName shiroConfig
 * @Author 你鹏哥
 * @Date 2020/1/10 19:54
 **/

public class shiroConfig {



    /**
     *
     * @return
     */
    @Bean
    public SessionManager sessionManager() {
        DefaultSessionManager sessionManager = new DefaultSessionManager();
        // 设置session过期时间
        sessionManager.setGlobalSessionTimeout(60 * 60 * 100);
        // 扫描session线程，清理超时会话
        sessionManager.setSessionValidationSchedulerEnabled(true);
        return sessionManager;
    }
    @Bean
    public SecurityManager securityManager(UserRealm userRealm,SessionManager sessionManagre){
        DefaultWebSecurityManager defaultWebSecurityManager = new DefaultWebSecurityManager();
        defaultWebSecurityManager.setRealm(userRealm);
        defaultWebSecurityManager.setSessionManager(sessionManagre);
        return defaultWebSecurityManager;

    }
    @Bean
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager){
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        shiroFilter.setSecurityManager(securityManager);
        shiroFilter.setLoginUrl("/admin/login.html"); //登入页面
        shiroFilter.setSuccessUrl("/admin/index.html"); //登入成功页面
        shiroFilter.setUnauthorizedUrl("/"); //未授权
        Map<String,String> FilterMap = new LinkedHashMap<>();
        FilterMap.put("/admin/**","anon");
        FilterMap.put("/blog/**","anon");
        FilterMap.put("/error/**","anon");
        FilterMap.put("/admin/login.html","anon");
        FilterMap.put("/admin","anon");
        FilterMap.put("/admin/login","anon");
        //FilterMap.put("/**","authc");
        shiroFilter.setFilterChainDefinitionMap(FilterMap);
        return shiroFilter;
    }
    @Bean
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor(){
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator(){
        DefaultAdvisorAutoProxyCreator proxyCreator = new DefaultAdvisorAutoProxyCreator();
        proxyCreator.setProxyTargetClass(true);
        return proxyCreator;
    }
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager){
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;

    }

}
