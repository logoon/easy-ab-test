<template>
  <div>
    <div class="toolbar">
      <h3 class="page-title">实验管理</h3>
      <div style="display: flex; gap: 10px;">
        <el-select
          v-model="selectedServiceId"
          placeholder="选择服务"
          style="width: 200px;"
          @change="fetchExperiments"
        >
          <el-option
            v-for="service in services"
            :key="service.id"
            :label="service.serviceName"
            :value="service.id"
          />
        </el-select>
        <el-button type="primary" :disabled="!selectedServiceId" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建实验
        </el-button>
      </div>
    </div>
    
    <el-table :data="experiments" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="experimentName" label="实验名称" min-width="180" />
      <el-table-column prop="version" label="版本" width="100" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" class="status-tag">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="splitStrategy" label="分流策略" width="120">
        <template #default="{ row }">
          <span v-if="row.splitStrategy === 'PERCENTAGE'">百分比</span>
          <span v-else-if="row.splitStrategy === 'USER_ATTRIBUTE'">用户属性</span>
        </template>
      </el-table-column>
      <el-table-column label="生效时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.effectiveTime) }}
        </template>
      </el-table-column>
      <el-table-column label="过期时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.expireTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-dropdown @command="(command) => handleStatusChange(row, command)">
            <el-button type="success" link>
              <el-icon><Operation /></el-icon>
              状态
              <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="DRAFT" :disabled="row.status === 'DRAFT'">
                  草稿
                </el-dropdown-item>
                <el-dropdown-item command="RUNNING" :disabled="row.status === 'RUNNING'">
                  运行中
                </el-dropdown-item>
                <el-dropdown-item command="PAUSED" :disabled="row.status === 'PAUSED'">
                  已暂停
                </el-dropdown-item>
                <el-dropdown-item command="FINISHED" :disabled="row.status === 'FINISHED'">
                  已结束
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
      :title="isEdit ? '编辑实验' : '新建实验'"
      width="800px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <div class="form-item-row">
          <el-form-item label="实验名称" prop="experimentName">
            <el-input v-model="form.experimentName" placeholder="请输入实验名称" />
          </el-form-item>
          <el-form-item label="版本" prop="version">
            <el-input v-model="form.version" placeholder="如: v1.0.0" />
          </el-form-item>
        </div>
        
        <div class="form-item-row">
          <el-form-item label="生效时间">
            <el-date-picker
              v-model="form.effectiveTime"
              type="datetime"
              placeholder="选择生效时间"
              style="width: 100%"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="过期时间">
            <el-date-picker
              v-model="form.expireTime"
              type="datetime"
              placeholder="选择过期时间"
              style="width: 100%"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
        </div>
        
        <el-form-item label="分流策略">
          <el-radio-group v-model="form.splitStrategy">
            <el-radio value="PERCENTAGE">按百分比分流</el-radio>
            <el-radio value="USER_ATTRIBUTE">按用户属性分流</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item v-if="form.splitStrategy === 'PERCENTAGE'" label="分流百分比">
          <el-slider
            v-model="form.percentage"
            :min="0"
            :max="100"
            :step="1"
            :show-input="true"
            style="width: 300px"
          />
          <span style="margin-left: 10px; color: #909399;">%</span>
        </el-form-item>
        
        <template v-if="form.splitStrategy === 'USER_ATTRIBUTE'">
          <el-form-item label="用户属性名">
            <el-input v-model="form.userAttribute" placeholder="如: region、device等" style="width: 300px" />
          </el-form-item>
          <el-form-item label="属性值列表">
            <el-input
              v-model="form.attributeValues"
              type="textarea"
              :rows="2"
              placeholder="多个值用逗号分隔，如: cn,us,jp"
            />
          </el-form-item>
        </template>
        
        <el-divider>实验组配置</el-divider>
        
        <div v-for="(group, index) in form.groups" :key="index" class="group-item">
          <div class="group-header">
            <span class="group-title">
              实验组 {{ index + 1 }}
              <el-tag v-if="group.isControl" type="success" size="small" style="margin-left: 8px;">
                对照组
              </el-tag>
            </span>
            <div>
              <el-checkbox
                v-model="group.isControl"
                :disabled="group.isControl || form.groups.some((g, i) => i !== index && g.isControl)"
                style="margin-right: 10px;"
              >
                设为对照组
              </el-checkbox>
              <el-button
                type="danger"
                link
                :disabled="form.groups.length <= 1"
                @click="form.groups.splice(index, 1)"
              >
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </div>
          <div class="form-item-row">
            <el-form-item label="组名称" :prop="`groups.${index}.groupName`">
              <el-input v-model="group.groupName" placeholder="如: 实验组A" />
            </el-form-item>
            <el-form-item label="组编码" :prop="`groups.${index}.groupCode`">
              <el-input v-model="group.groupCode" placeholder="如: A" />
            </el-form-item>
            <el-form-item label="权重" :prop="`groups.${index}.weight`">
              <el-input-number
                v-model="group.weight"
                :min="1"
                :max="100"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          <el-form-item label="配置JSON">
            <el-input
              v-model="group.config"
              type="textarea"
              :rows="4"
              class="config-textarea"
              placeholder='{"key": "value"}'
            />
          </el-form-item>
        </div>
        
        <el-form-item>
          <el-button type="primary" plain @click="addGroup">
            <el-icon><Plus /></el-icon>
            添加实验组
          </el-button>
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
import { getServices } from '@/api/service'
import {
  getExperimentsByService,
  createExperiment,
  updateExperiment,
  deleteExperiment,
  updateExperimentStatus
} from '@/api/experiment'

