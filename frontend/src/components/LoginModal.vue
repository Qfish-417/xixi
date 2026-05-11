<template>
  <el-dialog
    v-model="visible"
    title=""
    :show-header="false"
    width="420px"
    class="login-modal"
    @close="handleClose"
  >
    <div class="modal-content">
      <div class="modal-header">
        <div class="logo">
          <div class="logo-icon-wrapper">
            <PlayCircle class="logo-icon" />
          </div>
          <div class="logo-text">
            <h2>欢迎回来</h2>
            <p>登录后即可使用全部功能</p>
          </div>
        </div>
      </div>
      
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
  </el-dialog>
  
  <el-dialog title="忘记密码" v-model="showForgot" width="380px" class="forgot-dialog">
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
</template>

<script setup>
import { ref } from 'vue'
import { PlayCircle, User, Lock, Mail, Key } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { authAPI } from '../api/auth.js'

const emit = defineEmits(['login-success'])

const visible = ref(false)
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

const UserIcon = User
const LockIcon = Lock
const MailIcon = Mail

const open = () => {
  visible.value = true
}

const handleClose = () => {
  visible.value = false
}

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
    visible.value = false
    emit('login-success', user)
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
    visible.value = false
    emit('login-success', user)
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

defineExpose({ open })
</script>

<style scoped>
.login-modal {
  border-radius: 20px;
  overflow: hidden;
}

.login-modal :deep(.el-dialog__body) {
  padding: 0;
}

.modal-content {
  padding: 32px;
}

.modal-header {
  margin-bottom: 28px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo-icon-wrapper {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 24px rgba(99, 102, 241, 0.35);
}

.logo-icon {
  width: 24px;
  height: 24px;
  color: white;
}

.logo-text h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px 0;
  font-family: var(--font-heading);
}

.logo-text p {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.tabs {
  display: flex;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  padding: 3px;
  margin-bottom: 24px;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 8px;
  border-radius: 7px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab.active {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}

.form {
  margin-bottom: 20px;
}

.form-item {
  margin-bottom: 16px;
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.remember-me {
  color: var(--text-secondary);
  font-size: 13px;
}

.forgot-password {
  color: var(--primary-color);
  font-size: 13px;
  cursor: pointer;
}

.forgot-password:hover {
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  color: white;
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.35);
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(99, 102, 241, 0.45);
}

.social-login {
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.divider span:first-child,
.divider span:last-child {
  flex: 1;
  height: 1px;
  background: var(--border-color);
}

.divider-text {
  color: var(--text-secondary);
  font-size: 13px;
}

.social-buttons {
  display: flex;
  gap: 10px;
}

.social-btn {
  flex: 1;
  height: 40px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-secondary);
  transition: all 0.3s ease;
}

.social-btn:hover {
  background: rgba(255, 255, 255, 0.08);
}

.social-btn.wechat:hover {
  border-color: #07c160;
  color: #07c160;
}

.social-btn.github:hover {
  border-color: #fff;
  color: #fff;
}

.link {
  color: var(--primary-color);
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
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
}

.forgot-icon .icon {
  width: 24px;
  height: 24px;
  color: white;
}

.forgot-desc {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 18px;
}

.forgot-form {
  margin-bottom: 0;
}
</style>
