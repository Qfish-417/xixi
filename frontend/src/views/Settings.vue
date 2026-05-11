<template>
  <div class="settings-container">
    <el-container>
      <el-header class="navbar">
        <div class="nav-left">
          <el-button class="back-btn" @click="$router.push('/')">
            <ArrowLeft class="icon" />
          </el-button>
          <div class="logo">
            <PlayCircle class="logo-icon" />
            <span>AI漫剧创作平台</span>
          </div>
        </div>
        <div class="nav-right">
          <el-dropdown>
            <div class="user-profile">
              <User class="icon" />
              <span>{{ user.nickname }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-container>
        <el-aside width="220px" class="sidebar">
          <el-menu :default-active="activeTab" class="menu" @select="handleTabSelect">
            <el-menu-item index="general">
              <Settings class="icon" />
              <span>通用设置</span>
            </el-menu-item>
            <el-menu-item index="ai">
              <Brain class="icon" />
              <span>AI模型设置</span>
            </el-menu-item>
            <el-menu-item index="appearance">
              <Palette class="icon" />
              <span>外观设置</span>
            </el-menu-item>
            <el-menu-item index="shortcuts">
              <Keyboard class="icon" />
              <span>快捷键设置</span>
            </el-menu-item>
            <el-menu-item index="privacy">
              <Shield class="icon" />
              <span>隐私设置</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="content-area">
          <div v-if="activeTab === 'general'" class="settings-section">
            <h2>通用设置</h2>
            
            <el-form :model="generalSettings" class="settings-form">
              <el-form-item label="语言">
                <el-select v-model="generalSettings.language">
                  <el-option label="简体中文" value="zh-CN" />
                  <el-option label="English" value="en" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="时区">
                <el-select v-model="generalSettings.timezone">
                  <el-option label="UTC+8 北京" value="Asia/Shanghai" />
                  <el-option label="UTC+0 伦敦" value="Europe/London" />
                  <el-option label="UTC-5 纽约" value="America/New_York" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="自动保存">
                <el-switch v-model="generalSettings.autoSave" />
                <span class="switch-desc">自动保存项目更改</span>
              </el-form-item>
              
              <el-form-item label="保存间隔">
                <el-select v-model="generalSettings.saveInterval" :disabled="!generalSettings.autoSave">
                  <el-option label="1分钟" value="1" />
                  <el-option label="5分钟" value="5" />
                  <el-option label="10分钟" value="10" />
                  <el-option label="30分钟" value="30" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="通知提醒">
                <el-switch v-model="generalSettings.notifications" />
                <span class="switch-desc">接收系统通知和更新提醒</span>
              </el-form-item>
              
              <el-form-item>
                <el-button type="primary" @click="saveGeneralSettings">保存设置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-if="activeTab === 'ai'" class="settings-section">
            <h2>AI模型设置</h2>
            
            <div class="model-card">
              <div class="model-header">
                <h3>默认AI模型</h3>
                <span class="current-model">当前: {{ getModelName(currentModel) }}</span>
              </div>
              
              <div class="model-options">
                <div 
                  v-for="model in aiModels" 
                  :key="model.id"
                  :class="['model-option', { active: currentModel === model.id }]"
                  @click="selectModel(model.id)"
                >
                  <div class="model-radio">
                    <el-radio :value="model.id" v-model="currentModel" />
                  </div>
                  <div class="model-info">
                    <h4>{{ model.name }}</h4>
                    <p>{{ model.description }}</p>
                    <div class="model-features">
                      <span v-for="feature in model.features" :key="feature" class="feature-tag">{{ feature }}</span>
                    </div>
                  </div>
                </div>
              </div>
              
              <div class="api-settings">
                <h4>API设置</h4>
                <el-form :model="apiSettings">
                  <el-form-item label="API密钥">
                    <el-input type="password" v-model="apiSettings.apiKey" placeholder="输入API密钥" />
                  </el-form-item>
                  <el-form-item label="最大并发请求数">
                    <el-input-number v-model="apiSettings.maxConcurrent" :min="1" :max="10" />
                  </el-form-item>
                  <el-form-item label="请求超时时间(秒)">
                    <el-input-number v-model="apiSettings.timeout" :min="10" :max="300" />
                  </el-form-item>
                </el-form>
              </div>
              
              <el-button type="primary" @click="saveModelSettings">保存设置</el-button>
            </div>
          </div>

          <div v-if="activeTab === 'appearance'" class="settings-section">
            <h2>外观设置</h2>
            
            <el-form :model="appearanceSettings" class="settings-form">
              <el-form-item label="主题模式">
                <div class="theme-options">
                  <div 
                    :class="['theme-option', { active: appearanceSettings.theme === 'dark' }]"
                    @click="appearanceSettings.theme = 'dark'"
                  >
                    <div class="theme-preview dark"></div>
                    <span>深色模式</span>
                  </div>
                  <div 
                    :class="['theme-option', { active: appearanceSettings.theme === 'light' }]"
                    @click="appearanceSettings.theme = 'light'"
                  >
                    <div class="theme-preview light"></div>
                    <span>浅色模式</span>
                  </div>
                  <div 
                    :class="['theme-option', { active: appearanceSettings.theme === 'auto' }]"
                    @click="appearanceSettings.theme = 'auto'"
                  >
                    <div class="theme-preview auto"></div>
                    <span>跟随系统</span>
                  </div>
                </div>
              </el-form-item>
              
              <el-form-item label="界面缩放">
                <el-slider v-model="appearanceSettings.scale" :min="80" :max="150" :step="5" />
                <span class="slider-value">{{ appearanceSettings.scale }}%</span>
              </el-form-item>
              
              <el-form-item label="字体大小">
                <el-select v-model="appearanceSettings.fontSize">
                  <el-option label="小" value="small" />
                  <el-option label="中" value="medium" />
                  <el-option label="大" value="large" />
                </el-select>
              </el-form-item>
              
              <el-form-item>
                <el-button type="primary" @click="saveAppearanceSettings">保存设置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-if="activeTab === 'shortcuts'" class="settings-section">
            <h2>快捷键设置</h2>
            
            <div class="shortcuts-list">
              <div v-for="shortcut in shortcuts" :key="shortcut.id" class="shortcut-item">
                <div class="shortcut-info">
                  <span class="shortcut-name">{{ shortcut.name }}</span>
                  <span class="shortcut-desc">{{ shortcut.description }}</span>
                </div>
                <div class="shortcut-keys">
                  <span v-for="(key, index) in shortcut.keys" :key="index" class="key">{{ key }}</span>
                </div>
              </div>
            </div>
            
            <el-button type="primary" @click="resetShortcuts">恢复默认</el-button>
          </div>

          <div v-if="activeTab === 'privacy'" class="settings-section">
            <h2>隐私设置</h2>
            
            <el-form :model="privacySettings" class="settings-form">
              <el-form-item label="数据收集">
                <el-switch v-model="privacySettings.dataCollection" />
                <span class="switch-desc">允许收集使用数据以改进服务</span>
              </el-form-item>
              
              <el-form-item label="项目自动同步">
                <el-switch v-model="privacySettings.autoSync" />
                <span class="switch-desc">自动同步项目到云端</span>
              </el-form-item>
              
              <el-form-item label="素材分享">
                <el-switch v-model="privacySettings.shareMaterials" />
                <span class="switch-desc">允许将创作素材分享到公共素材库</span>
              </el-form-item>
              
              <el-form-item label="活动状态">
                <el-switch v-model="privacySettings.activityStatus" />
                <span class="switch-desc">显示在线状态和活动记录</span>
              </el-form-item>
              
              <div class="privacy-info">
                <h4>数据安全</h4>
                <p>您的数据存储在安全的加密服务器上，只有您可以访问。</p>
                <el-button type="text" @click="exportData">导出我的数据</el-button>
                <el-button type="text" @click="deleteData">删除所有数据</el-button>
              </div>
              
              <el-form-item>
                <el-button type="primary" @click="savePrivacySettings">保存设置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { 
  PlayCircle, User, Settings, Brain, Palette,
  Keyboard, Shield, ArrowLeft
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const activeTab = ref('general')

const generalSettings = reactive({
  language: 'zh-CN',
  timezone: 'Asia/Shanghai',
  autoSave: true,
  saveInterval: '5',
  notifications: true
})

const currentModel = ref('deepseek')

const aiModels = ref([
  {
    id: 'deepseek',
    name: 'DeepSeek',
    description: '深度求索模型，擅长创意内容生成',
    features: ['文生图', '图生图', '脚本生成', '对话']
  },
  {
    id: 'qwen',
    name: 'Qwen',
    description: '阿里巴巴通义千问，多模态能力强',
    features: ['文生图', '视频生成', '对话']
  },
  {
    id: 'jimeng',
    name: '即梦AI',
    description: '专注于动漫风格创作',
    features: ['文生图', '图生图', '图生视频']
  }
])

const apiSettings = reactive({
  apiKey: '',
  maxConcurrent: 3,
  timeout: 60
})

const appearanceSettings = reactive({
  theme: 'dark',
  scale: 100,
  fontSize: 'medium'
})

const shortcuts = ref([
  { id: '1', name: '保存', description: '保存当前项目', keys: ['Ctrl', 'S'] },
  { id: '2', name: '撤销', description: '撤销上一步操作', keys: ['Ctrl', 'Z'] },
  { id: '3', name: '重做', description: '重做上一步操作', keys: ['Ctrl', 'Shift', 'Z'] },
  { id: '4', name: '复制', description: '复制选中内容', keys: ['Ctrl', 'C'] },
  { id: '5', name: '粘贴', description: '粘贴剪贴板内容', keys: ['Ctrl', 'V'] },
  { id: '6', name: '删除', description: '删除选中内容', keys: ['Delete'] },
  { id: '7', name: '全选', description: '选中全部内容', keys: ['Ctrl', 'A'] },
  { id: '8', name: '导出', description: '导出项目', keys: ['Ctrl', 'E'] }
])

const privacySettings = reactive({
  dataCollection: true,
  autoSync: true,
  shareMaterials: false,
  activityStatus: true
})

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

const handleTabSelect = (tab) => {
  activeTab.value = tab
}

const getModelName = (modelId) => {
  const model = aiModels.value.find(m => m.id === modelId)
  return model ? model.name : modelId
}

const selectModel = (modelId) => {
  currentModel.value = modelId
}

const saveGeneralSettings = () => {
  ElMessage.success('通用设置已保存')
}

const saveModelSettings = () => {
  localStorage.setItem('model', currentModel.value)
  ElMessage.success('AI模型设置已保存')
}

const saveAppearanceSettings = () => {
  ElMessage.success('外观设置已保存')
}

const resetShortcuts = () => {
  ElMessage.success('快捷键已恢复默认')
}

const savePrivacySettings = () => {
  ElMessage.success('隐私设置已保存')
}

const exportData = () => {
  ElMessage.info('数据导出功能开发中')
}

const deleteData = () => {
  ElMessage.confirm('确定要删除所有数据吗？此操作不可恢复。')
}
</script>

<style scoped>
.settings-container {
  min-height: 100vh;
  background: var(--bg-dark);
}

.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: var(--gradient-primary);
  height: 56px;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-btn {
  background: rgba(255, 255, 255, 0.1) !important;
  border: none !important;
  color: white !important;
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.2) !important;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: bold;
  color: white;
  font-family: var(--font-heading);
}

.logo-icon {
  width: 32px;
  height: 32px;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: white;
}

.icon {
  width: 20px;
  height: 20px;
}

.sidebar {
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
}

.menu {
  border-right: none;
}

.content-area {
  padding: 24px;
}

.settings-section h2 {
  margin-bottom: 24px;
  font-family: var(--font-heading);
}

.settings-form {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  padding: 24px;
  border-radius: var(--radius-lg);
}

.switch-desc {
  margin-left: 12px;
  color: var(--text-secondary);
  font-size: 14px;
}

.slider-value {
  margin-left: 12px;
  color: var(--primary-color);
  font-weight: bold;
}

.model-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.model-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.model-header h3 {
  font-family: var(--font-heading);
}

.current-model {
  background: var(--primary-color);
  padding: 6px 14px;
  border-radius: 12px;
  font-size: 13px;
}

.model-options {
  margin-bottom: 24px;
}

.model-option {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: var(--bg-card-hover);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  margin-bottom: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.model-option:hover {
  border-color: rgba(99, 102, 241, 0.3);
}

.model-option.active {
  border-color: var(--primary-color);
  background: rgba(99, 102, 241, 0.1);
}

.model-info h4 {
  margin-bottom: 6px;
  font-family: var(--font-heading);
}

.model-info p {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 10px;
}

.model-features {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.feature-tag {
  background: var(--bg-elevated);
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.api-settings {
  background: var(--bg-card-hover);
  border: 1px solid var(--border-light);
  padding: 20px;
  border-radius: var(--radius-md);
  margin-bottom: 20px;
}

.api-settings h4 {
  margin-bottom: 16px;
  font-family: var(--font-heading);
}

.theme-options {
  display: flex;
  gap: 20px;
}

.theme-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px;
  background: var(--bg-card-hover);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.theme-option:hover {
  border-color: rgba(99, 102, 241, 0.3);
}

.theme-option.active {
  border-color: var(--primary-color);
  background: rgba(99, 102, 241, 0.1);
}

.theme-option span {
  color: var(--text-secondary);
  font-size: 13px;
}

.theme-preview {
  width: 60px;
  height: 40px;
  border-radius: var(--radius-sm);
}

.theme-preview.dark {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.theme-preview.light {
  background: #ffffff;
  border: 1px solid #e5e7eb;
}

.theme-preview.auto {
  background: linear-gradient(90deg, var(--bg-card), #ffffff);
}

.shortcuts-list {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 16px;
}

.shortcut-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-color);
}

.shortcut-item:last-child {
  border-bottom: none;
}

.shortcut-name {
  display: block;
  font-weight: bold;
  font-family: var(--font-heading);
}

.shortcut-desc {
  color: var(--text-secondary);
  font-size: 13px;
}

.shortcut-keys {
  display: flex;
  gap: 6px;
}

.key {
  background: var(--bg-elevated);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-family: 'SF Mono', 'Fira Code', monospace;
  color: var(--text-secondary);
}

.privacy-info {
  background: var(--bg-card-hover);
  border: 1px solid var(--border-light);
  padding: 16px;
  border-radius: var(--radius-md);
  margin-bottom: 20px;
}

.privacy-info h4 {
  margin-bottom: 10px;
  font-family: var(--font-heading);
}

.privacy-info p {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 12px;
}
</style>
