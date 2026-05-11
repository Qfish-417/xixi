<template>
  <div class="groups-container">
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
            <el-menu-item index="myGroups">
              <Users class="icon" />
              <span>我的组别</span>
            </el-menu-item>
            <el-menu-item index="createGroup">
              <Plus class="icon" />
              <span>创建组别</span>
            </el-menu-item>
            <el-menu-item index="joinRequests">
              <Bell class="icon" />
              <el-badge :value="pendingRequests" class="badge" />
              <span>加入申请</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="content-area">
          <div v-if="activeTab === 'myGroups'" class="my-groups-section">
            <div class="section-header">
              <h2>我的组别</h2>
              <el-button type="primary" icon="Plus" @click="activeTab = 'createGroup'">创建新组别</el-button>
            </div>

            <div class="groups-grid">
              <div v-for="group in myGroups" :key="group.id" class="group-card">
                <div class="group-header">
                  <div class="group-icon">
                    <Users class="icon" />
                  </div>
                  <div class="group-info">
                    <h3>{{ group.name }}</h3>
                    <p>{{ group.members.length }} 名成员</p>
                  </div>
                  <div class="group-role" :class="group.role">
                    {{ group.role === 'admin' ? '管理员' : '成员' }}
                  </div>
                </div>
                
                <p class="group-desc">{{ group.description }}</p>
                
                <div class="group-members">
                  <div class="members-avatars">
                    <el-avatar 
                      v-for="member in group.members.slice(0, 4)" 
                      :key="member.id" 
                      :icon="User"
                    />
                    <span v-if="group.members.length > 4" class="more-members">
                      +{{ group.members.length - 4 }}
                    </span>
                  </div>
                </div>
                
                <div class="group-stats">
                  <div class="stat">
                    <span class="value">{{ group.materialCount }}</span>
                    <span class="label">素材</span>
                  </div>
                  <div class="stat">
                    <span class="value">{{ group.workCount }}</span>
                    <span class="label">作品</span>
                  </div>
                </div>
                
                <div class="group-actions">
                  <el-button size="small" @click="openGroupDetail(group)">详情</el-button>
                  <el-button size="small" @click="openGroupLibrary(group.id)">素材库</el-button>
                  <el-button size="small" v-if="group.role === 'admin'" @click="manageGroup(group)">管理</el-button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'createGroup'" class="create-group-section">
            <h2>创建新组别</h2>
            <el-form :model="createForm" class="create-form">
              <el-form-item label="组别名称" required>
                <el-input v-model="createForm.name" placeholder="请输入组别名称" />
              </el-form-item>
              <el-form-item label="组别描述">
                <el-input type="textarea" v-model="createForm.description" :rows="3" placeholder="请输入组别描述" />
              </el-form-item>
              <el-form-item label="组别图标">
                <div class="icon-options">
                  <div 
                    v-for="icon in iconOptions" 
                    :key="icon"
                    :class="['icon-option', { active: createForm.icon === icon }]"
                    @click="createForm.icon = icon"
                  >
                    <component :is="getIconComponent(icon)" class="icon" />
                  </div>
                </div>
              </el-form-item>
              <el-form-item label="成员邀请">
                <el-select v-model="createForm.members" multiple placeholder="选择要邀请的成员">
                  <el-option v-for="user in availableUsers" :key="user.id" :label="user.nickname" :value="user.id" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="createGroup">创建组别</el-button>
                <el-button @click="activeTab = 'myGroups'">取消</el-button>
              </el-form-item>
            </el-form>
          </div>

          <div v-if="activeTab === 'joinRequests'" class="requests-section">
            <h2>加入申请</h2>
            <div v-if="requests.length > 0" class="requests-list">
              <div v-for="request in requests" :key="request.id" class="request-item">
                <div class="request-info">
                  <el-avatar :icon="User" />
                  <div class="request-detail">
                    <span class="request-name">{{ request.userNickname }}</span>
                    <span class="request-group">申请加入 "{{ request.groupName }}"</span>
                  </div>
                </div>
                <div class="request-actions">
                  <el-button size="small" type="primary" @click="approveRequest(request.id)">通过</el-button>
                  <el-button size="small" @click="rejectRequest(request.id)">拒绝</el-button>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <Bell class="empty-icon" />
              <p>暂无加入申请</p>
            </div>
          </div>
        </el-main>
      </el-container>
    </el-container>

    <el-dialog title="组别详情" v-model="showDetail" width="600px">
      <div v-if="selectedGroup" class="group-detail">
        <div class="detail-header">
          <div class="group-icon-large">
            <Users class="icon" />
          </div>
          <div class="detail-title">
            <h3>{{ selectedGroup.name }}</h3>
            <span class="group-role-badge">{{ selectedGroup.role === 'admin' ? '管理员' : '成员' }}</span>
          </div>
        </div>
        <p>{{ selectedGroup.description }}</p>
        
        <h4>成员列表</h4>
        <div class="members-list">
          <div v-for="member in selectedGroup.members" :key="member.id" class="member-item">
            <el-avatar :icon="User" />
            <span>{{ member.nickname }}</span>
            <span v-if="member.role === 'admin'" class="member-role">管理员</span>
          </div>
        </div>
        
        <h4>共享素材</h4>
        <div class="shared-materials">
          <div v-for="material in selectedGroup.sharedMaterials" :key="material.id" class="material-item">
            <img :src="material.image" />
            <span>{{ material.name }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { 
  PlayCircle, User, Users, Plus, Bell,
  Building2, Rocket, Palette, Gamepad2, ArrowLeft
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const activeTab = ref('myGroups')
const showDetail = ref(false)
const selectedGroup = ref(null)
const pendingRequests = ref(3)

const iconOptions = ['building', 'rocket', 'palette', 'gamepad']

const createForm = ref({
  name: '',
  description: '',
  icon: 'building',
  members: []
})

const availableUsers = ref([
  { id: 'u1', nickname: '创作者A' },
  { id: 'u2', nickname: '设计师B' },
  { id: 'u3', nickname: '编剧C' },
  { id: 'u4', nickname: '导演D' }
])

const myGroups = ref([
  {
    id: 'g1',
    name: '动漫创作小组',
    description: '专注于动漫风格创作的团队，共享素材和创作经验',
    icon: 'palette',
    role: 'admin',
    members: [
      { id: 'u0', nickname: '我', role: 'admin' },
      { id: 'u1', nickname: '创作者A', role: 'member' },
      { id: 'u2', nickname: '设计师B', role: 'member' },
      { id: 'u3', nickname: '编剧C', role: 'member' },
      { id: 'u4', nickname: '导演D', role: 'member' }
    ],
    materialCount: 128,
    workCount: 24,
    sharedMaterials: [
      { id: 'm1', name: '角色素材包', image: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=anime%20character%20pack&image_size=square' },
      { id: 'm2', name: '背景合集', image: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=anime%20background%20collection&image_size=square' }
    ]
  },
  {
    id: 'g2',
    name: '游戏开发团队',
    description: '游戏美术资源开发和共享',
    icon: 'gamepad',
    role: 'member',
    members: [
      { id: 'u0', nickname: '我', role: 'member' },
      { id: 'u1', nickname: '创作者A', role: 'admin' }
    ],
    materialCount: 56,
    workCount: 12,
    sharedMaterials: []
  },
  {
    id: 'g3',
    name: '独立创作者联盟',
    description: '独立创作者的交流和资源共享平台',
    icon: 'rocket',
    role: 'member',
    members: [
      { id: 'u0', nickname: '我', role: 'member' },
      { id: 'u2', nickname: '设计师B', role: 'admin' },
      { id: 'u3', nickname: '编剧C', role: 'member' }
    ],
    materialCount: 89,
    workCount: 18,
    sharedMaterials: []
  }
])

const requests = ref([
  { id: 'r1', userNickname: '新用户E', groupName: '动漫创作小组', groupId: 'g1' },
  { id: 'r2', userNickname: '新人F', groupName: '动漫创作小组', groupId: 'g1' },
  { id: 'r3', userNickname: '创作者G', groupName: '游戏开发团队', groupId: 'g2' }
])

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

const handleTabSelect = (tab) => {
  activeTab.value = tab
}

const getIconComponent = (icon) => {
  const icons = {
    building: Building2,
    rocket: Rocket,
    palette: Palette,
    gamepad: Gamepad2
  }
  return icons[icon] || Building2
}

const createGroup = () => {
  if (!createForm.value.name) {
    ElMessage.error('请输入组别名称')
    return
  }
  ElMessage.success('组别创建成功')
  activeTab.value = 'myGroups'
}

const openGroupDetail = (group) => {
  selectedGroup.value = group
  showDetail.value = true
}

const openGroupLibrary = (groupId) => {
  ElMessage.info(`正在打开组别 ${groupId} 的素材库`)
}

const manageGroup = (group) => {
  ElMessage.info(`正在管理组别 ${group.name}`)
}

const approveRequest = (id) => {
  requests.value = requests.value.filter(r => r.id !== id)
  pendingRequests.value--
  ElMessage.success('申请已通过')
}

const rejectRequest = (id) => {
  requests.value = requests.value.filter(r => r.id !== id)
  pendingRequests.value--
  ElMessage.info('申请已拒绝')
}
</script>

<style scoped>
.groups-container {
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

.badge {
  margin-left: -8px;
}

.content-area {
  padding: 24px;
}

.content-area h2 {
  font-family: var(--font-heading);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.groups-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 20px;
}

.group-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 20px;
  transition: all var(--transition-slow);
}

.group-card:hover {
  border-color: rgba(99, 102, 241, 0.2);
  box-shadow: var(--shadow-glow);
}

.group-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}

.group-icon {
  width: 48px;
  height: 48px;
  background: var(--gradient-primary);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.group-icon .icon {
  width: 24px;
  height: 24px;
  color: white;
}

.group-info h3 {
  margin-bottom: 2px;
  font-family: var(--font-heading);
}

.group-info p {
  color: var(--text-secondary);
  font-size: 13px;
}

.group-role {
  margin-left: auto;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.group-role.admin {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: white;
}

.group-role.member {
  background: var(--bg-elevated);
  color: var(--text-secondary);
}

.group-desc {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 16px;
  line-height: 1.5;
}

.group-members {
  margin-bottom: 16px;
}

.members-avatars {
  display: flex;
  align-items: center;
  gap: 8px;
}

.more-members {
  background: var(--bg-elevated);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-secondary);
}

.group-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.stat .value {
  display: block;
  font-size: 20px;
  font-weight: bold;
  color: var(--primary-color);
  font-family: var(--font-heading);
}

.stat .label {
  font-size: 13px;
  color: var(--text-secondary);
}

.group-actions {
  display: flex;
  gap: 8px;
}

.create-form {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  padding: 24px;
  border-radius: var(--radius-lg);
  max-width: 600px;
}

.icon-options {
  display: flex;
  gap: 12px;
}

.icon-option {
  width: 56px;
  height: 56px;
  background: var(--bg-card-hover);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid transparent;
}

.icon-option:hover {
  border-color: var(--primary-color);
}

.icon-option.active {
  background: var(--primary-color);
  border-color: var(--primary-color);
}

.icon-option .icon {
  width: 28px;
  height: 28px;
  color: var(--text-secondary);
}

.icon-option.active .icon {
  color: white;
}

.requests-list {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.request-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.request-item:last-child {
  border-bottom: none;
}

.request-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.request-name {
  display: block;
  font-weight: bold;
  font-family: var(--font-heading);
}

.request-group {
  color: var(--text-secondary);
  font-size: 13px;
}

.empty-state {
  text-align: center;
  padding: 60px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
}

.empty-icon {
  width: 80px;
  height: 80px;
  color: var(--bg-elevated);
  margin-bottom: 20px;
}

.group-detail {
  padding: 10px;
}

.detail-header {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.group-icon-large {
  width: 70px;
  height: 70px;
  background: var(--gradient-primary);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.group-icon-large .icon {
  width: 36px;
  height: 36px;
  color: white;
}

.detail-title h3 {
  font-family: var(--font-heading);
}

.group-role-badge {
  background: var(--bg-elevated);
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  margin-left: 8px;
  color: var(--text-secondary);
}

.members-list {
  margin-bottom: 20px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
}

.member-role {
  margin-left: auto;
  background: #f59e0b;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 12px;
  color: white;
}

.shared-materials {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.material-item {
  width: 100px;
  text-align: center;
}

.material-item img {
  width: 100%;
  height: 80px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  margin-bottom: 6px;
}

.material-item span {
  color: var(--text-secondary);
  font-size: 12px;
}
</style>
