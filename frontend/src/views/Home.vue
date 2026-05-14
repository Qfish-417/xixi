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
                  <!-- 流式文字输出 -->
                  <div class="text-content">
                    <span v-if="msg.isStreaming" class="typing-indicator">
                      <span class="typing-dot"></span>
                      <span class="typing-dot"></span>
                      <span class="typing-dot"></span>
                    </span>
                    <span v-html="msg.displayContent || msg.content"></span>
                  </div>
                  
                  <!-- 用户上传的图片 -->
                  <div v-if="msg.images && msg.images.length > 0" class="user-images-section">
                    <div class="images-grid">
                      <div v-for="(img, idx) in msg.images" :key="idx" class="image-card user-image">
                        <div class="image-wrapper">
                          <el-image :src="img" fit="cover" @error="handleImageError($event, idx)" />
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 剧本展示 - 直接展开显示 -->
                  <div v-if="msg.type === 'script' && msg.scenes" class="script-section">
                    <div class="section-title">
                      <FileText class="section-icon" />
                      <span>剧本场景</span>
                    </div>
                    <div class="scenes-list">
                      <div v-for="(scene, idx) in msg.scenes" :key="idx" class="scene-card">
                        <div class="scene-header">
                          <span class="scene-number">场景 {{ scene.sceneNumber }}</span>
                          <span class="scene-label">{{ scene.location || '室内' }}</span>
                        </div>
                        <p class="scene-description">{{ scene.description }}</p>
                        <div v-if="scene.characters" class="scene-characters">
                          <span class="char-label">登场角色：</span>
                          <span v-for="(char, cIdx) in scene.characters" :key="cIdx" class="character-tag">{{ char }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 提示词优化结果 -->
                  <div v-if="msg.type === 'optimization-result' && msg.optimization" class="optimization-section">
                    <div class="section-title">
                      <Sparkles class="section-icon" />
                      <span>提示词优化</span>
                      <span class="satisfaction-badge">满意度: {{ msg.optimization.optimizationResult.satisfactionScore.toFixed(1) }}%</span>
                    </div>
                    <div class="optimization-card">
                      <div class="optimization-item">
                        <h4>原始描述</h4>
                        <p class="original-text">{{ msg.optimization.originalDescription }}</p>
                      </div>
                      <div class="optimization-item">
                        <h4>优化后提示词</h4>
                        <p class="optimized-text">{{ msg.optimization.optimizationResult.optimizedPrompt }}</p>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 生成的图片 - 直接显示在文字下方 -->
                  <div v-if="msg.type === 'images' && msg.images" class="images-section">
                    <div class="section-title">
                      <ImageIcon class="section-icon" />
                      <span>生成图片</span>
                      <span class="count-badge">{{ msg.images.length }} 张</span>
                    </div>
                    <div class="images-grid">
                      <div v-for="(img, idx) in msg.images" :key="idx" class="image-card">
                        <div class="image-wrapper">
                          <el-image :src="img" fit="cover" @error="handleImageError($event, idx)" />
                          <div class="image-overlay">
                            <span class="image-index">场景 {{ idx + 1 }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 视频流程 -->
                  <div v-if="msg.type === 'video-flow' && msg.videoFlow" class="video-flow-section">
                    <div class="section-title">
                      <Video class="section-icon" />
                      <span>视频流程</span>
                      <span class="duration-badge">总时长: {{ msg.videoFlow.duration }}</span>
                    </div>
                    <div class="video-flow-card">
                      <div class="flow-info">
                        <div class="info-item">
                          <span class="info-label">转场风格:</span>
                          <span class="info-value">{{ msg.videoFlow.transitionStyle }}</span>
                        </div>
                        <div class="info-item">
                          <span class="info-label">音乐建议:</span>
                          <span class="info-value">{{ msg.videoFlow.musicSuggestion }}</span>
                        </div>
                      </div>
                      <div class="segments-list">
                        <div v-for="(segment, idx) in msg.videoFlow.videoSegments" :key="idx" class="segment-item">
                          <div class="segment-header">
                            <span class="segment-number">片段 {{ segment.order }}</span>
                            <span class="segment-duration">{{ segment.duration }}秒</span>
                          </div>
                          <p class="segment-desc">{{ segment.description }}</p>
                          <span v-if="segment.effect" class="effect-tag">{{ segment.effect }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 最终视频 - 直接显示 -->
                  <div v-if="msg.type === 'video-complete' && msg.videoUrl" class="video-complete-section">
                    <div class="section-title">
                      <Film class="section-icon" />
                      <span>最终视频</span>
                    </div>
                    <div class="video-player-wrapper">
                      <video :src="msg.videoUrl" controls class="final-video">
                        <source :src="msg.videoUrl" type="video/mp4">
                      </video>
                    </div>
                    <div class="video-actions">
                      <el-button type="primary" @click="downloadVideo(msg.videoUrl)">
                        <Download class="btn-icon" />
                        下载视频
                      </el-button>
                    </div>
                  </div>
                  
                  <!-- 步骤信息 -->
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
              <div class="input-wrapper">
                <label class="upload-btn" @click="triggerImageUpload">
                  <ImageIcon class="upload-icon" />
                </label>
                <input type="file" id="chatImageUpload" accept="image/*" class="hidden-input" @change="handleChatImageUpload" />
                <el-input 
                  v-model="inputMessage" 
                  placeholder="输入您的创作需求..."
                  @keyup.enter="sendMessage"
                />
              </div>
              <el-button type="primary" @click="sendMessage" :disabled="isStreaming">
                <Send class="icon" />
              </el-button>
              <!-- 已上传图片预览 -->
              <div v-if="uploadedImages.length > 0" class="uploaded-images-preview">
                <div v-for="(img, idx) in uploadedImages" :key="idx" class="preview-item">
                  <img :src="img" class="preview-image" />
                  <span class="remove-image" @click="removeUploadedImage(idx)">×</span>
                </div>
              </div>
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

<script setup>import { ref, computed, onMounted, nextTick } from 'vue';
import { PlayCircle, Bell, User, Home, Sparkles, Image as ImageIcon, Video, FolderOpen, Users, Send, Bot, PenTool, FileText, Clock, ChevronDown, Settings, LogOut, MessageSquare, Wand2, Film, Download } from 'lucide-vue-next';
import { ElMessage } from 'element-plus';
import LoginModal from '../components/LoginModal.vue';
import xixiAPI from '../api/xixi.js';
import { agentAPI } from '../api/agent.js';
const mode = ref('normal');
const activeMenu = ref('home');
const selectedModel = ref('deepseek');
const inputMessage = ref('');
const messages = ref([
 { sender: 'bot', content: '您好！我是AI漫剧助手，请问您需要创作什么内容？' }
]);
const chatContainer = ref(null);
const loginModal = ref(null);
const isStreaming = ref(false);
// 图片上传相关
const uploadedImages = ref([]);
// 嘻嘻模式相关状态
const xixiSession = ref(null);
const xixiWaitingFor = ref(null);
const xixiLogs = ref([]);
const xixiLoading = ref(false);
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'));
const isLoggedIn = computed(() => {
 return localStorage.getItem('token') && Object.keys(user.value).length > 0;
});
const openLoginModal = () => {
 loginModal.value?.open();
};
const handleLoginSuccess = (userData) => {
 user.value = userData;
};
const checkLogin = () => {
 if (!isLoggedIn.value) {
 ElMessage.info('请先登录以使用完整功能');
 return false;
 }
 return true;
};
const switchMode = (newMode) => {
 mode.value = newMode;
 localStorage.setItem('mode', newMode);
 ElMessage.info(`已切换到${newMode === 'xixi' ? '嘻嘻模式' : '普通模式'}`);
};
const updateModel = () => {
 localStorage.setItem('model', selectedModel.value);
 ElMessage.info(`已切换到${selectedModel.value}模型`);
};
const handleMenuSelect = (index) => {
 activeMenu.value = index;
 if (index === 'editor') {
 window.location.href = '/editor';
 }
 else if (index === 'library') {
 window.location.href = '/library';
 }
 else if (index === 'groups') {
 window.location.href = '/groups';
 }
};
const handleLogout = () => {
 localStorage.removeItem('token');
 localStorage.removeItem('user');
 user.value = {};
 ElMessage.success('已退出登录');
};
// 流式文字输出函数
const streamText = async (messageIndex, fullText, speed = 30) => {
 isStreaming.value = true;
 const msg = messages.value[messageIndex];
 msg.isStreaming = true;
 msg.displayContent = '';
 for (let i = 0; i < fullText.length; i++) {
 msg.displayContent += fullText[i];
 await new Promise(resolve => setTimeout(resolve, speed));
 }
 msg.isStreaming = false;
 isStreaming.value = false;
 await nextTick(() => {
 scrollToBottom();
 });
};
// 滚动到底部
const scrollToBottom = () => {
 nextTick(() => {
 if (chatContainer.value) {
 chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
 }
 });
};
// 处理图片加载失败
const handleImageError = (event, index) => {
 event.target.src = `https://picsum.photos/seed/${index}/300/200`;
};
// 触发图片上传
const triggerImageUpload = () => {
 document.getElementById('chatImageUpload').click();
};
// 处理图片上传
const handleChatImageUpload = (event) => {
 const files = event.target.files;
 if (!files || files.length === 0) return;
 Array.from(files).forEach(file => {
 const reader = new FileReader();
 reader.onload = (e) => {
 uploadedImages.value.push(e.target.result);
 };
 reader.readAsDataURL(file);
 });
 event.target.value = '';
};
// 移除已上传的图片
const removeUploadedImage = (index) => {
 uploadedImages.value.splice(index, 1);
};
const sendMessage = async () => {
 if (!inputMessage.value.trim() && uploadedImages.value.length === 0)
 return;
 if (!checkLogin()) {
 openLoginModal();
 return;
 }
 const message = inputMessage.value;
 // 构建消息对象，包含文本和图片
 const userMessage = {
 sender: 'user',
 content: message,
 images: uploadedImages.value.length > 0 ? [...uploadedImages.value] : undefined
 };
 // 嘻嘻模式下的用户确认流程
 if (mode.value === 'xixi' && xixiWaitingFor.value) {
 messages.value.push(userMessage);
 await handleXixiUserInput(message);
 inputMessage.value = '';
 uploadedImages.value = [];
 return;
 }
 messages.value.push(userMessage);
 inputMessage.value = '';
 uploadedImages.value = [];
 scrollToBottom();
 // 嘻嘻模式
 if (mode.value === 'xixi') {
 await startXixiMode(message);
 }
 else {
 // 普通模式 - 调用后端API获取响应
 const botIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true
 }) - 1;
 try {
 const response = await agentAPI.master({
 input: message,
 modelName: selectedModel.value
 });
 const result = response.data;
 if (result.message) {
 await streamText(botIndex, result.message);
 // 如果有图片，直接显示在文字下方
 if (result.images && result.images.length > 0) {
 messages.value.push({
 sender: 'bot',
 content: '',
 type: 'images',
 images: result.images
 });
 }
 // 如果有视频，直接显示
 if (result.videoUrl) {
 messages.value.push({
 sender: 'bot',
 content: '',
 type: 'video-complete',
 videoUrl: result.videoUrl
 });
 }
 }
 else {
 await streamText(botIndex, '抱歉，我暂时无法处理您的请求。');
 }
 }
 catch (error) {
 console.error('API请求失败:', error);
 await streamText(botIndex, '❌ 请求失败：' + (error.response?.data?.error || error.message));
 }
 }
};
// 启动嘻嘻模式
const startXixiMode = async (prompt) => {
 xixiLoading.value = true;
 const startMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true
 }) - 1;
 await streamText(startMsgIndex, '🎭 【嘻嘻模式】正在启动全自动创作流程...');
 try {
 const response = await xixiAPI.startSession(prompt, '日系动漫', selectedModel.value);
 const data = response.data;
 xixiSession.value = data.sessionId;
 xixiWaitingFor.value = data.waitingFor;
 xixiLogs.value = data.logs || [];
 // 显示剧本
 if (data.scenes && data.scenes.length > 0) {
 const scriptMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true,
 type: 'script',
 scenes: data.scenes
 }) - 1;
 await streamText(scriptMsgIndex, `📖 剧本生成完成！共 ${data.scenes.length} 个场景`);
 // 添加用户确认消息（流式输出）
 const confirmMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true,
 type: 'user-confirm'
 }) - 1;
 await streamText(confirmMsgIndex, '💭 请查看以上场景列表，如果需要添加、修改或删除场景，请告诉我。如果满意，请回复"确认"开始生成图片。');
 }
 }
 catch (error) {
 const errorMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true
 }) - 1;
 await streamText(errorMsgIndex, '❌ 启动嘻嘻模式失败：' + (error.response?.data?.error || error.message));
 }
 finally {
 xixiLoading.value = false;
 }
};
// 处理嘻嘻模式下的用户输入
const handleXixiUserInput = async (input) => {
 xixiLoading.value = true;
 messages.value.push({
 sender: 'user',
 content: input
 });
 scrollToBottom();
 try {
 if (xixiWaitingFor.value === 'script_confirmation') {
 const response = await xixiAPI.confirmScript(xixiSession.value, input, selectedModel.value);
 const data = response.data;
 xixiWaitingFor.value = data.waitingFor;
 xixiLogs.value = data.logs || [];
 if (input === '确认' || input === '开始') {
 // 显示提示词优化过程（流式）
 const optStartMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true,
 type: 'optimization-start'
 }) - 1;
 await streamText(optStartMsgIndex, '✨ 开始优化每个场景的绘画提示词...');
 // 显示每个场景的优化结果
 if (data.sceneOptimizations) {
 for (let i = 0; i < data.sceneOptimizations.length; i++) {
 const opt = data.sceneOptimizations[i];
 const optMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true,
 type: 'optimization-result',
 optimization: opt
 }) - 1;
 await streamText(optMsgIndex, `场景 ${i + 1} 优化完成！满意度: ${opt.optimizationResult.satisfactionScore.toFixed(1)}%`);
 }
 }
 // 显示生成的图片（直接显示，不带流式）
 if (data.generatedImages && data.generatedImages.length > 0) {
 const imagesMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true,
 type: 'images',
 images: data.generatedImages
 }) - 1;
 await streamText(imagesMsgIndex, `🎨 图片生成完成！共 ${data.generatedImages.length} 张图片`);
 }
 // 显示视频流程
 if (data.videoFlow) {
 const flowMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true,
 type: 'video-flow',
 videoFlow: data.videoFlow
 }) - 1;
 await streamText(flowMsgIndex, `🎬 视频流程规划完成！总时长: ${data.videoFlow.duration}`);
 // 添加用户确认消息（包含图片选项）
 const confirmMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true,
 type: 'user-confirm'
 }) - 1;
 await streamText(confirmMsgIndex, '💭 接下来请选择生成方式：\n\n' +
 '1. 【生成图片】- 先为每个场景生成图片，然后基于图片生成视频（推荐）\n' +
 '2. 【直接生成】- 跳过图片步骤，直接根据剧本描述生成视频\n\n' +
 '如果需要调整时长、转场效果或添加特效，也可以告诉我。\n' +
 '请回复"生成图片"或"直接生成"开始创作。');
 }
 }
 else {
 // 用户有修改
 const scriptMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true,
 type: 'script',
 scenes: data.scenes
 }) - 1;
 await streamText(scriptMsgIndex, '📝 剧本已根据您的要求更新，请再次确认。');
 }
 }
 else if (xixiWaitingFor.value === 'video_flow_confirmation') {
 const response = await xixiAPI.confirmVideoFlow(xixiSession.value, input, selectedModel.value);
 const data = response.data;
 xixiWaitingFor.value = data.waitingFor;
 if (input === '确认' || input === '开始') {
 const videoGenMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true,
 type: 'video-generating'
 }) - 1;
 await streamText(videoGenMsgIndex, '🎥 正在生成最终视频...');
 if (data.finalVideoUrl) {
 const videoCompleteMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true,
 type: 'video-complete',
 videoUrl: data.finalVideoUrl
 }) - 1;
 await streamText(videoCompleteMsgIndex, '🎉 视频生成完成！');
 // 重置状态
 xixiSession.value = null;
 xixiWaitingFor.value = null;
 }
 }
 else {
 const flowMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true,
 type: 'video-flow',
 videoFlow: data.videoFlow
 }) - 1;
 await streamText(flowMsgIndex, '📝 视频流程已根据您的要求更新，请再次确认。');
 }
 }
 }
 catch (error) {
 const errorMsgIndex = messages.value.push({
 sender: 'bot',
 content: '',
 isStreaming: true
 }) - 1;
 await streamText(errorMsgIndex, '❌ 处理失败：' + (error.response?.data?.error || error.message));
 }
 finally {
 xixiLoading.value = false;
 }
};
const downloadVideo = (url) => {
 const a = document.createElement('a');
 a.href = url;
 a.download = 'xixi-video.mp4';
 a.click();
};
onMounted(() => {
 const savedMode = localStorage.getItem('mode');
 if (savedMode) {
 mode.value = savedMode;
 }
 const savedModel = localStorage.getItem('model');
 if (savedModel) {
 selectedModel.value = savedModel;
 }
});
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
  height: 500px;
  overflow-y: auto;
  margin-bottom: 20px;
  padding-right: 4px;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease-out;
}

