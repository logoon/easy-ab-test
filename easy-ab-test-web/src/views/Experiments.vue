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
      <el-table-column prop="returnValueType" label="返回值类型" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.returnValueType" size="small">{{ getReturnValueTypeText(row.returnValueType) }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="规则数" width="80">
        <template #default="{ row }">
          {{ row.rules ? row.rules.length : 0 }}
        </template>
      </el-table-column>
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
      width="900px"
      :close-on-click-modal="false"
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
        
        <div class="form-item-row">
          <el-form-item label="分流策略">
            <el-radio-group v-model="form.splitStrategy">
              <el-radio value="PERCENTAGE">按百分比分流</el-radio>
              <el-radio value="USER_ATTRIBUTE">按用户属性分流</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="返回值类型">
            <el-select v-model="form.returnValueType" placeholder="选择返回值类型" style="width: 180px;">
              <el-option label="字符串" value="STRING" />
              <el-option label="整数" value="INT" />
              <el-option label="布尔值" value="BOOLEAN" />
              <el-option label="小数" value="DECIMAL" />
              <el-option label="JSON" value="JSON" />
            </el-select>
          </el-form-item>
        </div>
        
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
        
        <el-divider>规则配置</el-divider>
        
        <p style="color: #909399; margin-bottom: 15px; font-size: 13px;">
          规则按优先级顺序匹配（数字越小优先级越高），匹配成功后返回对应的值；所有规则都不匹配时返回默认值。
        </p>
        
        <div v-for="(rule, ruleIndex) in form.rules" :key="ruleIndex" class="rule-item">
          <div class="rule-header">
            <span class="rule-title">规则 {{ ruleIndex + 1 }}</span>
            <div style="display: flex; align-items: center; gap: 15px;">
              <span style="font-size: 13px; color: #909399;">
                优先级: 
                <el-input-number 
                  v-model="rule.priority" 
                  :min="1" 
                  :max="100" 
                  size="small"
                  style="width: 80px; margin-left: 5px;"
                />
              </span>
              <el-button
                type="danger"
                link
                @click="form.rules.splice(ruleIndex, 1)"
              >
                <el-icon><Delete /></el-icon>
                删除规则
              </el-button>
            </div>
          </div>
          
          <div class="rule-conditions">
            <div style="font-size: 13px; color: #606266; margin-bottom: 10px; font-weight: 500;">
              条件配置（全部条件都满足时才匹配）：
            </div>
            <div v-for="(condition, condIndex) in rule.conditions" :key="condIndex" class="condition-row">
              <el-input 
                v-model="condition.fieldName" 
                placeholder="字段名" 
                style="width: 120px;"
              />
              <el-select 
                v-model="condition.operator" 
                placeholder="运算符" 
                style="width: 100px;"
              >
                <el-option label="等于" value="EQ" />
                <el-option label="不等于" value="NE" />
                <el-option label="包含" value="IN" />
                <el-option label="包含子串" value="CONTAINS" />
                <el-option label="大于" value="GT" />
                <el-option label="小于" value="LT" />
                <el-option label="大于等于" value="GTE" />
                <el-option label="小于等于" value="LTE" />
              </el-select>
              <template v-if="['IN'].includes(condition.operator)">
                <div style="flex: 1; display: flex; align-items: center; gap: 5px;">
                  <el-tag 
                    v-for="(val, vIdx) in condition.values" 
                    :key="vIdx"
                    closable
                    @close="condition.values.splice(vIdx, 1)"
                    style="margin-right: 5px;"
                  >
                    {{ val }}
                  </el-tag>
                  <el-input
                    v-model="newConditionValue[ruleIndex + '_' + condIndex]"
                    placeholder="输入值后回车"
                    size="small"
                    style="width: 100px;"
                    @keyup.enter="addConditionValue(ruleIndex, condIndex)"
                  />
                </div>
              </template>
              <template v-else>
                <el-input 
                  v-model="condition.value" 
                  placeholder="值" 
                  style="width: 150px;"
                />
              </template>
              <el-button 
                type="danger" 
                link 
                :disabled="rule.conditions.length <= 1"
                @click="rule.conditions.splice(condIndex, 1)"
              >
                <el-icon><Minus /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" plain size="small" @click="addCondition(rule)">
              <el-icon><Plus /></el-icon>
              添加条件
            </el-button>
          </div>
          
          <div class="rule-return-value">
            <div style="font-size: 13px; color: #606266; margin-bottom: 10px; font-weight: 500;">
              命中后的返回值：
            </div>
            <el-radio-group v-model="rule.returnValue.mode" @change="(v) => onReturnValueModeChange(rule, v)">
              <el-radio value="FIXED">固定值</el-radio>
              <el-radio value="WEIGHTED">加权值</el-radio>
            </el-radio-group>
            
            <template v-if="rule.returnValue.mode === 'FIXED'">
              <el-input 
                v-model="rule.returnValue.fixedValue" 
                placeholder="返回值" 
                style="width: 300px; margin-top: 10px;"
              />
            </template>
            
            <template v-else>
              <div style="margin-top: 10px;">
                <div v-for="(wv, wvIndex) in rule.returnValue.weightedValues" :key="wvIndex" class="weighted-row">
                  <el-input 
                    v-model="wv.weight" 
                    placeholder="权重(如: 0.7)" 
                    style="width: 100px;"
                  />
                  <span style="margin: 0 5px;">→</span>
                  <el-input 
                    v-model="wv.value" 
                    placeholder="值" 
                    style="width: 150px;"
                  />
                  <el-button 
                    type="danger" 
                    link 
                    :disabled="rule.returnValue.weightedValues.length <= 1"
                    @click="rule.returnValue.weightedValues.splice(wvIndex, 1)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
                <el-button type="primary" plain size="small" @click="addWeightedValue(rule.returnValue)">
                  <el-icon><Plus /></el-icon>
                  添加加权值
                </el-button>
              </div>
            </template>
          </div>
        </div>
        
        <el-form-item>
          <el-button type="primary" plain @click="addRule">
            <el-icon><Plus /></el-icon>
            添加规则
          </el-button>
        </el-form-item>
        
        <el-divider>默认返回值</el-divider>
        
        <p style="color: #909399; margin-bottom: 15px; font-size: 13px;">
          当所有规则都不匹配时，返回此默认值。
        </p>
        
        <el-radio-group v-model="form.defaultValue.mode" @change="(v) => onDefaultValueModeChange(v)">
          <el-radio value="FIXED">固定值</el-radio>
          <el-radio value="WEIGHTED">加权值</el-radio>
        </el-radio-group>
        
        <template v-if="form.defaultValue.mode === 'FIXED'">
          <el-form-item label="默认值" style="margin-top: 15px;">
            <el-input 
              v-model="form.defaultValue.fixedValue" 
              placeholder="默认返回值" 
              style="width: 300px;"
            />
          </el-form-item>
        </template>
        
        <template v-else>
          <div style="margin-top: 15px;">
            <div v-for="(wv, wvIndex) in form.defaultValue.weightedValues" :key="wvIndex" class="weighted-row">
              <el-input 
                v-model="wv.weight" 
                placeholder="权重(如: 0.7)" 
                style="width: 100px;"
              />
              <span style="margin: 0 5px;">→</span>
              <el-input 
                v-model="wv.value" 
                placeholder="值" 
                style="width: 150px;"
              />
              <el-button 
                type="danger" 
                link 
                :disabled="form.defaultValue.weightedValues.length <= 1"
                @click="form.defaultValue.weightedValues.splice(wvIndex, 1)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" plain size="small" @click="addWeightedValue(form.defaultValue)">
              <el-icon><Plus /></el-icon>
              添加加权值
            </el-button>
          </div>
        </template>
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
import { ref, reactive, onMounted, watch } from 'vue'
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
const newConditionValue = reactive({})

