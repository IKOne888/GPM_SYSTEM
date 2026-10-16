<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="角色名称">
          <el-input v-model="queryParams.name" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="queryParams.code" placeholder="请输入角色编码" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增角色
        </el-button>
        <span class="table-tip">共 {{ total }} 个角色，权限分配立即生效</span>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="name" label="角色名称" width="140" />
        <el-table-column prop="code" label="角色编码" width="180">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="使用用户" width="100" align="center">
          <template #default="{ row }">
            <el-badge :value="row.userCount || 0" type="primary" class="user-count-badge" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="row.code === 'SUPER_ADMIN'"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              link
              type="warning"
              @click="handleAssignPermission(row)"
            >分配权限</el-button>
            <el-popconfirm
              :title="'确定删除角色「' + row.name + '」吗？如存在用户关联将不允许删除'"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button link type="danger" :disabled="row.code === 'SUPER_ADMIN'">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入角色名称（如 部门主管）" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input
            v-model="formData.code"
            :disabled="!!formData.id"
            placeholder="大写英文字母，如 DEPT_LEADER"
          />
          <div v-if="formData.id" class="form-tip">角色编码创建后不可修改，用于后端权限识别</div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色职责与权限范围（可空）"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限对话框：展示完整菜单树，勾选即绑定 -->
    <el-dialog
      v-model="permDialogVisible"
      :title="'为角色「' + currentRole?.name + '」分配菜单权限'"
      width="560px"
      destroy-on-close
    >
      <div class="perm-dialog-tip">
        提示：勾选菜单节点后点击保存，角色下的用户登录后将只能看到和访问已勾选的功能。<br>
        超级管理员不受此限制。
      </div>
      <div v-loading="treeLoading" class="perm-tree-wrapper">
        <el-tree
          ref="treeRef"
          :data="menuTree"
          show-checkbox
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          default-expand-all
          :checked-keys="checkedMenuIds"
          @check="handleTreeCheck"
        />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSubmitLoading" @click="handleSavePermission">
          保存权限
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 角色管理页面（仅超级管理员可见）
 *
 * 功能清单：
 *   - 角色分页查询（支持按名称/编码/状态筛选）
 *   - 角色新增 / 编辑 / 删除 / 启停用
 *   - 菜单权限分配（弹窗内 el-tree 勾选，全量替换式绑定）
 *
 * 后端接口契约（见 backend RoleController + MenuController）：
 *   GET  /system/roles/page              分页查询
 *   GET  /system/roles/list              查询全部启用角色（下拉用）
 *   POST /system/roles                   新增
 *   PUT  /system/roles                   修改
 *   DELETE /system/roles/{id}            删除（有用户关联时拒绝）
 *   PUT  /system/roles/{id}/status?status=0|1   启停用
 *   GET  /system/menus/tree              菜单树（供弹窗渲染）
 *   GET  /system/roles/{roleId}/menu-ids 某角色当前已分配的菜单ID
 *   POST /system/roles/{roleId}/bind-menus  全量替换绑定菜单
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post, put, del } from '@/utils/request'
import { formatDateTime } from '@/utils/format'

// ==================== 基础状态 ====================

// 表格查询参数
const queryParams = reactive({
  name: '',
  code: '',
  status: '',
  page: 1,
  size: 10
})

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

// 角色编辑对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  status: 1
})
const formRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    {
      // 角色编码规范：大写字母、数字、下划线，3-50位
      pattern: /^[A-Z][A-Z0-9_]{2,49}$/,
      message: '角色编码需以大写字母开头，仅含大写字母/数字/下划线，长度3-50位',
      trigger: 'blur'
    }
  ]
}

