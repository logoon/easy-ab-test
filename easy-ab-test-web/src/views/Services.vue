<template>
  <div>
    <div class="toolbar">
      <h3 class="page-title">服务管理</h3>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建服务
      </el-button>
    </div>
    
    <el-table :data="services" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="serviceName" label="服务名称" min-width="180" />
      <el-table-column prop="serviceCode" label="服务编码" min-width="180">
        <template #default="{ row }">
          <el-tag type="info">{{ row.serviceCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button type="danger" link @click="handleDelete(row)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑服务' : '新建服务'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="服务名称" prop="serviceName">
          <el-input v-model="form.serviceName" placeholder="请输入服务名称" />
        </el-form-item>
        <el-form-item label="服务编码" prop="serviceCode">
          <el-input
            v-model="form.serviceCode"
            placeholder="请输入服务编码（字母开头，支持字母、数字、下划线）"
            :disabled="isEdit"
          />
          <div v-if="isEdit" style="color: #909399; font-size: 12px; margin-top: 4px;">
            提示：服务编码创建后不可修改
          </div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入服务描述（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getServices, createService, updateService, deleteService } from '@/api/service'

const loading = ref(false)
const submitLoading = ref(false)
const services = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editId = ref(null)

const form = reactive({
  serviceName: '',
  serviceCode: '',
  description: ''
})

const rules = {
  serviceName: [
    { required: true, message: '请输入服务名称', trigger: 'blur' }
  ],
  serviceCode: [
    { required: true, message: '请输入服务编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '服务编码必须以字母开头，只能包含字母、数字和下划线', trigger: 'blur' }
  ]
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const fetchServices = async () => {
  loading.value = true
  try {
    services.value = await getServices()
  } catch (error) {
    console.error('获取服务列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  isEdit.value = false
  editId.value = null
  form.serviceName = ''
  form.serviceCode = ''
  form.description = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.serviceName = row.serviceName
  form.serviceCode = row.serviceCode
  form.description = row.description || ''
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除服务"${row.serviceName}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteService(row.id)
      ElMessage.success('删除成功')
      fetchServices()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateService(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createService(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchServices()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchServices()
})
</script>
