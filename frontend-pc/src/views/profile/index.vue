<template>
  <div class="page-container">
    <!-- 顶部概览卡片：头像 + 用户名 + 角色标签 + 上次登录时间 -->
    <el-card shadow="never" class="profile-header">
      <div class="header-left">
        <el-avatar :size="64" icon="UserFilled" />
        <div class="header-info">
          <div class="name-row">
            <span class="real-name">{{ profile.realName || profile.username }}</span>
            <!-- 角色标签：将角色编码转换为中文名称展示 -->
            <el-tag
              v-for="role in profile.roles || []"
              :key="role"
              size="small"
              type="primary"
              effect="plain"
            >
              {{ roleText(role) }}
            </el-tag>
          </div>
          <div class="sub-row">账号：{{ profile.username }}</div>
          <div class="sub-row">上次登录：{{ formatDateTime(profile.lastLoginTime) }}</div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <!-- 左：基本资料 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">基本资料</span>
          </template>
          <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="90px">
            <el-form-item label="用户名">
              <!-- 用户名由管理员维护，个人不可修改 -->
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="profileForm.realName" placeholder="请输入姓名" maxlength="50" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" maxlength="100" />
            </el-form-item>
            <el-form-item label="部门">
              <el-input :model-value="profile.departmentName || '未分配'" disabled />
            </el-form-item>
            <el-form-item label="岗位">
              <el-input :model-value="profile.positionName || '未分配'" disabled />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSaveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右：修改密码 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">修改密码</span>
          </template>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-20位，需含字母和数字" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
// 通用请求方法：管理端请求头直接透传原始 token（不加 Bearer 前缀）
import { get, put } from '@/utils/request'
// 时间格式化工具：统一渲染为 yyyy-MM-dd HH:mm:ss
import { formatDateTime } from '@/utils/format'

/** 角色编码 → 中文名称映射 */
const ROLE_TEXT = {
  SUPER_ADMIN: '系统管理员',
  QA_MANAGER: '质量管理员',
  TRAINING_ADMIN: '培训管理员',
  DEPT_LEADER: '部门负责人',
  EMPLOYEE: '普通员工'
}

/**
 * 角色编码转中文
 * @param {string} code 角色编码
 */
function roleText(code) {
  return ROLE_TEXT[code] || code
}

/** 个人资料展示数据（只读部分） */
const profile = ref({})
/** 资料修改表单 */
const profileForm = reactive({ username: '', realName: '', phone: '', email: '' })
/** 资料表单引用 */
const profileFormRef = ref(null)
/** 保存中状态 */
const saving = ref(false)
/** 资料表单校验规则 */
const profileRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

/** 修改密码表单 */
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
/** 密码表单引用 */
const pwdFormRef = ref(null)
/** 提交中状态 */
const changingPwd = ref(false)
/** 密码表单校验规则：与后端保持一致（6-20位且同时包含字母和数字） */
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{6,20}$/,
      message: '新密码需为6-20位且同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        // 两次输入必须一致，避免误输入
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

/** 拉取当前登录用户资料 */
async function fetchProfile() {
  try {
    const res = await get('/auth/profile')
    profile.value = res.data || {}
    // 回填可编辑字段
    profileForm.username = res.data.username
    profileForm.realName = res.data.realName
    profileForm.phone = res.data.phone
    profileForm.email = res.data.email
  } catch (e) {
    console.error(e)
  }
}

/** 保存基本资料 */
async function handleSaveProfile() {
  try {
    await profileFormRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    await put('/auth/profile', {
      realName: profileForm.realName,
      phone: profileForm.phone,
      email: profileForm.email
    })
    ElMessage.success('资料已更新')
    fetchProfile()
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

/** 修改密码 */
async function handleChangePassword() {
  try {
    await pwdFormRef.value.validate()
  } catch (e) {
    return
  }
  changingPwd.value = true
  try {
    await put('/auth/password', { ...pwdForm })
    ElMessage.success('密码修改成功')
    // 清空密码表单
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e) {
    console.error(e)
  } finally {
    changingPwd.value = false
  }
}

onMounted(fetchProfile)
</script>

<style scoped>
.page-container { display: flex; flex-direction: column; gap: 16px; }
.profile-header :deep(.el-card__body) { padding: 20px; }
.header-left { display: flex; align-items: center; gap: 20px; }
.header-info { display: flex; flex-direction: column; gap: 6px; }
.name-row { display: flex; align-items: center; gap: 10px; }
.real-name { font-size: 20px; font-weight: 600; color: #303133; }
.sub-row { font-size: 13px; color: #909399; }
.card-title { font-weight: 600; }
</style>