const createEmptyGroup = () => ({
  groupName: '',
  groupCode: '',
  weight: 1,
  config: '{}',
  isControl: false
})

const createEmptyCondition = () => ({
  fieldName: '',
  operator: 'EQ',
  value: '',
  values: []
})

const createEmptyReturnValue = () => ({
  mode: 'FIXED',
  fixedValue: '',
  weightedValues: [
    { weight: '0.5', value: '' },
    { weight: '0.5', value: '' }
  ]
})

const createEmptyRule = (priority) => ({
  priority: priority,
  conditions: [createEmptyCondition()],
  returnValue: createEmptyReturnValue()
})

const resetForm = () => {
  form.experimentName = ''
  form.version = 'v1.0.0'
  form.effectiveTime = null
  form.expireTime = null
  form.splitStrategy = 'PERCENTAGE'
  form.percentage = 100
  form.userAttribute = ''
  form.attributeValues = ''
  form.returnValueType = null
  form.groups = [
    { groupName: '对照组', groupCode: 'A', weight: 1, config: '{}', isControl: true },
    { groupName: '实验组', groupCode: 'B', weight: 1, config: '{}', isControl: false }
  ]
  form.rules = []
  form.defaultValue = createEmptyReturnValue()
}

const form = reactive({
  experimentName: '',
  version: 'v1.0.0',
  effectiveTime: null,
  expireTime: null,
  splitStrategy: 'PERCENTAGE',
  percentage: 100,
  userAttribute: '',
  attributeValues: '',
  returnValueType: null,
  groups: [
    { groupName: '对照组', groupCode: 'A', weight: 1, config: '{}', isControl: true },
    { groupName: '实验组', groupCode: 'B', weight: 1, config: '{}', isControl: false }
  ],
  rules: [],
  defaultValue: createEmptyReturnValue()
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

const getReturnValueTypeText = (type) => {
  const map = {
    STRING: '字符串',
    INT: '整数',
    BOOLEAN: '布尔值',
    DECIMAL: '小数',
    JSON: 'JSON'
  }
  return map[type] || type
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

const addRule = () => {
  const nextPriority = form.rules.length > 0 
    ? Math.max(...form.rules.map(r => r.priority)) + 1 
    : 1
  form.rules.push(createEmptyRule(nextPriority))
}

const addCondition = (rule) => {
  rule.conditions.push(createEmptyCondition())
}

const addConditionValue = (ruleIndex, condIndex) => {
  const key = ruleIndex + '_' + condIndex
  const value = newConditionValue[key]
  if (value && value.trim()) {
    const rule = form.rules[ruleIndex]
    const condition = rule.conditions[condIndex]
    if (!condition.values) {
      condition.values = []
    }
    if (!condition.values.includes(value.trim())) {
      condition.values.push(value.trim())
    }
    newConditionValue[key] = ''
  }
}

const addWeightedValue = (returnValue) => {
  returnValue.weightedValues.push({ weight: '', value: '' })
}

const onReturnValueModeChange = (rule, mode) => {
  if (mode === 'WEIGHTED') {
    rule.returnValue.weightedValues = [
      { weight: '0.5', value: '' },
      { weight: '0.5', value: '' }
    ]
  }
}

const onDefaultValueModeChange = (mode) => {
  if (mode === 'WEIGHTED') {
    form.defaultValue.weightedValues = [
      { weight: '0.5', value: '' },
      { weight: '0.5', value: '' }
    ]
  }
}

const handleCreate = () => {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

const parseRuleFromData = (ruleData) => {
  if (!ruleData) return null
  return {
    priority: ruleData.priority || 1,
    conditions: Array.isArray(ruleData.conditions) ? ruleData.conditions : [createEmptyCondition()],
    returnValue: ruleData.returnValue || createEmptyReturnValue()
  }
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
  form.returnValueType = row.returnValueType || null
  form.groups = JSON.parse(JSON.stringify(row.groups || []))
  
  form.rules = row.rules 
    ? row.rules.map(r => parseRuleFromData(r)) 
    : []
  
  form.defaultValue = row.defaultValue 
    ? JSON.parse(JSON.stringify(row.defaultValue)) 
    : createEmptyReturnValue()
  
  if (!form.defaultValue.weightedValues || form.defaultValue.weightedValues.length === 0) {
    form.defaultValue.weightedValues = [
      { weight: '0.5', value: '' },
      { weight: '0.5', value: '' }
    ]
  }
  
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
      serviceId: selectedServiceId.value,
      returnValueType: form.returnValueType,
      rules: form.rules,
      defaultValue: form.defaultValue
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

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.form-item-row {
  display: flex;
  gap: 20px;
}

.form-item-row .el-form-item {
  flex: 1;
  margin-bottom: 18px;
}

.group-item {
  background: #f9fafc;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #e4e7ed;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #dcdfe6;
}

.group-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.config-textarea {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
}

.rule-item {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #e4e7ed;
}

.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.rule-title {
  font-weight: 600;
  font-size: 15px;
  color: #409eff;
}

.rule-conditions {
  margin-bottom: 20px;
}

.condition-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
}

.rule-return-value {
  padding: 12px;
  background: #fff;
  border-radius: 4px;
}

.weighted-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.status-tag {
  margin-right: 4px;
}
</style>
