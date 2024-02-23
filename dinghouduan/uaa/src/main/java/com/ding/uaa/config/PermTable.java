package com.ding.uaa.config;

import org.springframework.stereotype.Component;

@Component
public class PermTable {
    //后期可以写在一个配置文件里面方便维护
    private String[] permNames = new String[3];
    private String[] permKeys = new String[3];

    private String[] RoleNames = new String[4];
    private String[] RoleKeys = new String[4];

    public PermTable(){
        RoleKeys[0] = "admin";
        RoleNames[0] = "adminIndex";

        RoleKeys[1] = "user";
        RoleNames[1] = "userindex";

        RoleKeys[2] = "admin";
        RoleNames[2] = "upLoadFile";

        RoleKeys[3] = "admin";
        RoleNames[3] = "downLoadFile";
    }
    public String getPermKey(String Name){
        String res = "";
        for (int i=0;i<permNames.length;i++){
            if(permNames[i].equals(Name)){
                res = permKeys[i];
            }
        }
        return res;
    }

    public String getRoleKey(String Name){
        String res = "";
        for (int i=0;i<RoleNames.length;i++){
            if(RoleNames[i].equals(Name)){
                res = RoleKeys[i];
            }
        }
        return res;
    }
}
