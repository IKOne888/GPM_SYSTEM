package com.gmp.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.gmp.entity.SysUser;
import com.gmp.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus配置类
 */
@Configuration
@RequiredArgsConstructor
public class MybatisPlusConfig {

    // 使用 ObjectProvider 延迟获取 Mapper，避免 Mapper -> sqlSessionFactory -> 本配置类的循环依赖
    private final ObjectProvider<SysUserMapper> userMapperProvider;

    /**
     * 分页插件
     * 设置最大分页上限，防止恶意大查询拖垮数据库
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大 100 条
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    /**
     * 自动填充处理器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

                // 填充创建人
                try {
                    if (StpUtil.isLogin()) {
                        Long userId = StpUtil.getLoginIdAsLong();
                        SysUserMapper userMapper = userMapperProvider.getIfAvailable();
                        SysUser user = userMapper != null ? userMapper.selectById(userId) : null;
                        if (user != null) {
                            this.strictInsertFill(metaObject, "createBy", String.class, user.getUsername());
                            this.strictInsertFill(metaObject, "updateBy", String.class, user.getUsername());
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

                // 填充更新人
                try {
                    if (StpUtil.isLogin()) {
                        Long userId = StpUtil.getLoginIdAsLong();
                        SysUserMapper userMapper = userMapperProvider.getIfAvailable();
                        SysUser user = userMapper != null ? userMapper.selectById(userId) : null;
                        if (user != null) {
                            this.strictUpdateFill(metaObject, "updateBy", String.class, user.getUsername());
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        };
    }
}
