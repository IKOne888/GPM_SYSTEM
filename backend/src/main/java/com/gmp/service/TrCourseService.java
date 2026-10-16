package com.gmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gmp.dto.CourseAddDTO;
import com.gmp.dto.CourseUpdateDTO;
import com.gmp.dto.CourseVO;
import com.gmp.entity.TrCourse;

/**
 * 课程Service接口
 */
public interface TrCourseService extends IService<TrCourse> {

    /**
     * 分页查询课程（含分类名称）
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param name       课程名称（模糊）
     * @param categoryId 分类ID
     * @param status     状态
     * @return 分页结果
     */
    Page<CourseVO> pageCourses(Integer pageNum, Integer pageSize, String name, Long categoryId, Integer status);

    /**
     * 查询全部启用课程（用于下拉选择，如视频上传时选择课程）
     *
     * @return 课程VO列表（含分类/部门/岗位名称）
     */
    java.util.List<CourseVO> listCourses();

    /**
     * 新增课程
     */
    void addCourse(CourseAddDTO dto);

    /**
     * 修改课程
     */
    void updateCourse(CourseUpdateDTO dto);
}
