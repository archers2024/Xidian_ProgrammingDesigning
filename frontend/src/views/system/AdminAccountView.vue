<template>
  <!-- FR-12 管理员账号管理（A岗，仅超级管理员可见） -->
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增管理员</el-button>
    </div>
    <el-table :data="list" border>
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="dept" label="部门" />
      <el-table-column prop="roleCode" label="角色" />
      <el-table-column label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          <el-button link type="warning" @click="resetPwd(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="新增管理员" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.dept" /></el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="form.password" type="password" show-password
                    placeholder="必填，至少 8 位且含字母和数字" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCode">
            <el-option label="活动管理员" value="ADMIN" />
            <el-option label="超级管理员" value="SUPER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api'

const list = ref([])
const visible = ref(false)
const form = reactive({ username: '', realName: '', dept: '', password: '', roleCode: 'ADMIN' })

const load = async () => { list.value = await adminApi.list() }
onMounted(load)

const openCreate = () => { Object.assign(form, { username: '', realName: '', dept: '', password: '', roleCode: 'ADMIN' }); visible.value = true }

const submit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写账号和初始密码')
    return
  }
  if (form.password.length < 8 || !/[A-Za-z]/.test(form.password) || !/\d/.test(form.password)) {
    ElMessage.warning('密码至少 8 位且同时包含字母和数字')
    return
  }
  await adminApi.create(form)
  visible.value = false
  ElMessage.success('已新增')
  load()
}
const toggle = async (row) => { await adminApi.status(row.adminId, row.status === 1 ? 0 : 1); load() }
const resetPwd = async (row) => {
  const data = await adminApi.resetPwd(row.adminId)
  const pwd = (data && data.tempPassword) || ''
  // 一次性密码只在本次响应里出现，关掉弹窗后无法再查
  ElMessageBox.alert(`一次性密码：${pwd}\n\n请立即转交本人，关闭后不再显示；该账号原有登录状态已失效。`,
    '密码已重置', { confirmButtonText: '已记录' })
}
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
