<template>
  <div class="register-container">
    <div class="bg-animation">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <div class="register-card">
      <div class="card-header">
        <div class="logo">
          <div class="logo-icon-wrapper">
            <PlayCircle class="logo-icon" />
          </div>
          <div class="logo-text">
            <h1>AI漫剧创作平台</h1>
            <p>开启您的创意之旅</p>
          </div>
        </div>
      </div>

      <el-form :model="form" ref="formRef" class="register-form">
        <el-form-item>
          <el-input
            v-model="form.nickname"
            placeholder="昵称"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.email"
            type="email"
            placeholder="邮箱地址"
            :prefix-icon="Mail"
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码（至少6位）"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="form.agreed" class="agreement">
            我已阅读并同意<a href="#" class="link">用户协议</a>和<a href="#" class="link">隐私政策</a>
          </el-checkbox>
        </el-form-item>

        <el-button type="primary" class="register-btn" :loading="loading" @click="handleRegister">
          注册
        </el-button>
      </el-form>

      <div class="login-link">
        <span>已有账号？</span>
        <a @click="$router.push('/login')">立即登录</a>
      </div>

      <div class="social-login">
        <div class="divider">
          <span></span>
          <span class="divider-text">或</span>
          <span></span>
        </div>
        <div class="social-buttons">
          <el-button class="social-btn wechat">微信登录</el-button>
          <el-button class="social-btn github">GitHub</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { PlayCircle, User, Mail, Lock } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { authAPI } from '../api/auth.js'

const form = ref({
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreed: false
})

const loading = ref(false)

const handleRegister = async () => {
  if (!form.value.nickname || !form.value.email || !form.value.password) {
    ElMessage.error('请填写完整信息')
    return
  }

  if (form.value.password.length < 6) {
    ElMessage.error('密码至少需要6位')
    return
  }

  if (form.value.password !== form.value.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  if (!form.value.agreed) {
    ElMessage.error('请同意用户协议和隐私政策')
    return
  }

  loading.value = true
  try {
    const response = await authAPI.register({
      nickname: form.value.nickname,
      email: form.value.email,
      password: form.value.password,
      confirmPassword: form.value.confirmPassword
    })

    const { token, user } = response.data
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))

    ElMessage.success('注册成功，即将跳转')
    setTimeout(() => {
      window.location.href = '/'
    }, 1500)
  } catch (error) {
    const errorMsg = error.response?.data?.error || '注册失败，请稍后重试'
    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0f23 0%, #1a1a2e 50%, #16213e 100%);
  position: relative;
  overflow: hidden;
  padding: 20px;
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
  left: -100px;
  animation: float1 6s ease-in-out infinite;
}

.bg-circle-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #ec4899, #f472b6);
  bottom: -50px;
  right: -50px;
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

.register-card {
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

.register-form {
  margin-bottom: 24px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.register-btn {
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
  margin-top: 8px;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(99, 102, 241, 0.5);
}

.register-btn:active {
  transform: translateY(0);
}

.agreement {
  color: #9ca3af;
  font-size: 13px;
}

.link {
  color: #6366f1;
  text-decoration: none;
  margin: 0 2px;
}

.link:hover {
  text-decoration: underline;
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: #9ca3af;
  margin-bottom: 24px;
}

.login-link a {
  color: #6366f1;
  margin-left: 4px;
  cursor: pointer;
}

.login-link a:hover {
  text-decoration: underline;
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
  color: #fff;
}
</style>
