<template>
  <div class="profile-container">
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
              <el-dropdown-item @click="$router.push('/settings')">设置</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-container>
        <el-aside width="220px" class="sidebar">
          <el-menu :default-active="activeTab" class="menu">
            <el-menu-item index="profile">
              <User class="icon" />
              <span>个人信息</span>
            </el-menu-item>
            <el-menu-item index="works">
              <FolderOpen class="icon" />
              <span>我的作品</span>
            </el-menu-item>
            <el-menu-item index="favorites">
              <Star class="icon" />
              <span>收藏夹</span>
            </el-menu-item>
            <el-menu-item index="statistics">
              <BarChart3 class="icon" />
              <span>数据统计</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="content-area">
          <div v-if="activeTab === 'profile'" class="profile-section">
            <div class="profile-header">
              <div class="avatar-wrapper">
                <el-avatar :size="120" :icon="User">
                  <img v-if="user.avatar" :src="user.avatar" />
                </el-avatar>
                <el-button type="primary" size="small" class="upload-btn">
                  <Upload class="icon" />
                  更换头像
                </el-button>
              </div>
              <div class="profile-info">
                <h2>{{ user.nickname }}</h2>
                <p class="email">{{ user.email }}</p>
                <div class="user-stats">
                  <div class="stat">
                    <span class="value">128</span>
                    <span class="label">作品</span>
                  </div>
                  <div class="stat">
                    <span class="value">52</span>
                    <span class="label">收藏</span>
                  </div>
                  <div class="stat">
                    <span class="value">3.2k</span>
                    <span class="label">浏览</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="edit-form">
              <h3>编辑资料</h3>
              <el-form :model="editForm" class="form">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="昵称">
                      <el-input v-model="editForm.nickname" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="邮箱">
                      <el-input v-model="editForm.email" disabled />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="个性签名">
                      <el-input v-model="editForm.signature" placeholder="分享你的创作理念..." />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="创作领域">
                      <el-select v-model="editForm.field" placeholder="选择领域">
                        <el-option label="动画漫画" value="anime" />
                        <el-option label="游戏设计" value="game" />
                        <el-option label="影视创作" value="film" />
                        <el-option label="插画艺术" value="illustration" />
                        <el-option label="其他" value="other" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item>
                  <el-button type="primary" @click="saveProfile">保存修改</el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>

          <div v-if="activeTab === 'works'" class="works-section">
            <div class="section-header">
              <h2>我的作品</h2>
              <el-button-group>
                <el-button :type="workType === 'all' ? 'primary' : 'default'" @click="workType = 'all'">全部</el-button>
                <el-button :type="workType === 'image' ? 'primary' : 'default'" @click="workType = 'image'">图片</el-button>
                <el-button :type="workType === 'video' ? 'primary' : 'default'" @click="workType = 'video'">视频</el-button>
                <el-button :type="workType === 'script' ? 'primary' : 'default'" @click="workType = 'script'">脚本</el-button>
              </el-button-group>
            </div>
            <div class="works-grid">
              <div v-for="work in works" :key="work.id" class="work-card">
                <div class="work-cover">
                  <img :src="work.cover" />
                  <div class="work-overlay">
                    <el-button size="small" icon="Edit">编辑</el-button>
                    <el-button size="small" icon="Download">下载</el-button>
                  </div>
                </div>
                <div class="work-info">
                  <h4>{{ work.title }}</h4>
                  <p>{{ work.date }}</p>
                  <div class="work-tags">
                    <span v-for="tag in work.tags" :key="tag" class="tag">{{ tag }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'favorites'" class="favorites-section">
            <h2>收藏夹</h2>
            <div class="favorites-grid">
              <div v-for="item in favorites" :key="item.id" class="favorite-card">
                <img :src="item.image" />
                <div class="favorite-info">
                  <h4>{{ item.name }}</h4>
                  <p>{{ item.category }}</p>
                  <el-button size="small" icon="Star" @click="removeFavorite(item.id)">取消收藏</el-button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'statistics'" class="stats-section">
            <h2>数据统计</h2>
            <div class="stats-cards">
              <div class="stat-card">
                <div class="stat-icon blue">
                  <ImageIcon class="icon" />
                </div>
                <div class="stat-content">
                  <span class="stat-value">128</span>
                  <span class="stat-label">生成图片</span>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon orange">
                  <Video class="icon" />
                </div>
                <div class="stat-content">
                  <span class="stat-value">24</span>
                  <span class="stat-label">生成视频</span>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon purple">
                  <FileText class="icon" />
                </div>
                <div class="stat-content">
                  <span class="stat-value">16</span>
                  <span class="stat-label">创作脚本</span>
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-icon green">
                  <Clock class="icon" />
                </div>
                <div class="stat-content">
                  <span class="stat-value">12h</span>
                  <span class="stat-label">创作时长</span>
                </div>
              </div>
            </div>
            <div class="chart-section">
              <h3>近7天创作趋势</h3>
              <div class="chart-container">
                <div class="chart-bars">
                  <div v-for="(day, index) in chartData" :key="index" class="bar-item">
                    <div class="bar" :style="{ height: day.value + '%' }"></div>
                    <span class="day-label">{{ day.label }}</span>
                  </div>
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
import { ref, onMounted } from 'vue'
import { 
  PlayCircle, User, FolderOpen, Star, BarChart3,
  Upload, Image as ImageIcon, Video, FileText, Clock, ArrowLeft
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

const activeTab = ref('profile')
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

const editForm = ref({
  nickname: '',
  email: '',
  signature: '',
  field: ''
})

const workType = ref('all')

const works = ref([
  { id: 1, title: '星空下的少女', cover: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=anime%20girl%20under%20starry%20sky&image_size=square', date: '2024-01-15', tags: ['动漫', '星空'] },
  { id: 2, title: '未来都市', cover: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=futuristic%20city%20anime%20style&image_size=square', date: '2024-01-14', tags: ['科幻', '都市'] },
  { id: 3, title: '古风庭院', cover: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=ancient%20chinese%20garden%20anime&image_size=square', date: '2024-01-13', tags: ['古风', '场景'] },
  { id: 4, title: '校园青春', cover: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=anime%20school%20life%20romantic&image_size=square', date: '2024-01-12', tags: ['校园', '青春'] },
])

const favorites = ref([
  { id: 1, name: '魔法少女素材包', category: '角色素材', image: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=magical%20girl%20character%20design&image_size=square' },
  { id: 2, name: '日系场景合集', category: '背景素材', image: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=japanese%20anime%20background&image_size=square' },
])

const chartData = ref([
  { label: '周一', value: 60 },
  { label: '周二', value: 80 },
  { label: '周三', value: 45 },
  { label: '周四', value: 90 },
  { label: '周五', value: 70 },
  { label: '周六', value: 95 },
  { label: '周日', value: 85 },
])

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

const saveProfile = () => {
  ElMessage.success('资料保存成功')
}

const removeFavorite = (id) => {
  favorites.value = favorites.value.filter(f => f.id !== id)
  ElMessage.success('已取消收藏')
}

onMounted(() => {
  editForm.value = {
    nickname: user.value.nickname || '',
    email: user.value.email || '',
    signature: '',
    field: ''
  }
})
</script>

<style scoped>
.profile-container {
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

.profile-section {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.profile-header {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}

.avatar-wrapper {
  position: relative;
}

.upload-btn {
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
}

.profile-info {
  flex: 1;
}

.profile-info h2 {
  font-size: 28px;
  margin-bottom: 8px;
  font-family: var(--font-heading);
}

.email {
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.user-stats {
  display: flex;
  gap: 40px;
}

.stat {
  text-align: center;
}

.stat .value {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: var(--primary-color);
  font-family: var(--font-heading);
}

.stat .label {
  font-size: 14px;
  color: var(--text-secondary);
}

.edit-form h3 {
  margin-bottom: 20px;
  font-family: var(--font-heading);
}

.form {
  background: var(--bg-card-hover);
  padding: 24px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  font-family: var(--font-heading);
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.work-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all var(--transition-slow);
}

.work-card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  transform: translateY(-4px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.work-cover {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.work-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.work-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0,0,0,0.8));
  padding: 12px;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity var(--transition-base);
}

.work-card:hover .work-overlay {
  opacity: 1;
}

.work-info {
  padding: 16px;
}

.work-info h4 {
  margin-bottom: 8px;
  font-family: var(--font-heading);
}

.work-info p {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 12px;
}

.work-tags {
  display: flex;
  gap: 8px;
}

.tag {
  background: var(--bg-elevated);
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  color: var(--text-secondary);
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.favorite-card {
  display: flex;
  gap: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  padding: 16px;
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
}

.favorite-card:hover {
  border-color: rgba(99, 102, 241, 0.2);
}

.favorite-card img {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-sm);
  object-fit: cover;
}

.favorite-info {
  flex: 1;
}

.favorite-info h4 {
  font-family: var(--font-heading);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  display: flex;
  gap: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  padding: 20px;
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
}

.stat-card:hover {
  border-color: rgba(99, 102, 241, 0.2);
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.stat-icon.orange {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.stat-icon.purple {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
}

.stat-icon.green {
  background: linear-gradient(135deg, #10b981, #059669);
}

.stat-icon .icon {
  width: 28px;
  height: 28px;
  color: white;
}

.stat-content .stat-value {
  display: block;
  font-size: 28px;
  font-weight: bold;
  font-family: var(--font-heading);
}

.stat-content .stat-label {
  color: var(--text-secondary);
}

.chart-section {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  padding: 24px;
  border-radius: var(--radius-md);
}

.chart-section h3 {
  font-family: var(--font-heading);
}

.chart-container {
  height: 200px;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
}

.chart-bars {
  display: flex;
  gap: 20px;
  width: 100%;
  justify-content: center;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.bar {
  width: 40px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 8px 8px 0 0;
  transition: height var(--transition-slow);
  min-height: 4px;
}

.day-label {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
