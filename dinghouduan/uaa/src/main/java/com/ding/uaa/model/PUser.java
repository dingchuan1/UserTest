package com.ding.uaa.model;

import lombok.Data;

import java.io.Serializable;

//分布式实体类都要实现序列化，不然在传输时会报错
//@Data
public class PUser implements Serializable {

    //主键key
    private String userid;
    private String name;
    private String password;
    private int authority;

}
