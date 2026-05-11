<template>
  <div class="home-container">
    <el-container>
      <el-header class="navbar">
        <div class="logo">
          <PlayCircle class="logo-icon" />
          <span>AI漫剧创作平台</span>
        </div>
        <div class="nav-center">
          <el-button-group>
            <el-button 
              :type="mode === 'normal' ? 'primary' : 'default'"
              @click="switchMode('normal')"
            >
              普通模式
            </el-button>
            <el-button 
              :type="mode === 'xixi' ? 'primary' : 'default'"
              @click="switchMode('xixi')"
            >
              嘻嘻模式
            </el-button>
          </el-button-group>
        </div>
        <div class="nav-right">
            <el-badge :value="0" class="notification">
              <Bell class="icon" />
            </el-badge>
            
            <template v-if="isLoggedIn">
              <el-dropdown>
                <div class="user-profile" @click.stop>
                  <el-avatar 
                    :src="user.avatar || undefined" 
                    :icon="User" 
                    class="user-avatar"
                  />
                  <div class="user-info">
                    <span class="user-name">{{ user.nickname || '用户' }}</span>
                    <span class="user-email">{{ user.email }}</span>
                  </div>
                  <ChevronDown class="dropdown-icon" />
                </div>
                <template #dropdown>
                  <div class="dropdown-header">
                    <el-avatar 
                      :src="user.avatar || undefined" 
                      :icon="User" 
                      class="dropdown-avatar"
                    />
                    <div class="dropdown-user-info">
                      <span class="dropdown-name">{{ user.nickname || '用户' }}</span>
                      <span class="dropdown-email">{{ user.email }}</span>
                    </div>
                  </div>
                  <el-dropdown-item @click="$router.push('/profile')">
                    <User class="dropdown-item-icon" />
                    <span>个人中心</span>
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/settings')">
                    <Settings class="dropdown-item-icon" />
                    <span>设置</span>
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <LogOut class="dropdown-item-icon" />
                    <span>退出登录</span>
                  </el-dropdown-item>
                </template>
              </el-dropdown>
            </template>
            
            <template v-else>
              <div class="login-btn-wrapper" @click="openLoginModal">
                <el-avatar :icon="User" class="guest-avatar" />
                <span class="login-text">登录</span>
              </div>
            </template>
          </div>
          
          <LoginModal ref="loginModal" @login-success="handleLoginSuccess" />
      </el-header>

      <el-container>
        <el-aside width="220px" class="sidebar">
          <el-menu :default-active="activeMenu" class="menu" @select="handleMenuSelect">
            <el-menu-item index="home">
              <Home class="icon" />
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="editor">
              <PenTool class="icon" />
              <span>创作画布</span>
            </el-menu-item>

            <el-menu-item index="library">
              <FolderOpen class="icon" />
              <span>资料库</span>
            </el-menu-item>
            <el-menu-item index="groups">
              <Users class="icon" />
              <span>组别管理</span>
            </el-menu-item>
          </el-menu>
          
          <div class="model-selector">
            <el-select 
              v-model="selectedModel" 
              placeholder="选择模型"
              @change="updateModel"
            >
              <el-option label="DeepSeek" value="deepseek" />
              <el-option label="Qwen" value="qwen" />
              <el-option label="Jimeng" value="jimeng" />
            </el-select>
          </div>
        </el-aside>

        <el-main class="content-area">
          <div class="welcome-banner">
            <h1 class="welcome-title">AI 漫剧创作平台</h1>
            <p class="welcome-sub">让 AI 为您的创意插上翅膀 — 文本生图、图生视频、智能剧本，一站式创作</p>
          </div>
          <div class="chat-section">
            <div class="section-header">
              <h2>
                <Wand2 v-if="mode === 'xixi'" class="section-header-icon xixi-icon" />
                <MessageSquare v-else class="section-header-icon" />
                {{ mode === 'xixi' ? '嘻嘻模式 - 全自动创作' : '智能助手' }}
              </h2>
              <div class="mode-badge">
                <span :class="mode === 'xixi' ? 'xixi-badge' : 'normal-badge'">
                  {{ mode === 'xixi' ? '嘻嘻模式' : '普通模式' }}
                </span>
              </div>
            </div>
            
            <div class="chat-container" ref="chatContainer">
              <div 
                v-for="(msg, index) in messages" 
                :key="index"
                :class="['message', msg.sender === 'bot' ? 'bot-message' : 'user-message']"
              >
                <el-avatar :icon="msg.sender === 'bot' ? Bot : User" />
                <div class="message-content">
                  <p>{{ msg.content }}</p>
                  
                  <!-- 剧本展示 -->
                  <div v-if="msg.type === 'script' && msg.scenes" class="script-section">
                    <el-collapse v-if="msg.collapsible">
                      <el-collapse-item title="查看场景详情">
                        <div v-for="(scene, idx) in msg.scenes" :key="idx" class="scene-item">
                          <h4>场景 {{ scene.sceneNumber }}</h4>
                          <p>{{ scene.description }}</p>
                        </div>
                      </el-collapse-item>
                    </el-collapse>
                    <div v-else>
                      <div v-for="(scene, idx) in msg.scenes" :key="idx" class="scene-item">
                        <h4>场景 {{ scene.sceneNumber }}</h4>
                        <p>{{ scene.description }}</p>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 提示词优化结果 -->
                  <div v-if="msg.type === 'optimization-result' && msg.optimization" class="optimization-section">
                    <el-collapse>
                      <el-collapse-item :title="`查看优化详情 (满意度: ${msg.optimization.optimizationResult.satisfactionScore.toFixed(1)}%)`">
                        <div class="optimization-detail">
                          <h5>原始描述</h5>
                          <p>{{ msg.optimization.originalDescription }}</p>
                          <h5>优化后提示词</h5>
                          <p class="optimized-prompt">{{ msg.optimization.optimizationResult.optimizedPrompt }}</p>
                          <h5>想象画面</h5>
                          <p>{{ msg.optimization.optimizationResult.imaginedDescription }}</p>
                          <h5>迭代记录 (共 {{ msg.optimization.optimizationResult.iterationCount }} 轮)</h5>
                          <el-timeline>
                            <el-timeline-item 
                              v-for="(log, idx) in msg.optimization.optimizationResult.iterationLogs" 
                              :key="idx"
                              :timestamp="`第 ${log.iteration} 轮`"
                            >
                              <p>满意度: {{ log.satisfactionScore.toFixed(1) }}%</p>
                            </el-timeline-item>
                          </el-timeline>
                        </div>
                      </el-collapse-item>
                    </el-collapse>
                  </div>
                  
                  <!-- 生成的图片 -->
                  <div v-if="msg.type === 'images' && msg.images" class="images-section">
                    <el-collapse>
                      <el-collapse-item title="查看生成的图片">
                        <div class="images-grid">
                          <div v-for="(img, idx) in msg.images" :key="idx" class="image-item">
                            <el-image :src="img" fit="cover" />
                            <span>场景 {{ idx + 1 }}</span>
                          </div>
                        </div>
                      </el-collapse-item>
                    </el-collapse>
                  </div>
                  
                  <!-- 视频流程 -->
                  <div v-if="msg.type === 'video-flow' && msg.videoFlow" class="video-flow-section">
                    <el-collapse>
                      <el-collapse-item :title="`查看视频流程 (${msg.videoFlow.duration})`">
                        <div class="video-flow-detail">
                          <p><strong>转场风格:</strong> {{ msg.videoFlow.transitionStyle }}</p>
                          <p><strong>音乐建议:</strong> {{ msg.videoFlow.musicSuggestion }}</p>
                          <h5>视频片段</h5>
                          <el-timeline>
                            <el-timeline-item 
                              v-for="(segment, idx) in msg.videoFlow.videoSegments" 
                              :key="idx"
                              :timestamp="`片段 ${segment.order}`"
                            >
                              <p>{{ segment.description }}</p>
                              <p>时长: {{ segment.duration }}秒</p>
                              <p v-if="segment.effect">特效: {{ segment.effect }}</p>
                            </el-timeline-item>
                          </el-timeline>
                        </div>
                      </el-collapse-item>
                    </el-collapse>
                  </div>
                  
                  <!-- 最终视频 -->
                  <div v-if="msg.type === 'video-complete' && msg.videoUrl" class="video-complete-section">
                    <video :src="msg.videoUrl" controls class="final-video" />
                    <el-button type="primary" @click="downloadVideo(msg.videoUrl)">下载视频</el-button>
                  </div>
                  
                  <div v-if="msg.steps" class="steps-info">
                    <el-timeline>
                      <el-timeline-item 
                        v-for="(step, idx) in msg.steps" 
                        :key="idx"
                        :timestamp="'步骤' + (idx + 1)"
                      >
                        <span :class="step.status">{{ step.tool }}</span>
                      </el-timeline-item>
                    </el-timeline>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="chat-input">
              <el-input 
                v-model="inputMessage" 
                placeholder="输入您的创作需求..."
                @keyup.enter="sendMessage"
              />
              <el-button type="primary" @click="sendMessage">
                <Send class="icon" />
              </el-button>
            </div>
          </div>

          <div class="quick-stats">
            <h3>我的创作</h3>
            <div class="stats-grid">
              <div class="stat-card">
                <div class="stat-icon">
                  <ImageIcon class="icon" />
                </div>
                <div class="stat-info">
                  <span class="stat-value">128</span>
                  <span class="stat-label">生成图片</span>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon">
                  <Video class="icon" />
                </div>
                <div class="stat-info">
                  <span class="stat-value">24</span>
                  <span class="stat-label">生成视频</span>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon">
                  <FileText class="icon" />
                </div>
                <div class="stat-info">
                  <span class="stat-value">16</span>
                  <span class="stat-label">创作脚本</span>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon">
                  <Clock class="icon" />
                </div>
                <div class="stat-info">
                  <span class="stat-value">12h</span>
                  <span class="stat-label">创作时长</span>
                </div>
              </div>
            </div>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import {
  PlayCircle, Bell, User, Home, Sparkles,
  Image as ImageIcon, Video, FolderOpen,
  Users, Send, Bot, PenTool, FileText, Clock,
  ChevronDown, Settings, LogOut, MessageSquare, Wand2
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import LoginModal from '../components/LoginModal.vue'
import xixiAPI from '../api/xixi.js'

const mode = ref('normal')
const activeMenu = ref('home')
const selectedModel = ref('deepseek')
const inputMessage = ref('')
const messages = ref([
  { sender: 'bot', content: '您好！我是AI漫剧助手，请问您需要创作什么内容？' }
])
const chatContainer = ref(null)
const loginModal = ref(null)

// 嘻嘻模式相关状态
const xixiSession = ref(null)
const xixiWaitingFor = ref(null)
const xixiLogs = ref([])
const xixiLoading = ref(false)

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const isLoggedIn = computed(() => {
  return localStorage.getItem('token') && Object.keys(user.value).length > 0
})

const openLoginModal = () => {
  loginModal.value?.open()
}

const handleLoginSuccess = (userData) => {
  user.value = userData
}

const checkLogin = () => {
  if (!isLoggedIn.value) {
    ElMessage.info('请先登录以使用完整功能')
    return false
  }
  return true
}

const switchMode = (newMode) => {
  mode.value = newMode
  localStorage.setItem('mode', newMode)
  ElMessage.info(`已切换到${newMode === 'xixi' ? '嘻嘻模式' : '普通模式'}`)
}

const updateModel = () => {
  localStorage.setItem('model', selectedModel.value)
  ElMessage.info(`已切换到${selectedModel.value}模型`)
}

const handleMenuSelect = (index) => {
  activeMenu.value = index
  if (index === 'editor') {
    window.location.href = '/editor'
  } else if (index === 'library') {
    window.location.href = '/library'
  } else if (index === 'groups') {
    window.location.href = '/groups'
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  user.value = {}
  ElMessage.success('已退出登录')
}

const sendMessage = async () => {
  if (!inputMessage.value.trim()) return
  
  if (!checkLogin()) {
    openLoginModal()
    return
  }
  
  const message = inputMessage.value
  
  // 嘻嘻模式下的用户确认流程
  if (mode.value === 'xixi' && xixiWaitingFor.value) {
    await handleXixiUserInput(message)
    inputMessage.value = ''
    return
  }
  
  messages.value.push({
    sender: 'user',
    content: message
  })
  
  inputMessage.value = ''
  
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
  
  // 嘻嘻模式
  if (mode.value === 'xixi') {
    await startXixiMode(message)
  } else {
    // 普通模式
    messages.value.push({
      sender: 'bot',
      content: `💬 已收到您的需求："${message}"\n\n我来帮您分析并执行相应操作。`
    })
  }
  
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// 启动嘻嘻模式
const startXixiMode = async (prompt) => {
  xixiLoading.value = true
  messages.value.push({
    sender: 'bot',
    content: '🎭 【嘻嘻模式】正在启动全自动创作流程...',
    type: 'xixi-start'
  })
  
  try {
    const response = await xixiAPI.startSession(prompt, '日系动漫', selectedModel.value)
    const data = response.data
    
    xixiSession.value = data.sessionId
    xixiWaitingFor.value = data.waitingFor
    xixiLogs.value = data.logs || []
    
    // 显示剧本
    if (data.scenes && data.scenes.length > 0) {
      messages.value.push({
        sender: 'bot',
        content: `📖 剧本生成完成！共 ${data.scenes.length} 个场景：`,
        scenes: data.scenes,
        type: 'script',
        collapsible: true
      })
      
      messages.value.push({
        sender: 'bot',
        content: '💭 请查看以上场景列表，如果需要添加、修改或删除场景，请告诉我。如果满意，请回复"确认"开始生成图片。',
        type: 'user-confirm'
      })
    }
  } catch (error) {
    messages.value.push({
      sender: 'bot',
      content: '❌ 启动嘻嘻模式失败：' + (error.response?.data?.error || error.message)
    })
  } finally {
    xixiLoading.value = false
  }
}

// 处理嘻嘻模式下的用户输入
const handleXixiUserInput = async (input) => {
  xixiLoading.value = true
  
  messages.value.push({
    sender: 'user',
    content: input
  })
  
  try {
    if (xixiWaitingFor.value === 'script_confirmation') {
      const response = await xixiAPI.confirmScript(xixiSession.value, input, selectedModel.value)
      const data = response.data
      
      xixiWaitingFor.value = data.waitingFor
      xixiLogs.value = data.logs || []
      
      if (input === '确认' || input === '开始') {
        // 显示提示词优化过程
        messages.value.push({
          sender: 'bot',
          content: '✨ 开始优化每个场景的绘画提示词...',
          type: 'optimization-start'
        })
        
        // 显示每个场景的优化结果
        if (data.sceneOptimizations) {
          data.sceneOptimizations.forEach((opt, index) => {
            messages.value.push({
              sender: 'bot',
              content: `场景 ${index + 1} 优化完成！满意度: ${opt.optimizationResult.satisfactionScore.toFixed(1)}% (第 ${opt.optimizationResult.iterationCount} 轮)`,
              optimization: opt,
              type: 'optimization-result',
              collapsible: true
            })
          })
        }
        
        // 显示生成的图片
        if (data.generatedImages && data.generatedImages.length > 0) {
          messages.value.push({
            sender: 'bot',
            content: `🎨 所有场景图片生成完成！共 ${data.generatedImages.length} 张：`,
            images: data.generatedImages,
            type: 'images',
            collapsible: true
          })
        }
        
        // 显示视频流程
        if (data.videoFlow) {
          messages.value.push({
            sender: 'bot',
            content: `🎬 视频流程规划完成！总时长: ${data.videoFlow.duration}，共 ${data.videoFlow.videoSegments.length} 个片段`,
            videoFlow: data.videoFlow,
            type: 'video-flow',
            collapsible: true
          })
          
          messages.value.push({
            sender: 'bot',
            content: '💭 请查看以上视频流程，如果需要调整时长、转场效果或添加特效，请告诉我。如果满意，请回复"确认"开始生成视频。',
            type: 'user-confirm'
          })
        }
      } else {
        // 用户有修改
        messages.value.push({
          sender: 'bot',
          content: '📝 剧本已根据您的要求更新，请再次确认。',
          scenes: data.scenes,
          type: 'script',
          collapsible: true
        })
      }
    } else if (xixiWaitingFor.value === 'video_flow_confirmation') {
      const response = await xixiAPI.confirmVideoFlow(xixiSession.value, input, selectedModel.value)
      const data = response.data
      
      xixiWaitingFor.value = data.waitingFor
      
      if (input === '确认' || input === '开始') {
        messages.value.push({
          sender: 'bot',
          content: '🎥 正在生成最终视频...',
          type: 'video-generating'
        })
        
        if (data.finalVideoUrl) {
          messages.value.push({
            sender: 'bot',
            content: '🎉 视频生成完成！',
            videoUrl: data.finalVideoUrl,
            type: 'video-complete'
          })
          
          // 重置状态
          xixiSession.value = null
          xixiWaitingFor.value = null
        }
      } else {
        messages.value.push({
          sender: 'bot',
          content: '📝 视频流程已根据您的要求更新，请再次确认。',
          videoFlow: data.videoFlow,
          type: 'video-flow',
          collapsible: true
        })
      }
    }
  } catch (error) {
    messages.value.push({
      sender: 'bot',
      content: '❌ 处理失败：' + (error.response?.data?.error || error.message)
    })
  } finally {
    xixiLoading.value = false
  }
}

const downloadVideo = (url) => {
  const a = document.createElement('a')
  a.href = url
  a.download = 'xixi-video.mp4'
  a.click()
}

onMounted(() => {
  const savedMode = localStorage.getItem('mode')
  if (savedMode) {
    mode.value = savedMode
  }
  const savedModel = localStorage.getItem('model')
  if (savedModel) {
    selectedModel.value = savedModel
  }
})
</script>

<style scoped>
.home-container {
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
  position: relative;
  z-index: 10;
}

.navbar::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
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

.nav-center {
  display: flex;
  gap: 8px;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification {
  cursor: pointer;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  color: white;
  padding: 6px 12px;
  border-radius: 12px;
  transition: background var(--transition-base);
}

.user-profile:hover {
  background: rgba(255, 255, 255, 0.1);
}

.login-btn-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: white;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  transition: all var(--transition-base);
}

.login-btn-wrapper:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

.guest-avatar {
  width: 28px;
  height: 28px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.login-text {
  font-size: 14px;
  font-weight: 500;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: white;
}

.user-email {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.dropdown-icon {
  width: 16px;
  height: 16px;
  color: rgba(255, 255, 255, 0.7);
  transition: transform var(--transition-slow);
}

.user-profile:hover .dropdown-icon {
  transform: rotate(180deg);
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 8px;
}

.dropdown-avatar {
  width: 48px;
  height: 48px;
}

.dropdown-user-info {
  display: flex;
  flex-direction: column;
}

.dropdown-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.dropdown-email {
  font-size: 13px;
  color: var(--text-secondary);
}

.dropdown-item-icon {
  width: 16px;
  height: 16px;
  margin-right: 8px;
}

.icon {
  width: 20px;
  height: 20px;
}

.sidebar {
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.menu {
  border-right: none;
  flex: 1;
}

.model-selector {
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.model-selector :deep(.el-input__wrapper) {
  background: var(--bg-card-hover);
}

.content-area {
  padding: 24px;
  background: var(--bg-dark);
}

/* ===== Welcome Banner ===== */
.welcome-banner {
  text-align: center;
  padding: 32px 24px 24px;
  margin-bottom: 24px;
}

.welcome-title {
  font-size: 28px;
  font-weight: 700;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.welcome-sub {
  font-size: 15px;
  color: var(--text-secondary);
  max-width: 500px;
  margin: 0 auto;
  line-height: 1.6;
}

/* ===== Section Headers ===== */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  position: relative;
}

.section-header h2 {
  font-family: var(--font-heading);
  font-size: 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}

.section-header h2::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 32px;
  height: 3px;
  background: var(--gradient-primary);
  border-radius: 2px;
}

.section-header-icon {
  width: 22px;
  height: 22px;
  color: var(--primary-color);
}

.xixi-icon {
  color: #ec4899;
}

.mode-badge {
  display: flex;
  gap: 8px;
}

.xixi-badge {
  background: linear-gradient(135deg, #f472b6, #ec4899);
  color: white;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.normal-badge {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  color: white;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

/* ===== Chat Section ===== */
.chat-section {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.chat-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.3), transparent);
}

.chat-container {
  height: 400px;
  overflow-y: auto;
  margin-bottom: 20px;
  padding-right: 4px;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  animation: fadeIn 0.3s ease-out;
}

.message-content {
  background: var(--bg-card-hover);
  padding: 14px 18px;
  border-radius: 0 var(--radius-md) var(--radius-md) var(--radius-md);
  max-width: 70%;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.bot-message .message-content {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(139, 92, 246, 0.1));
  border-color: rgba(99, 102, 241, 0.2);
}

.bot-message {
  flex-direction: row;
}

.user-message {
  flex-direction: row-reverse;
}

.user-message .message-content {
  border-radius: var(--radius-md) 0 var(--radius-md) var(--radius-md);
  background: rgba(99, 102, 241, 0.2);
  border-color: rgba(99, 102, 241, 0.3);
}

.steps-info {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.chat-input {
  display: flex;
  gap: 12px;
}

.chat-input :deep(.el-input__wrapper) {
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
}

.chat-input :deep(.el-input__wrapper:hover) {
  border-color: var(--primary-color);
}

/* ===== Tools Section ===== */
.tools-section {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.tools-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.3), transparent);
}

.tools-section h3 {
  font-family: var(--font-heading);
  font-size: 16px;
  color: var(--text-primary);
  position: relative;
  display: inline-block;
}

.tools-section h3::after {
  content: '';
  position: absolute;
  bottom: -3px;
  left: 0;
  width: 28px;
  height: 2px;
  background: var(--gradient-primary);
  border-radius: 2px;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.tool-card {
  background: var(--bg-card-hover);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 28px 24px;
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-slow);
  position: relative;
  overflow: hidden;
}

.tool-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  opacity: 0;
  transition: opacity var(--transition-base);
}

.tool-card.text2image::before {
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
}

.tool-card.image2image::before {
  background: linear-gradient(90deg, #10b981, #059669);
}

.tool-card.image2video::before {
  background: linear-gradient(90deg, #f59e0b, #d97706);
}

.tool-card.script::before {
  background: linear-gradient(90deg, #8b5cf6, #7c3aed);
}

.tool-card:hover {
  transform: translateY(-6px);
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: var(--shadow-glow);
}

.tool-card:hover::before {
  opacity: 1;
}

.tool-card h4 {
  font-family: var(--font-heading);
  font-size: 16px;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.tool-card p {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.tool-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform var(--transition-base);
  position: relative;
}

.tool-card:hover .tool-icon {
  transform: scale(1.1);
}

.tool-icon::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 20px;
  opacity: 0;
  transition: opacity var(--transition-base);
}

.tool-card:hover .tool-icon::after {
  opacity: 1;
}

.tool-icon.text2image {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}

.tool-icon.image2image {
  background: linear-gradient(135deg, #10b981, #059669);
}

.tool-icon.image2video {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.tool-icon.script {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
}

.tool-icon .icon {
  width: 30px;
  height: 30px;
  color: white;
}

/* ===== Stats Section ===== */
.quick-stats {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 24px;
  position: relative;
  overflow: hidden;
}

.quick-stats::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.3), transparent);
}

.quick-stats h3 {
  font-family: var(--font-heading);
  font-size: 16px;
  color: var(--text-primary);
  position: relative;
  display: inline-block;
}

.quick-stats h3::after {
  content: '';
  position: absolute;
  bottom: -3px;
  left: 0;
  width: 28px;
  height: 2px;
  background: var(--gradient-primary);
  border-radius: 2px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--bg-card-hover);
  padding: 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  transition: all var(--transition-base);
}

.stat-card:hover {
  border-color: rgba(99, 102, 241, 0.25);
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow);
}

.stat-icon {
  width: 48px;
  height: 48px;
  background: var(--gradient-primary);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon .icon {
  width: 24px;
  height: 24px;
  color: white;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: var(--text-primary);
  font-family: var(--font-heading);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* ===== Script Section (Xixi Mode) ===== */
.script-section {
  margin-top: 12px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: var(--radius-sm);
}

.scene-item {
  padding: 12px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-light);
}

.scene-item h4 {
  margin: 0 0 8px 0;
  color: var(--primary-color);
  font-size: 14px;
  font-family: var(--font-heading);
}

.scene-item p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.optimization-section {
  margin-top: 12px;
}

.optimization-detail {
  padding: 12px;
}

.optimization-detail h5 {
  margin: 12px 0 8px 0;
  color: var(--primary-color);
  font-size: 13px;
  font-family: var(--font-heading);
}

.optimization-detail p {
  margin: 0 0 12px 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.optimized-prompt {
  background: rgba(99, 102, 241, 0.1);
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid var(--primary-color);
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 12px;
}

.images-section {
  margin-top: 12px;
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
  padding: 12px;
}

.image-item {
  text-align: center;
}

.image-item .el-image {
  width: 100%;
  height: 150px;
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.image-item span {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.video-flow-section {
  margin-top: 12px;
}

.video-flow-detail {
  padding: 12px;
}

.video-flow-detail h5 {
  margin: 16px 0 8px 0;
  color: var(--primary-color);
  font-size: 14px;
  font-family: var(--font-heading);
}

.video-flow-detail p {
  margin: 4px 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.video-complete-section {
  margin-top: 16px;
  text-align: center;
}

.final-video {
  width: 100%;
  max-width: 600px;
  border-radius: var(--radius-md);
  margin-bottom: 16px;
  box-shadow: var(--shadow-md);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .content-area {
    padding: 16px;
  }

  .chat-section,
  .tools-section,
  .quick-stats {
    padding: 16px;
  }

  .tools-grid {
    grid-template-columns: 1fr 1fr;
  }

  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }

  .message-content {
    max-width: 85%;
  }

  .nav-center {
    display: none;
  }
}
</style>
