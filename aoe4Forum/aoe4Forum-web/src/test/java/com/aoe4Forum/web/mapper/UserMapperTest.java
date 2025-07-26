package com.aoe4Forum.web.mapper;

import com.aoe4Forum.entity.User;
import com.aoe4Forum.mapper.UserMapper;
import com.aoe4Forum.web.aoe4ForumWebRunApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest(classes = aoe4ForumWebRunApplication.class)
@RunWith(SpringRunner.class)
public class UserMapperTest {
    @Resource
    private UserMapper userMapper;

    @Test
    public void queryByPhoneNoTest(){
        String phoneNo = "13070143464";
        List<User> users1 = userMapper.queryByPhoneNo(phoneNo);
        System.out.println(users1.get(0).getName());
        List<User> users2 = userMapper.queryByPhoneNo(phoneNo);
    }
}