.message-content {
  background: var(--bg-card-hover);
  padding: 16px 20px;
  border-radius: 0 var(--radius-lg) var(--radius-lg) var(--radius-lg);
  max-width: 75%;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.bot-message .message-content {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(139, 92, 246, 0.08));
  border-color: rgba(99, 102, 241, 0.15);
}

.bot-message {
  flex-direction: row;
}

.user-message {
  flex-direction: row-reverse;
}

.user-message .message-content {
  border-radius: var(--radius-lg) 0 var(--radius-lg) var(--radius-lg);
  background: rgba(99, 102, 241, 0.18);
  border-color: rgba(99, 102, 241, 0.25);
}

/* 文字内容区域 */
.text-content {
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

/* 打字指示器 */
.typing-indicator {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 4px 0;
}

.typing-dot {
  width: 8px;
  height: 8px;
  background: var(--primary-color);
  border-radius: 50%;
  animation: typingBounce 1.4s infinite ease-in-out;
}

.typing-dot:nth-child(1) { animation-delay: 0s; }
.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes typingBounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 聊天输入框样式 */
.chat-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 4px 12px;
}

.upload-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  transition: background var(--transition-base);
}

.upload-btn:hover {
  background: rgba(99, 102, 241, 0.15);
}

.upload-icon {
  width: 18px;
  height: 18px;
  color: var(--primary-color);
}