// 权限分配对话框
const permDialogVisible = ref(false)
const treeLoading = ref(false)
const permSubmitLoading = ref(false)
const treeRef = ref(null)
const menuTree = ref([])            // 完整菜单树（从 /system/menus/tree 拉取）
const checkedMenuIds = ref([])      // 当前角色已勾选的菜单 ID（用于回填 el-tree）
const currentRole = ref(null)       // 当前正在分配权限的角色对象
// el-tree 内部维护的选中状态（check 事件回调时拿到全量选中 keys）
const selectedKeys = ref([])

// ==================== 数据拉取 ====================

/** 拉角色分页列表 */
async function fetchData() {
  loading.value = true
  try {
    const res = await get('/system/roles/page', queryParams)
    tableData.value = res.data?.records || []
    total.value = Number(res.data?.total) || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// ==================== 搜索 / 重置 ====================
function handleSearch() { queryParams.page = 1; fetchData() }
function handleReset() {
  Object.assign(queryParams, { name: '', code: '', status: '', page: 1 })
  fetchData()
}

// ==================== 新增 / 编辑 / 删除 / 状态切换 ====================

function handleAdd() {
  dialogTitle.value = '新增角色'
  Object.assign(formData, { id: null, name: '', code: '', description: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑角色'
  // 复制一份，避免直接改表格里的对象
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (formData.id) {
      // 修改：code 不允许改，后端 RoleUpdateDTO 不接收 code 字段
      await put('/system/roles', {
        id: formData.id,
        name: formData.name,
        description: formData.description,
        status: formData.status
      })
      ElMessage.success('角色更新成功')
    } else {
      await post('/system/roles', formData)
      ElMessage.success('角色创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await del(`/system/roles/${row.id}`)
    ElMessage.success('角色删除成功')
    fetchData()
  } catch (e) {
    console.error(e)
  }
}

async function handleStatusChange(row) {
  try {
    await put(`/system/roles/${row.id}/status?status=${row.status}`)
    ElMessage.success('状态更新成功')
  } catch (e) {
    // 后端拒绝时（如该角色仍被用户使用），回滚状态
    row.status = row.status === 1 ? 0 : 1
    console.error(e)
  }
}

// ==================== 权限分配对话框 ====================

/** 打开权限分配对话框：并行拉菜单树 + 当前角色已勾选菜单 ID */
async function handleAssignPermission(row) {
  currentRole.value = row
  checkedMenuIds.value = []
  selectedKeys.value = []
  permDialogVisible.value = true
  treeLoading.value = true

  try {
    const [treeRes, idsRes] = await Promise.all([
      get('/system/menus/tree'),
      get(`/system/roles/${row.id}/menu-ids`)
    ])
    menuTree.value = treeRes.data || []
    checkedMenuIds.value = (idsRes.data || []).map(Number)
  } catch (e) {
    console.error(e)
  } finally {
    treeLoading.value = false
  }
}

/**
 * el-tree check 事件：用户勾选/取消时回调
 * @param {object} checkedInfo { checkedKeys: 当前所有勾选的节点 ID, halfCheckedKeys: 半选节点 ID }
 */
function handleTreeCheck(checkedInfo) {
  selectedKeys.value = checkedInfo.checkedKeys || []
}

/** 保存权限：全量替换式绑定（先清空旧关联，再插入新关联） */
async function handleSavePermission() {
  permSubmitLoading.value = true
  try {
    await post(`/system/roles/${currentRole.value.id}/bind-menus`, selectedKeys.value)
    ElMessage.success('权限保存成功，已选 ' + selectedKeys.value.length + ' 个菜单')
    permDialogVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    permSubmitLoading.value = false
  }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.table-tip {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}

.user-count-badge :deep(.el-badge__content) {
  min-width: 20px;
  height: 18px;
  line-height: 18px;
  font-size: 12px;
}

/* 表单辅助提示 */
.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

/* 权限分配对话框 */
.perm-dialog-tip {
  padding: 10px 12px;
  background: #f4f4f5;
  border-radius: 4px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.perm-tree-wrapper {
  max-height: 460px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
}
</style>