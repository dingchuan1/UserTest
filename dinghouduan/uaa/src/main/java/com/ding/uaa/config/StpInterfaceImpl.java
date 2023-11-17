package com.ding.uaa.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限验证接口扩展
 */
@Component    // 保证此类被SpringBoot扫描，完成Sa-Token的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 本list仅做模拟，实际项目中要根据具体业务逻辑来查询权限
        //目前没有连接数据库，权限写死
        List<String> list = new ArrayList<String>();
        if("test".equals(loginId)){
            list.add("userservice");
            list.add("userservice.add");
            list.add("userservice.update");
            list.add("userservice.get");
            // list.add("user.delete");
            //list.add("art.*");
            return list;
        }
        if("DC".equals(loginId)){
            list.add("adminservice");
            list.add("adminservice.add");
            list.add("adminservice.update");
            list.add("adminservice.get");
            // list.add("user.delete");
            //list.add("art.*");
            return list;
        }
        return null;
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 本list仅做模拟，实际项目中要根据具体业务逻辑来查询权限
        //目前没有连接数据库，权限写死
        List<String> list = new ArrayList<String>();
        if("DC".equals(loginId)) {
            list.add("admin");
            list.add("super-admin");
            return list;
        }if("test".equals(loginId)) {
            list.add("user");
            return list;
        }
        return null;
    }
}
