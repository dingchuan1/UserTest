package com.ding.uaa.dao;

import com.ding.uaa.model.PUser;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

//@Mapper
//@Repository
public interface UserDao {

    public PUser getUser(String userid);

    public List<PUser> getAllUser();
}
