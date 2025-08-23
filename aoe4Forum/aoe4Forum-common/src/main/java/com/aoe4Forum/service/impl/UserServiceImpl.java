package com.aoe4Forum.service.impl;

import com.aoe4Forum.component.RedisComponent;
import com.aoe4Forum.entity.User;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.dto.TokenUserInfoDto;
import com.aoe4Forum.entity.dto.UserInfoDto;
import com.aoe4Forum.exception.ErrorParamsException;
import com.aoe4Forum.exception.UserAlreadyExistsException;
import com.aoe4Forum.mapper.UserMapper;
import com.aoe4Forum.redis.RedisUtils;
import com.aoe4Forum.service.UserService;
import com.aoe4Forum.utils.CopyUtil;
import com.aoe4Forum.utils.StringTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;
    @Autowired
    RedisComponent redisComponent;
    @Autowired
    private RedisUtils redisUtils;

    private final ObjectMapper objectMapper;

    public UserServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public User queryById(Long id) {
        List<User> users = userMapper.queryById(id);
        if(users==null|| users.isEmpty()){
            return null;
        }
        return users.get(0);
    }

    @Override
    public User queryByUsername(String username) {
        List<User> users = userMapper.queryByName(username);
        if(users==null|| users.isEmpty()){
            return null;
        }
        return users.get(0);
    }

    @Override
    public int insert(User user) {
        userMapper.insert(user);
        return 0;
    }

    @Override
    public int updateById(User user) {
        return 0;
    }

    @Override
    public int deleteById(int id) {
        return 0;
    }

    @Override
    public Boolean register(String name, String password, String phoneNo) {
        User user = new User();
        user.setName(name);
        user.setPassword(StringTools.encodeByMd5(password));
        user.setPhoneNo(phoneNo);
        user.setJoinTime(LocalDateTime.now());
        user.setLastLoginTime(LocalDateTime.now());
        user.setRole(0);
        user.setFolloweeNums(0);
        user.setFollowerNums(0);
        user.setExp(0);
        // 随机选择一个默认头像
        String randomAvatar = getRandomDefaultAvatar();
        user.setAvatar(randomAvatar);
        
//        查看是否有重复手机号和用户名
        Map<String,String> dupMap = userMapper.checkDuplicate(user);
        if(dupMap!=null){
            if("name".equals(dupMap.get("duplicateName"))){
                throw new UserAlreadyExistsException("用户名");
            }
            if("phoneNo".equals(dupMap.get("duplicatePhoneNo"))){
                throw new UserAlreadyExistsException("手机号");
            }
        }
        userMapper.insert(user);
        return true;
    }

    private String getRandomDefaultAvatar() {
        String[] defaultAvatars = {
            "abbasid_dynasty.png",
            "ayyubids.png", 
            "byzantines.png",
            "chinese.png",
            "delhi_sultanate.png",
            "english.png",
            "french.png",
            "holy_roman_empire.png",
            "house_of_lancaster.png",
            "japanese.png",
            "jeanne_darc.png",
            "knights_templar.png",
            "malians.png",
            "mongols.png",
            "order_of_the_dragon.png",
            "ottomans.png",
            "rus.png",
            "zhu_xis_legacy.png"
        };
        
        // 随机选择一个头像
        Random random = new Random();
        int randomIndex = random.nextInt(defaultAvatars.length);
        return "/defaultImg/" + defaultAvatars[randomIndex];
    }

    @Override
    public TokenUserInfoDto login(String phoneNo, String password, HttpServletRequest request) {
//        根据电话查用户
        List<User> users = userMapper.queryByPhoneNo(phoneNo);
        TokenUserInfoDto tokenUserInfoDto;

        try{
            User user = users.get(0);
//            如果用户不存在或密码不对则抛异常
            if(!user.getPassword().equals(StringTools.encodeByMd5(password))){
                throw new ErrorParamsException("用户名或密码");
            }
//            设置最近登录时间和ip
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(getIpAddress(request));

            userMapper.updateById(user);
            tokenUserInfoDto = CopyUtil.copy(user, TokenUserInfoDto.class);
            redisComponent.saveToken(tokenUserInfoDto);
            redisComponent.saveUserInfo(user);
        }catch (ErrorParamsException e){
            throw new ErrorParamsException("用户名或密码");
        }
//        返回token
        return tokenUserInfoDto;
    }

    @Override
    public String uploadAvatar(MultipartFile file){
        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equals("image/jpeg") ||
                        contentType.equals("image/png")  ||
                        contentType.equals("image/gif")  ||
                        contentType.equals("image/webp"))) {
            throw new ErrorParamsException("图片类型错误");
        }
        if(file.isEmpty()){
            throw new ErrorParamsException("没有文件");
        }
        String uuid = UUID.randomUUID().toString().replaceAll("-","").toLowerCase();
        String originalName = file.getOriginalFilename();
        if(originalName==null || originalName.isEmpty()){
            throw new ErrorParamsException("文件名错误");
        }
        int indexOf = originalName.lastIndexOf(".");
        String suffix = originalName.substring(indexOf);
        String filename = uuid.concat(suffix);
        File dir = new File("./tempImg");
        if (!dir.exists()) {
            dir.mkdirs(); // 创建目录
        }
        String path;
        try{
            String realPath = dir.getCanonicalPath();
            path = realPath + "/" + filename;
            file.transferTo(new File(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return path;
    }

    @Override
    public TokenUserInfoDto confirmAvatar(String path, TokenUserInfoDto tokenUserInfoDto){
        File dir = new File("./avatarImg");
        Long id = tokenUserInfoDto.getId();
        StringBuffer sb = new StringBuffer();
        String fileName = String.valueOf(tokenUserInfoDto.getId());
        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        String remPath;
        if (!dir.exists()) {
            dir.mkdirs(); // 创建目录
        }
        try{
            sb.append(dir.getCanonicalPath());
            sb.append("\\");
            sb.append(fileName);
            sb.append(".");
            sb.append( path.substring(path.lastIndexOf('.') + 1));
            remPath = sb.toString();
            bufferedInputStream = new BufferedInputStream(new FileInputStream(new File(path)));
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(remPath)));
            byte[] bytes = new byte[1024];
            while (bufferedInputStream.read(bytes) != -1) {
                bufferedOutputStream.write(bytes);
            }
            bufferedOutputStream.flush();
            tokenUserInfoDto.setAvatar(remPath);

        }catch (IOException e){
            throw new RuntimeException(e);
        }finally {
            try {
                bufferedOutputStream.close();
                bufferedInputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        User user = new User();
        user.setId(id);
        user.setAvatar(remPath);
        userMapper.updateById(user);
//        删缓存
        redisComponent.cleanUserInfo(tokenUserInfoDto.getId());
        redisComponent.cleanToken(tokenUserInfoDto.getToken());


        return tokenUserInfoDto;
    }

    @Override
    public List<UserInfoDto> batchQueryByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 构建Redis键列表
        List<String> redisKeys = ids.stream()
                .map(id -> Constants.USER_INFO + id)
                .collect(Collectors.toList());

        // 2. 批量查询Redis
        List<String> userJsonList = redisUtils.multiGet(redisKeys);
        List<UserInfoDto> userInfoDtos = new ArrayList<>(userJsonList.size());
        List<Long> missUserIds = new ArrayList<>();
        // 3. 解析Redis结果，收集缺失的用户ID
        for (int i = 0; i < userJsonList.size(); i++) {
            String userJson = userJsonList.get(i);
            Long userId = ids.get(i);
            if (userJson != null) {
                try {
                    // Redis中存储的直接是UserInfoDto的JSON
                    UserInfoDto userInfo = objectMapper.readValue(userJson, UserInfoDto.class);
                    userInfoDtos.add(userInfo);
                } catch (Exception e) {
                    // 反序列化失败视为缺失
                    missUserIds.add(userId);
                }
            } else {
                // Redis中不存在该用户
                missUserIds.add(userId);
            }
        }
        // 4. 处理缓存缺失的用户（从数据库查询）
        if (!missUserIds.isEmpty()) {
            // 从数据库查询原始User对象
            List<User> missUsers = userMapper.batchQueryById(missUserIds);
            if (missUsers != null && !missUsers.isEmpty()) {
                List<UserInfoDto> missUserDtos = new ArrayList<>();

                // 转换数据库User对象为UserInfoDto
                for (User user : missUsers) {
                    UserInfoDto dto = convertToUserInfoDto(user);
                    missUserDtos.add(dto);
                }

                // 将数据库查询结果添加到返回列表
                userInfoDtos.addAll(missUserDtos);

                // 4.2 批量写入Redis缓存（键值对映射）
                Map<String, String> userCacheMap = new HashMap<>(missUserDtos.size());
                for (UserInfoDto dto : missUserDtos) {
                    try {
                        String json = objectMapper.writeValueAsString(dto);
                        userCacheMap.put(Constants.USER_INFO + dto.getId(), json);
                    } catch (JsonProcessingException e) {
                        // 序列化失败不影响主流程，仅日志记录
                    }
                }
                // 批量写入Redis
                if (!userCacheMap.isEmpty()) {
                    redisUtils.multiSet(userCacheMap);
                }
            }
        }

        return userInfoDtos;
    }

    // 新增User转UserInfoDto的转换方法
    private UserInfoDto convertToUserInfoDto(User user) {
        UserInfoDto dto = new UserInfoDto();
        dto.setId(user.getId());
        dto.setUsername(user.getName());
        dto.setFolloweeNums(user.getFolloweeNums());  // 假设User有该字段
        dto.setFollowerNums(user.getFollowerNums());// 假设User有该字段
        dto.setAvatar(user.getAvatar());
        // expireTime通常是缓存相关字段，数据库查询时可设为null或根据业务设置
        dto.setExpireTime(null);
        return dto;
    }

    public static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