.hidden-input {
  display: none;
}

.input-wrapper :deep(.el-input__wrapper) {
  background: transparent;
  border: none;
  box-shadow: none;
}

.input-wrapper :deep(.el-input__inner) {
  border: none;
  background: transparent;
}

/* 已上传图片预览 */
.uploaded-images-preview {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-item {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 20px;
  height: 20px;
  background: #ef4444;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  transition: background var(--transition-base);
}

.remove-image:hover {
  background: #dc2626;
}

/* 用户上传图片显示 */
.user-images-section {
  margin-top: 12px;
}

.user-images-section .images-grid {
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
}

.user-image {
  border: 1px solid rgba(99, 102, 241, 0.25);
}

.steps-info {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

/* ===== Section Titles ===== */
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-light);
}

.section-icon {
  width: 18px;
  height: 18px;
  color: var(--primary-color);
}

.section-title span {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 徽章样式 */
.satisfaction-badge, .count-badge, .duration-badge {
  margin-left: auto;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(99, 102, 241, 0.15);
  color: var(--primary-color);
  font-weight: 500;
}

/* ===== Script Section ===== */
.script-section {
  margin-top: 16px;
}

.scenes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scene-card {
  background: rgba(0, 0, 0, 0.1);
  border-radius: var(--radius-md);
  padding: 16px;
  border: 1px solid var(--border-light);
}

.scene-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.scene-number {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-color);
}

