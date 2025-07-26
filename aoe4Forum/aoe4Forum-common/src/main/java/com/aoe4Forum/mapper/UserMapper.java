package com.aoe4Forum.mapper;

import com.aoe4Forum.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    List<User> queryById(Long id);
    List<User> batchQueryById(@Param("ids") List<Long> ids);
    List<User> queryByName(String name);
    List<User> queryByPhoneNo(String phoneNo);
    Map<String, String> checkDuplicate(User user);
    int insert(User user);
    int updateById(User user);
    int deleteById(int id);
    void updateFollowerNumsDelta(@Param("id") Long Id, @Param("delta") int delta);
    void updateFolloweeNumsDelta(@Param("id") Long Id, @Param("delta") int delta);
}
