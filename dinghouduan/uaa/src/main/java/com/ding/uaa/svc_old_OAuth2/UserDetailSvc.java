package com.ding.uaa.svc_old_OAuth2;

//import com.ding.uaa.model.PUser;
//import com.ding.uaa.dao.UserDao;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
public class UserDetailSvc{

}
//@Component
//public class UserDetailSvc implements UserDetailsService {
//
//    @Autowired
//    private UserDao userDao;
//
//    @Override
//    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
//        List<GrantedAuthority> grantedAuths = new ArrayList<>();
//        PUser pUser = userDao.getUser(s);
//        if(Objects.isNull(pUser)){
//            throw new UsernameNotFoundException("没有找到该用户" + s);
//        }
//        grantedAuths.add(new SimpleGrantedAuthority(pUser.getAuthority()+""));
//
//        return new User(pUser.getUserid(),pUser.getPassword(),grantedAuths);
//    }
//}