.scene-label {
  font-size: 12px;
  padding: 2px 8px;
  background: rgba(99, 102, 241, 0.15);
  border-radius: 6px;
  color: var(--primary-color);
}

.scene-description {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-secondary);
  margin: 0;
}

.scene-characters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.char-label {
  font-size: 12px;
  color: var(--text-muted);
}

.character-tag {
  font-size: 12px;
  padding: 2px 8px;
  background: rgba(16, 185, 129, 0.15);
  border-radius: 4px;
  color: #10b981;
}

/* ===== Optimization Section ===== */
.optimization-section {
  margin-top: 16px;
}

.optimization-card {
  background: rgba(0, 0, 0, 0.1);
  border-radius: var(--radius-md);
  padding: 16px;
}

.optimization-item {
  margin-bottom: 12px;
}

.optimization-item:last-child {
  margin-bottom: 0;
}

.optimization-item h4 {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  margin: 0 0 6px 0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.original-text {
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-secondary);
  margin: 0;
  padding: 10px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 6px;
}

.optimized-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-primary);
  margin: 0;
  padding: 12px;
  background: rgba(99, 102, 241, 0.15);
  border-radius: 6px;
  border-left: 3px solid var(--primary-color);
  font-family: 'SF Mono', 'Fira Code', monospace;
}

