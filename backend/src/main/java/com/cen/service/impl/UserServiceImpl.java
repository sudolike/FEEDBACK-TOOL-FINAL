package com.cen.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.common.Constants;
import com.cen.common.Result;
import com.cen.common.lang.Const;
import com.cen.controller.dto.UserDTO;
import com.cen.entity.User;
import com.cen.exception.ServiceException;
import com.cen.mapper.UserMapper;
import com.cen.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cen.utils.RedisUtil;
import com.cen.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private RedisUtil redisUtil;

    /**
     * 用户登录
     * 1. 校验用户名和密码
     * 2. 验证用户角色权限
     * 3. 生成JWT token
     */
    @Override
    public UserDTO login(UserDTO userDTO) {
        //获得存储在redis中的验证码
//        String redisCode = (String) redisUtil.get(Const.CAPTCHA_KEY);
//        if(redisCode== null || !redisCode.equals(userDTO.getCode())){
//            throw new ServiceException(Constants.CODE_402,"验证码错误");
//        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username",userDTO.getUsername());
        queryWrapper.eq("password",userDTO.getPassword());
        User one = getOne(queryWrapper);

        if(one!=null){
            // 特殊处理admin用户
            if (!userDTO.getUsername().equals("admin")) {
                // 验证用户角色是否匹配
                queryWrapper.eq("role",userDTO.getRole());
                User one2 = getOne(queryWrapper);
                if(one2==null){
                    throw new ServiceException(Constants.CODE_402,"该用户不是" + userDTO.getRole() + "账号，请切换身份登录");
                }
            } else {
                userDTO.setRole("admin");
            }
            //把数据库查到的one数据拷贝到userDTO里  true是忽略大小写
            BeanUtil.copyProperties(one,userDTO,true);
            // 生成JWT token
            String token = TokenUtils.getToken(one);
            userDTO.setToken(token);
            return userDTO;
        }else{
            throw new ServiceException(Constants.CODE_402,"用户名或密码错误");
        }
    }

    //用户新增的时候判断一下是否存在
    /**
     * 用户注册
     *  1. 严禁 admin 角色通过开放接口注册（管理员账号仅由系统内置）
     *  2. 强制规范化角色字段，缺省按学生处理
     *  3. 检查用户名是否已存在
     *  4. 校验用户名/密码长度与基本规范
     *  5. 创建新用户
     */
    @Override
    public Object register(UserDTO userDTO) {
        String role = userDTO.getRole();
        if (role == null || role.trim().isEmpty()) {
            role = "student";
        }
        role = role.trim().toLowerCase();
        if (!"student".equals(role) && !"teacher".equals(role)) {
            throw new ServiceException(Constants.CODE_400,
                    "管理员账号不开放注册，请使用学生或教师身份");
        }
        if (userDTO.getUsername() == null || userDTO.getUsername().trim().length() < 3) {
            throw new ServiceException(Constants.CODE_400, "用户名至少 3 个字符");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().length() < 4) {
            throw new ServiceException(Constants.CODE_400, "密码至少 4 个字符");
        }
        String trimmedUsername = userDTO.getUsername().trim();
        if (trimmedUsername.toLowerCase().startsWith("admin")) {
            throw new ServiceException(Constants.CODE_400,
                    "用户名不能以 admin 开头，该前缀已被系统保留");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", trimmedUsername);
        User one = getOne(queryWrapper);
        if (one != null) {
            throw new ServiceException(Constants.CODE_500, "用户已存在");
        }
        one = new User();
        BeanUtil.copyProperties(userDTO, one, true);
        one.setUsername(trimmedUsername);
        one.setRole(role);
        one.setRoleId("teacher".equals(role) ? 3 : 2);
        one.setStatus(1);
        save(one);
        return one;
    }

    /**
     * 保存用户信息
     * 1. 新增用户时检查用户名是否存在
     * 2. 更新用户时检查用户名是否重复
     */
    @Override
    public Result saveUser(User user) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username",user.getUsername());
        User one = getOne(queryWrapper);
        if (one==null){
            return Result.success(saveOrUpdate(user));
        }else{
            // 更新用户信息时的处理
            if(user.getId()!=null){
                QueryWrapper<User> queryWrapper2 = new QueryWrapper<>();
                queryWrapper2.eq("id",user.getId());
                User upOne = getOne(queryWrapper2);
                if(user.getUsername().equals(upOne.getUsername())){
                    return Result.success(saveOrUpdate(user));
                }else{
                    throw new ServiceException(Constants.CODE_500,"修改失败,用户已存在");
                }
            }else{
                throw new ServiceException(Constants.CODE_500,"用户已存在");
            }
        }
    }

    /**
     * 修改用户密码
     * 1. 检查新密码是否与旧密码相同
     * 2. 更新密码
     */
    @Override
    public Result editPow(User user) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",user.getId());
        User one = getOne(queryWrapper);
        String pow = one.getPassword();
        String newPow = user.getPassword();
        if(one.getPassword().equals(user.getPassword())){
            throw new ServiceException(Constants.CODE_500,"与旧密码重复");
        }else{
            return Result.success(saveOrUpdate(user));
        }
    }
}
