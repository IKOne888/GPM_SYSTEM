<template>
  <div v-loading="loading" class="profile-page">
    <div class="page-head">
      <div>
        <h2 class="gmp-page-title">个人资料</h2>
        <p class="gmp-page-subtitle">维护个人联系方式与登录密码，姓名变更请联系培训管理员</p>
      </div>
    </div>

    <div class="profile-grid">
      <!-- 左栏：用户信息卡 -->
      <div class="gmp-card user-card">
        <div class="user-avatar">
          <el-avatar :size="76">{{ avatarText }}</el-avatar>
        </div>
        <h3 class="user-name">{{ info.realName || info.username || '学员' }}</h3>
        <p class="user-role">{{ roleText }}</p>

        <div class="user-divider"></div>

        <div class="user-meta">
          <div class="meta-row">
            <span class="meta-label"><el-icon><User /></el-icon> 用户名</span>
            <span class="meta-value">{{ info.username || '-' }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label"><el-icon><Iphone /></el-icon> 手机号</span>
            <span class="meta-value">{{ info.phone || '未填写' }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label"><el-icon><Message /></el-icon> 邮箱</span>
            <span class="meta-value">{{ info.email || '未填写' }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label"><el-icon><Clock /></el-icon> 最近登录</span>
            <span class="meta-value">{{ formatDateTime(info.lastLoginTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 右栏：资料表单 + 密码表单 -->
      <div class="form-col">
        <!-- 基本资料 -->
        <div class="gmp-card form-card">
          <div class="panel-head">
            <span class="panel-title">基本信息</span>
            <span class="panel-sub">修改后立即生效</span>
          </div>

          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-width="90px"
            class="form-body"
          >
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" maxlength="50" clearable />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入 11 位手机号" maxlength="11" clearable />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱地址" maxlength="100" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSaveProfile">保存修改</el-button>
              <el-button @click="resetProfileForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 修改密码 -->
        <div class="gmp-card form-card">
          <div class="panel-head">
            <span class="panel-title">修改密码</span>
            <span class="panel-sub">密码需 6-20 位，且同时包含字母和数字</span>
          </div>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="90px"
            class="form-body"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入当前登录密码"
                show-password
                maxlength="20"
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="6-20 位，含字母和数字"
                show-password
                maxlength="20"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
                maxlength="20"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="changing" @click="handleChangePassword">确认修改</el-button>
              <el-button @click="resetPasswordForm">清空</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile, updatePassword } from '@/api/profile'
import { useUserStore } from '@/store/user'
import { formatDateTime } from '@/utils/format'

const userStore = useUserStore()

/** 加载状态 */
const loading = ref(false)
/** 资料保存状态 */
const saving = ref(false)
/** 密码修改状态 */
const changing = ref(false)
/** 用户信息 */
const info = ref({})
/** 资料表单实例 */
const profileFormRef = ref(null)
/** 密码表单实例 */
const passwordFormRef = ref(null)

/** 头像文字 */
const avatarText = computed(() => (info.value.realName || info.value.username || '学').slice(0, 1))

/** 角色文案 */
const roleText = computed(() => {
  const roles = info.value.roles || []
  const map = {
    SUPER_ADMIN: '系统管理员',
    QA_MANAGER: 'QA 负责人',
    TRAINING_ADMIN: '培训管理员',
    DEPT_LEADER: '部门主管',
    EMPLOYEE: '普通员工'
  }
  return roles.map((r) => map[r] || r).join(' / ') || '培训学员'
})

/** 基本资料表单 */
const profileForm = reactive({
  realName: '',
  phone: '',
  email: ''
})

/** 密码表单 */
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/** 资料校验规则（与后端 ProfileUpdateDTO 保持一致） */
const profileRules = {
  realName: [{ max: 50, message: '姓名长度不能超过50个字符', trigger: 'blur' }],
  phone: [
    { pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

/** 密码校验规则（与后端 PasswordUpdateDTO 正则保持一致） */
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d\S]{6,20}$/,
      message: '密码需 6-20 位，且同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

/**
 * 加载个人信息
 */
async function loadProfile() {
  loading.value = true
  try {
    const res = await getProfile()
    info.value = res.data || {}
    profileForm.realName = info.value.realName || ''
    profileForm.phone = info.value.phone || ''
    profileForm.email = info.value.email || ''
  } finally {
    loading.value = false
  }
}

/**
 * 保存基本资料
 */
async function handleSaveProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await updateProfile({ ...profileForm })
    ElMessage.success('个人资料已更新')
    // 同步 Store 与本地缓存，刷新顶部展示
    await userStore.fetchProfile()
    info.value = userStore.userInfo
  } finally {
    saving.value = false
  }
}

/**
 * 重置资料表单为服务端当前值
 */
function resetProfileForm() {
  profileForm.realName = info.value.realName || ''
  profileForm.phone = info.value.phone || ''
  profileForm.email = info.value.email || ''
  profileFormRef.value?.clearValidate()
}

/**
 * 修改登录密码
 */
async function handleChangePassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  changing.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    resetPasswordForm()
    // 密码变更后退出登录，避免旧会话继续使用
    await userStore.logout()
    window.location.href = '/login'
  } finally {
    changing.value = false
  }
}

/**
 * 清空密码表单
 */
function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

onMounted(loadProfile)
</script>

<style scoped>
.page-head {
  margin-bottom: 16px;
}

.profile-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  align-items: start;
}

/* ---------- 用户信息卡 ---------- */
.user-card {
  padding: 28px 24px;
  text-align: center;
}

.user-avatar :deep(.el-avatar) {
  background: var(--brand-gradient);
  font-size: 30px;
  font-weight: 600;
}

.user-name {
  margin: 14px 0 4px;
  font-size: 18px;
  color: var(--text-primary);
}

.user-role {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.user-divider {
  height: 1px;
  margin: 20px 0 16px;
  background: var(--border-color);
}

.user-meta {
  text-align: left;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 13px;
}

.meta-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-secondary);
}

.meta-value {
  color: var(--text-primary);
  font-weight: 500;
}

/* ---------- 表单 ---------- */
.form-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-card {
  padding: 20px 24px 12px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 18px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.panel-sub {
  font-size: 12px;
  color: var(--text-secondary);
}

.form-body {
  max-width: 460px;
}

@media (max-width: 1100px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
