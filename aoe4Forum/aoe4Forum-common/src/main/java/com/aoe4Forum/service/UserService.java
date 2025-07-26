package com.aoe4Forum.service;

import com.aoe4Forum.entity.User;
import com.aoe4Forum.entity.dto.TokenUserInfoDto;
import com.aoe4Forum.entity.dto.UserInfoDto;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface UserService {
    User queryById(Long id);
    User queryByUsername(String username);
    int insert(User user);
    int updateById(User user);
    int deleteById(int id);
    Boolean register(String name,String password,String phoneNo);
    TokenUserInfoDto login(String phoneNo, String password, HttpServletRequest request);
    String uploadAvatar(MultipartFile file);
    TokenUserInfoDto confirmAvatar(String path,TokenUserInfoDto tokenUserInfoDto);
    List<UserInfoDto> batchQueryByIds(List<Long> ids);
}
