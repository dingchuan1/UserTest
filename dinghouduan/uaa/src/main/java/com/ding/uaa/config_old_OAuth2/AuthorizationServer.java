package com.ding.uaa.config_old_OAuth2;

//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
//import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
//import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
//import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
//import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
//import org.springframework.security.oauth2.provider.ClientDetailsService;
//import org.springframework.security.oauth2.provider.approval.ApprovalStore;
//import org.springframework.security.oauth2.provider.approval.JdbcApprovalStore;
//import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
//import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
//import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices;
//import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
//import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
//import org.springframework.security.oauth2.provider.token.TokenStore;
//import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import javax.sql.DataSource;

public class AuthorizationServer{

}
//@Configuration
//@EnableAuthorizationServer
//public class AuthorizationServer extends AuthorizationServerConfigurerAdapter {
//    //数据库连接池对象，spring boot配置完成后自动注入
//    @Autowired
//    private DataSource dataSource;
//
//    //授权模式专用对象，在security配置中注入容器
//    @Autowired
//    private AuthenticationManager authenticationManager;
//
//    //客户端信息来源
//    @Bean
//    public ClientDetailsService jdbcClientDetaService(){
//        return  new JdbcClientDetailsService(dataSource);
//    }
//
//    //token 保存策略，指的是生成的Token要往哪里存储
//    //有四种  jdbcTokenStore,InMemoryTokenStore,JwkTokenStore,RedisTokenStore
//    @Bean
//    public TokenStore tokenStore(){
//        return new JdbcTokenStore(dataSource);
//    }
//
//    //配置客户端详细信息服务
//    //指定客户端信息的数据库来源
//    @Override
//    public void configure(ClientDetailsServiceConfigurer clients) throws Exception{
//        clients.withClientDetails(jdbcClientDetaService());
//    }
//
//    // 授权信息保存策略
//    @Bean
//    public ApprovalStore approvalStore(){
//        return new JdbcApprovalStore(dataSource);
//    }
//
//    //授权码模式数据来源
//    @Bean
//    public AuthorizationCodeServices authorizationCodeServices(){
//        return new JdbcAuthorizationCodeServices(dataSource);
//    }
//
//    //令牌服务配置（令牌管理）
//    @Bean
//    public AuthorizationServerTokenServices tokenServices(){
//        DefaultTokenServices tokenServices = new DefaultTokenServices();
//        //token保存策略
//        tokenServices.setTokenStore(tokenStore());
//        //支持刷新模式
//        tokenServices.setSupportRefreshToken(true);
//        //客户端信息来源
//        tokenServices.setClientDetailsService(jdbcClientDetaService());
//        //token自定义有效期，默认12小时
//        tokenServices.setAccessTokenValiditySeconds(60 * 60 * 12);
//        //refresh token 自定义有效期，默认 30天
//        tokenServices.setRefreshTokenValiditySeconds(60 * 60 * 24 * 7);
//
//        return tokenServices;
//    }
//
//    //检查token的策略，即配置令牌端点的安全约束
//    //就是这个端点谁能访问谁不能访问
//    @Override
//    public void configure(AuthorizationServerSecurityConfigurer securtiy){
//        //此时指 endpoint 完全公开
//        securtiy.tokenKeyAccess("permitAll()");
//        //checktoken 这个endpoint完全公开
//        securtiy.checkTokenAccess("permitAll()");
//        //允许表单认证，这个如果配置支持allowFormAuthenticationForClients的，且url中有client_id和client_secret的会走ClientCredentialsTokenEndpointFilter来保护
//        //如果没有支持allowFormAuthenticationForClients或者支持但是url中没有client_id和client_secret的，走basic认证保护
//        securtiy.allowFormAuthenticationForClients();
//
//    }
//
//    //OAuth2的主配置信息
//    @Override
//    public void configure(AuthorizationServerEndpointsConfigurer endpoints){
//        endpoints.approvalStore(approvalStore())
//                .authenticationManager(authenticationManager)//密码模式需要
//                .authorizationCodeServices(authorizationCodeServices())
//                .tokenServices(tokenServices());
//    }
//}
