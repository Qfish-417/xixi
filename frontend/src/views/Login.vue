<template>
  <div class="login-container">
    <div class="bg-animation">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>
    
    <div class="login-card">
      <div class="card-header">
        <div class="logo">
          <div class="logo-icon-wrapper">
            <PlayCircle class="logo-icon" />
          </div>
          <div class="logo-text">
            <h1>AI漫剧创作平台</h1>
            <p>让AI为您的创意插上翅膀</p>
          </div>
        </div>
      </div>
      
      <div class="card-body">
        <div class="tabs">
          <span 
            :class="['tab', activeTab === 'login' ? 'active' : '']" 
            @click="activeTab = 'login'"
          >
            登录
          </span>
          <span 
            :class="['tab', activeTab === 'register' ? 'active' : '']" 
            @click="activeTab = 'register'"
          >
            注册
          </span>
        </div>
        
        <el-form v-if="activeTab === 'login'" :model="loginForm" class="form">
          <el-form-item class="form-item">
            <el-input 
              v-model="loginForm.email" 
              type="email" 
              placeholder="邮箱地址"
              class="input-field"
              :prefix-icon="UserIcon"
            />
          </el-form-item>
          
          <el-form-item class="form-item">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="密码"
              class="input-field"
              :prefix-icon="LockIcon"
              show-password
            />
          </el-form-item>
          
          <el-form-item class="form-item actions">
            <el-checkbox v-model="loginForm.rememberMe" class="remember-me">记住我</el-checkbox>
            <span class="forgot-password" @click="showForgot = true">忘记密码?</span>
          </el-form-item>
          
          <el-button type="primary" class="submit-btn" :loading="loginLoading" @click="handleLogin">
            登录
          </el-button>
        </el-form>
        
        <el-form v-if="activeTab === 'register'" :model="registerForm" class="form">
          <el-form-item class="form-item">
            <el-input 
              v-model="registerForm.nickname" 
              type="text" 
              placeholder="昵称"
              class="input-field"
              :prefix-icon="UserIcon"
            />
          </el-form-item>
          
          <el-form-item class="form-item">
            <el-input 
              v-model="registerForm.email" 
              type="email" 
              placeholder="邮箱地址"
              class="input-field"
              :prefix-icon="MailIcon"
            />
          </el-form-item>
          
          <el-form-item class="form-item">
            <el-input 
              v-model="registerForm.password" 
              type="password" 
              placeholder="密码"
              class="input-field"
              :prefix-icon="LockIcon"
              show-password
            />
          </el-form-item>
          
          <el-form-item class="form-item">
            <el-input 
              v-model="registerForm.confirmPassword" 
              type="password" 
              placeholder="确认密码"
              class="input-field"
              :prefix-icon="LockIcon"
              show-password
            />
          </el-form-item>
          
          <el-form-item class="form-item actions">
            <el-checkbox v-model="registerForm.agreed" class="remember-me">
              我已阅读并同意<a href="#" class="link">用户协议</a>和<a href="#" class="link">隐私政策</a>
            </el-checkbox>
          </el-form-item>
          
          <el-button type="primary" class="submit-btn" :loading="registerLoading" @click="handleRegister">
            注册
          </el-button>
        </el-form>
        
        <div class="social-login">
          <div class="divider">
            <span></span>
            <span class="divider-text">或</span>
            <span></span>
          </div>
          <div class="social-buttons">
            <el-button class="social-btn wechat" icon="Message">微信登录</el-button>
            <el-button class="social-btn github" icon="Globe">GitHub</el-button>
          </div>
        </div>
      </div>
    </div>
    
    <el-dialog title="忘记密码" v-model="showForgot" width="420px" class="forgot-dialog">
      <div class="forgot-content">
        <div class="forgot-icon">
          <Key class="icon" />
        </div>
        <p class="forgot-desc">请输入您注册时使用的邮箱，我们将发送重置链接到您的邮箱</p>
        <el-form :model="forgotForm" class="forgot-form">
          <el-form-item>
            <el-input 
              v-model="forgotForm.email" 
              type="email" 
              placeholder="请输入邮箱地址"
              class="input-field"
              :prefix-icon="MailIcon"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showForgot = false">取消</el-button>
        <el-button type="primary" @click="sendResetEmail">发送重置链接</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { PlayCircle, User, Lock, Mail, Key } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { authAPI } from '../api/auth.js'

const UserIcon = User
const LockIcon = Lock
const MailIcon = Mail

const activeTab = ref('login')
const showForgot = ref(false)

const loginForm = ref({
  email: '',
  password: '',
  rememberMe: false
})

const registerForm = ref({
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreed: false
})

const forgotForm = ref({
  email: ''
})

const loginLoading = ref(false)
const registerLoading = ref(false)