const loading = ref(false)
const submitLoading = ref(false)
const services = ref([])
const experiments = ref([])
const selectedServiceId = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const editId = ref(null)

const form = reactive({
  experimentName: '',
  version: 'v1.0.0',
  effectiveTime: null,
  expireTime: null,
  splitStrategy: 'PERCENTAGE',
  percentage: 100,
  userAttribute: '',
  attributeValues: '',
  groups: [
    { groupName: '对照组', groupCode: 'A', weight: 1, config: '{}', isControl: true },
    { groupName: '实验组', groupCode: 'B', weight: 1, config: '{}', isControl: false }
  ]
})

const rules = {
  experimentName: [
    { required: true, message: '请输入实验名称', trigger: 'blur' }
  ],
  version: [
    { required: true, message: '请输入版本', trigger: 'blur' }
  ]
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getStatusType = (status) => {
  const map = {
    DRAFT: 'info',
    RUNNING: 'success',
    PAUSED: 'warning',
    FINISHED: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    DRAFT: '草稿',
    RUNNING: '运行中',
    PAUSED: '已暂停',
    FINISHED: '已结束'
  }
  return map[status] || status
}

const fetchServices = async () => {
  try {
    services.value = await getServices()
  } catch (error) {
    console.error('获取服务列表失败:', error)
  }
}

const fetchExperiments = async () => {
  if (!selectedServiceId.value) {
    experiments.value = []
    return
  }
  loading.value = true
  try {
    experiments.value = await getExperimentsByService(selectedServiceId.value)
  } catch (error) {
    console.error('获取实验列表失败:', error)
  } finally {
    loading.value = false
  }
}

const addGroup = () => {
  const nextChar = String.fromCharCode(65 + form.groups.length)
  form.groups.push({
    groupName: `实验组${nextChar}`,
    groupCode: nextChar,
    weight: 1,
    config: '{}',
    isControl: false
  })
}

const handleCreate = () => {
  isEdit.value = false
  editId.value = null
  form.experimentName = ''
  form.version = 'v1.0.0'
  form.effectiveTime = null
  form.expireTime = null
  form.splitStrategy = 'PERCENTAGE'
  form.percentage = 100
  form.userAttribute = ''
  form.attributeValues = ''
  form.groups = [
    { groupName: '对照组', groupCode: 'A', weight: 1, config: '{}', isControl: true },
    { groupName: '实验组', groupCode: 'B', weight: 1, config: '{}', isControl: false }
  ]
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.experimentName = row.experimentName
  form.version = row.version
  form.effectiveTime = row.effectiveTime
  form.expireTime = row.expireTime
  form.splitStrategy = row.splitStrategy
  form.percentage = row.percentage || 100
  form.userAttribute = row.userAttribute || ''
  form.attributeValues = row.attributeValues || ''
  form.groups = JSON.parse(JSON.stringify(row.groups || []))
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除实验"${row.experimentName}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteExperiment(row.id)
      ElMessage.success('删除成功')
      fetchExperiments()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

const handleStatusChange = async (row, status) => {
  const statusText = getStatusText(status)
  ElMessageBox.confirm(`确定要将实验"${row.experimentName}"状态修改为"${statusText}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await updateExperimentStatus(row.id, status)
      ElMessage.success('状态更新成功')
      fetchExperiments()
    } catch (error) {
      console.error('状态更新失败:', error)
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  if (form.groups.length === 0) {
    ElMessage.warning('请至少配置一个实验组')
    return
  }
  
  submitLoading.value = true
  try {
    const data = {
      experimentName: form.experimentName,
      version: form.version,
      effectiveTime: form.effectiveTime,
      expireTime: form.expireTime,
      splitStrategy: form.splitStrategy,
      percentage: form.percentage,
      userAttribute: form.userAttribute,
      attributeValues: form.attributeValues,
      groups: form.groups,
      serviceId: selectedServiceId.value
    }
    
    if (isEdit.value) {
      await updateExperiment(editId.value, data)
      ElMessage.success('更新成功')
    } else {
      await createExperiment(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchExperiments()
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
