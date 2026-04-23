<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-title">
        <el-icon :size="24" style="margin-right: 8px; color: #409EFF;">
          <DataAnalysis />
        </el-icon>
        ABTest配置平台
      </div>
      <div class="header-user">
        <el-tag>{{ userStore.username }}</el-tag>
        <el-button type="text" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#fff"
          text-color="#303133"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/services">
            <el-icon><OfficeBuilding /></el-icon>
            <span>服务管理</span>
          </el-menu-item>
          <el-menu-item index="/experiments">
            <el-icon><Histogram /></el-icon>
            <span>实验管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
}
</script>
