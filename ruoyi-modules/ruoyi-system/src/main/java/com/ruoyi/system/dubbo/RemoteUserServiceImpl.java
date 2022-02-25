package com.ruoyi.system.dubbo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.enums.UserStatus;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.exception.user.UserException;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.api.model.LoginUser;
import com.ruoyi.system.api.model.RoleDTO;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysPermissionService;
import com.ruoyi.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 操作日志记录
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteUserServiceImpl implements RemoteUserService {

    private final ISysUserService userService;
    private final ISysPermissionService permissionService;
    private final ISysConfigService configService;

    @Override
    public LoginUser getUserInfo(String username) throws UserException {
        SysUser sysUser = userService.selectUserByUserName(username);
        if (ObjectUtil.isNull(sysUser)) {
            throw new UserException("user.not.exists", username);
        }
        if (UserStatus.DELETED.getCode().equals(sysUser.getDelFlag())) {
            throw new UserException("user.password.delete", username);
        }
        if (UserStatus.DISABLE.getCode().equals(sysUser.getStatus())) {
            throw new UserException("user.blocked", username);
        }
        // 角色集合
        Set<String> rolePermission = permissionService.getRolePermission(sysUser.getUserId());
        // 权限集合
        Set<String> menuPermissions = permissionService.getMenuPermission(sysUser.getUserId());
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(sysUser.getUserId());
        loginUser.setDeptId(sysUser.getDeptId());
        loginUser.setUsername(sysUser.getUserName());
        loginUser.setPassword(sysUser.getPassword());
        loginUser.setUserType(sysUser.getUserType());
        loginUser.setDeptName(sysUser.getDept().getDeptName());
        loginUser.setMenuPermission(menuPermissions);
        loginUser.setRolePermission(rolePermission);
        List<RoleDTO> roles = BeanUtil.copyToList(sysUser.getRoles(), RoleDTO.class);
        loginUser.setRoles(roles);
        return loginUser;
    }

    @Override
    public Boolean registerUserInfo(SysUser sysUser) {
        String username = sysUser.getUserName();
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser")))) {
            throw new ServiceException("当前系统没有开启注册功能");
        }
        if (UserConstants.NOT_UNIQUE.equals(userService.checkUserNameUnique(username))) {
            throw new UserException("user.register.save.error", username);
        }
        return userService.registerUser(sysUser);
    }

}