package com.ding.uaa.dao;

import com.ding.uaa.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface UserDao {

    public User getUser(String userid);

    public List<User> getAllUser();
}
