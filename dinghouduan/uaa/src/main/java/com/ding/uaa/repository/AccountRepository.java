package com.ding.uaa.repository;

import com.ding.uaa.model.User;
import org.springframework.stereotype.Repository;

public interface AccountRepository extends Repository {
    User findByUserid(String name);
}
