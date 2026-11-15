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
        <el-form-item label="初始密码"><el-input v-model="form.passwordHash" placeholder="不填则默认 12345678" /></el-form-item>
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
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api'

const list = ref([])
const visible = ref(false)
const form = reactive({ username: '', realName: '', dept: '', passwordHash: '', roleCode: 'ADMIN' })

const load = async () => { list.value = await adminApi.list() }
onMounted(load)

const openCreate = () => { Object.assign(form, { username: '', realName: '', dept: '', passwordHash: '', roleCode: 'ADMIN' }); visible.value = true }
const submit = async () => { await adminApi.create(form); visible.value = false; ElMessage.success('已新增'); load() }
const toggle = async (row) => { await adminApi.status(row.adminId, row.status === 1 ? 0 : 1); load() }
const resetPwd = async (row) => { await adminApi.resetPwd(row.adminId); ElMessage.success('密码已重置为 12345678') }
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
