package com.aoe4Forum.web.controller;

import com.aoe4Forum.component.RedisComponent;
import com.aoe4Forum.entity.ResponseVO;
import com.aoe4Forum.entity.User;
import com.aoe4Forum.entity.constans.Constants;
import com.aoe4Forum.entity.request.LoginRequest;
import com.aoe4Forum.entity.request.RegisterRequest;
import com.aoe4Forum.entity.dto.TokenUserInfoDto;
import com.aoe4Forum.entity.dto.UserInfoDto;
import com.aoe4Forum.exception.ErrorParamsException;
import com.aoe4Forum.service.UserService;
import com.aoe4Forum.service.impl.UserServiceImpl;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

import static com.aoe4Forum.entity.constans.Constants.*;

@CrossOrigin
@RestController
@RequestMapping("/account")
@Validated
public class AccountController extends ABaseController{
    @Autowired
    private UserService userService;

    @Resource
    private RedisComponent redisComponent;
    @Autowired
    private UserServiceImpl userServiceImpl;

    @GetMapping("/checkCode")
    public ResponseVO<Map<String,String>> checkCode(){
        //生成验证码
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 48);
        String code = captcha.text();
//        保存至redis
        String checkCodeKey = redisComponent.saveCheckCode(code);
        String checkCodeBase64 = captcha.toBase64();

        Map<String,String> map = new HashMap<>();
        map.put("checkCodeKey",checkCodeKey);
        map.put("checkCodeBase64",checkCodeBase64);

        return ResponseVO.success(code,map);
    }

//    public ResponseVO<String> register(@NotEmpty @Size(max = 128) String name,
//                                       @NotEmpty @Pattern(regexp = PASSWORD_REGEX) String password,
//                                       @NotEmpty @Length(min=11,max=11) String phoneNo,
//                                       @RequestParam String checkCode)
    @PostMapping("/register")
    public ResponseVO<Map<String,String>> register(@Valid @RequestBody RegisterRequest registerRequest)
                                                    {
        //获取并校验验证码
        String checkCodeKey = registerRequest.getCheckCodeKey();
        String checkCode =  registerRequest.getCheckCode();
        String name = registerRequest.getName();
        String password = registerRequest.getPassword();
        String phoneNo = registerRequest.getPhoneNo();
        try {
            if (!checkCode.equals(redisComponent.getCodeKey(checkCodeKey))) {
                throw new ErrorParamsException("验证码");
            }
            userService.register(name, password, phoneNo);
        }
        finally{
            redisComponent.cleanCheckCode(checkCodeKey);
        }

        return ResponseVO.success("注册成功",null);
    }

    @PostMapping(value = "/login")
    public ResponseVO<Map<String, String>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                 HttpServletResponse  response,
                                                 HttpServletRequest request
                                                 ) {
        String phoneNo = loginRequest.getPhoneNo();
        String password = loginRequest.getPassword();
        Cookie[] cookies = request.getCookies();
        if(cookies!=null){
            for(Cookie c : cookies) {
                if(c.getName().equals(Constants.WEB_TOKEN)) {
                    redisComponent.cleanToken(c.getValue());
                }
            }
        }

        TokenUserInfoDto tokenUserInfoDto = userService.login(phoneNo,password,request);
        saveToken2Cookie(response,tokenUserInfoDto.getToken());
        // 可能添加扩展信息
        return ResponseVO.success("登录成功",null);
    }

    @GetMapping("/autologin")
    public ResponseVO<Map<String,String>> autoLogin(HttpServletResponse response){
        TokenUserInfoDto tokenUserInfoDto = getTokenFromCookie();
        if(tokenUserInfoDto==null){
            return ResponseVO.error("102",null);
        }
        if(tokenUserInfoDto.getExpireTime()-System.currentTimeMillis()< REDIS_KEY_EXPIRES_ONE_WEEK/7*3){
            redisComponent.saveToken(tokenUserInfoDto);
            saveToken2Cookie(response,tokenUserInfoDto.getToken());
        }
        return ResponseVO.success();
    }

    @GetMapping("/logout")
    public ResponseVO<Map<String,String>> logout(HttpServletResponse response){
        cleanCookie(response);
        return ResponseVO.success("1",null);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/profile")
    public ResponseVO<TokenUserInfoDto> profile(HttpServletRequest request) {
        TokenUserInfoDto userInfo = getTokenFromCookie();
        if (userInfo == null) {
            return ResponseVO.error("401", "未登录或token无效");
        }
        return ResponseVO.success("获取成功", userInfo);
    }

    @GetMapping("/userInfo")
    public ResponseVO<UserInfoDto> userInfo(Long userId) {
        UserInfoDto userInfoDto = redisComponent.getUserInfo(userId);
//        空则查数据库
        if(userInfoDto==null){
            User user = userServiceImpl.queryById(userId);
            if(user==null){
                return  ResponseVO.error("401", "没有该用户");
            }
            userInfoDto = new UserInfoDto();
            userInfoDto.setAvatar(user.getAvatar());
            userInfoDto.setId(userId);
            userInfoDto.setUsername(user.getName());
            userInfoDto.setFolloweeNums(user.getFolloweeNums());
            userInfoDto.setFollowerNums(user.getFollowerNums());
            redisComponent.saveUserInfo(user);
        }
        return ResponseVO.success("获取成功", userInfoDto);
    }


    @PostMapping("/uploadAvatar")
    public ResponseVO<Map<String,String>> uploadAvatar(@RequestParam("multipartFile") MultipartFile file) {
        System.out.println("Received file: " + file.getOriginalFilename() + ", size: " + file.getSize() + " bytes");
        String path = userServiceImpl.uploadAvatar(file);
        if(path==null){
            return ResponseVO.error("401", "上传失败");
        }
        Map<String,String> map = new HashMap<>();
        System.out.println("Avatar path: " + path);
        map.put("path",path);
        return ResponseVO.success("上传成功",map);
    }

    @GetMapping("/confirmAvatar")
    public ResponseVO<Map<String,String>> confirmAvatar(@RequestParam("path") String path,
                                                        @RequestParam("id") Long id
                                                        ){
        TokenUserInfoDto tokenUserInfoDto = getTokenFromCookie();
        tokenUserInfoDto = userServiceImpl.confirmAvatar(path, tokenUserInfoDto);
        redisComponent.changeToken(tokenUserInfoDto);
        return ResponseVO.success("更换头像成功",null);
    }
}
