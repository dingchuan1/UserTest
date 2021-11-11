package com.ding.uaa.repository;

import com.ding.uaa.model.PUser;
import org.springframework.stereotype.Repository;

public interface AccountRepository extends Repository {
    PUser findByUserid(String name);
}