/* ===== Images Section ===== */
.images-section {
  margin-top: 16px;
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.image-card {
  background: rgba(0, 0, 0, 0.1);
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--border-light);
}

.image-wrapper {
  position: relative;
  aspect-ratio: 4/3;
}

.image-wrapper .el-image {
  width: 100%;
  height: 100%;
}

.image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  padding: 12px;
}

.image-index {
  font-size: 12px;
  color: white;
  font-weight: 500;
}

/* ===== Video Flow Section ===== */
.video-flow-section {
  margin-top: 16px;
}

.video-flow-card {
  background: rgba(0, 0, 0, 0.1);
  border-radius: var(--radius-md);
  padding: 16px;
}

.flow-info {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.info-item {
  display: flex;
  gap: 6px;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
}

.info-value {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
}

.segments-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.segment-item {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  padding: 12px;
}

.segment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.segment-number {
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-color);
}

.segment-duration {
  font-size: 12px;
  color: var(--text-muted);
  padding: 2px 8px;
  background: rgba(245, 158, 11, 0.15);
  border-radius: 4px;
}

.segment-desc {
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-secondary);
  margin: 0;
}

.effect-tag {
  font-size: 11px;
  padding: 2px 6px;
  background: rgba(139, 92, 246, 0.15);
  border-radius: 4px;
  color: #8b5cf6;
  margin-top: 6px;
  display: inline-block;
}

/* ===== Video Complete Section ===== */
.video-complete-section {
  margin-top: 16px;
}

.video-player-wrapper {
  background: rgba(0, 0, 0, 0.2);
  border-radius: var(--radius-md);
  overflow: hidden;
  margin-top: 12px;
}

.final-video {
  width: 100%;
  max-height: 400px;
  object-fit: contain;
}

.video-actions {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.btn-icon {
  width: 16px;
  height: 16px;
  margin-right: 6px;
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

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .content-area {
    padding: 16px;
  }

  .chat-section,
  .quick-stats {
    padding: 16px;
  }

  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }

  .message-content {
    max-width: 85%;
  }

  .images-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .nav-center {
    display: none;
  }

  .chat-container {
    height: 400px;
  }
}
</style>