const handleLogin = async () => {
  if (!loginForm.value.email || !loginForm.value.password) {
    ElMessage.error('请填写完整信息')
    return
  }

  loginLoading.value = true
  try {
    const response = await authAPI.login({
      email: loginForm.value.email,
      password: loginForm.value.password
    })

    const { token, user } = response.data
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))

    ElMessage.success('登录成功')
    setTimeout(() => {
      window.location.href = '/'
    }, 1500)
  } catch (error) {
    const errorMsg = error.response?.data?.error || '登录失败，请检查邮箱和密码'
    ElMessage.error(errorMsg)
  } finally {
    loginLoading.value = false
  }
}

const handleRegister = async () => {
  if (!registerForm.value.nickname || !registerForm.value.email || !registerForm.value.password) {
    ElMessage.error('请填写完整信息')
    return
  }

  if (registerForm.value.password.length < 6) {
    ElMessage.error('密码至少需要6位')
    return
  }

  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  if (!registerForm.value.agreed) {
    ElMessage.error('请同意用户协议和隐私政策')
    return
  }

  registerLoading.value = true
  try {
    const response = await authAPI.register({
      nickname: registerForm.value.nickname,
      email: registerForm.value.email,
      password: registerForm.value.password,
      confirmPassword: registerForm.value.confirmPassword
    })

    const { token, user } = response.data
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))

    ElMessage.success('注册成功')
    setTimeout(() => {
      window.location.href = '/'
    }, 1500)
  } catch (error) {
    const errorMsg = error.response?.data?.error || '注册失败，请稍后重试'
    ElMessage.error(errorMsg)
  } finally {
    registerLoading.value = false
  }
}

const sendResetEmail = () => {
  if (!forgotForm.value.email) {
    ElMessage.error('请输入邮箱')
    return
  }
  ElMessage.success('重置链接已发送')
  showForgot.value = false
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0f23 0%, #1a1a2e 50%, #16213e 100%);
  position: relative;
  overflow: hidden;
}

.bg-animation {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}

.bg-circle-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  top: -100px;
  right: -100px;
  animation: float1 6s ease-in-out infinite;
}

.bg-circle-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #ec4899, #f472b6);
  bottom: -50px;
  left: -50px;
  animation: float2 8s ease-in-out infinite;
}

.bg-circle-3 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, #10b981, #06b6d4);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float3 10s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-30px, 30px); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, -30px); }
}

@keyframes float3 {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.2); }
}

.login-card {
  background: rgba(26, 26, 46, 0.75);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 24px;
  padding: 40px;
  width: 100%;
  max-width: 460px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.4),
              0 0 0 1px rgba(255, 255, 255, 0.05);
  position: relative;
  z-index: 10;
  animation: fadeInUp 0.5s ease-out;
}

.card-header {
  margin-bottom: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-icon-wrapper {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.4);
}

.logo-icon {
  width: 28px;
  height: 28px;
  color: white;
}

.logo-text h1 {
  font-size: 22px;
  font-weight: 700;
  color: white;
  margin: 0 0 4px 0;
  font-family: var(--font-heading);
}

.logo-text p {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
}

.tabs {
  display: flex;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 28px;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 10px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: var(--font-heading);
}

.tab:hover {
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.03);
}

.tab.active {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.4);
}

.form {
  margin-bottom: 24px;
}

.form-item {
  margin-bottom: 20px;
}

.input-field {
  width: 100%;
  height: 48px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: white;
  font-size: 15px;
}

.input-field:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.remember-me {
  color: #9ca3af;
  font-size: 14px;
}

.forgot-password {
  color: #6366f1;
  font-size: 14px;
  cursor: pointer;
}

.forgot-password:hover {
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  height: 52px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  box-shadow: 0 8px 25px rgba(99, 102, 241, 0.4);
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(99, 102, 241, 0.5);
}

.submit-btn:active {
  transform: translateY(0);
}

.social-login {
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.divider span:first-child,
.divider span:last-child {
  flex: 1;
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
}

.divider-text {
  color: #9ca3af;
  font-size: 14px;
}

.social-buttons {
  display: flex;
  gap: 12px;
}

.social-btn {
  flex: 1;
  height: 44px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  color: white;
  transition: all 0.3s ease;
}

.social-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.social-btn.wechat:hover {
  border-color: #07c160;
  color: #07c160;
}

.social-btn.github:hover {
  border-color: #333;
  color: #333;
}

.link {
  color: #6366f1;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}

.forgot-dialog {
  border-radius: 16px;
}

.forgot-content {
  text-align: center;
}

.forgot-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.forgot-icon .icon {
  width: 28px;
  height: 28px;
  color: white;
}

.forgot-desc {
  color: #9ca3af;
  font-size: 14px;
  margin-bottom: 20px;
}

.forgot-form {
  margin-bottom: 0;
}
</style>
