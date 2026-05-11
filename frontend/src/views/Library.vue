<template>
  <div class="library-container">
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
          <div class="search-box">
            <el-input 
              v-model="searchKeyword" 
              placeholder="搜索素材..." 
              prefix-icon="Search"
              @keyup.enter="searchMaterials"
            />
          </div>
          
          <div class="category-list">
            <h3>分类</h3>
            <el-menu :default-active="selectedCategory" class="category-menu" @select="selectCategory">
              <el-menu-item index="all">
                <FolderOpen class="icon" />
                <span>全部素材</span>
              </el-menu-item>
              <el-menu-item index="character">
                <User class="icon" />
                <span>角色素材</span>
              </el-menu-item>
              <el-menu-item index="background">
                <ImageIcon class="icon" />
                <span>背景素材</span>
              </el-menu-item>
              <el-menu-item index="prop">
                <Package class="icon" />
                <span>道具素材</span>
              </el-menu-item>
              <el-menu-item index="effect">
                <Sparkles class="icon" />
                <span>特效素材</span>
              </el-menu-item>
              <el-menu-item index="script">
                <FileText class="icon" />
                <span>脚本素材</span>
              </el-menu-item>
            </el-menu>
          </div>
          
          <div class="group-filter">
            <h3>组别</h3>
            <el-select v-model="selectedGroup" placeholder="选择组别" @change="filterByGroup">
              <el-option label="全部组别" value="" />
              <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
            </el-select>
          </div>
        </el-aside>

        <el-main class="content-area">
          <div class="content-header">
            <h2>素材库</h2>
            <div class="header-actions">
              <el-button-group>
                <el-button :type="viewMode === 'grid' ? 'primary' : 'default'" @click="viewMode = 'grid'" icon="LayoutGrid">网格</el-button>
                <el-button :type="viewMode === 'list' ? 'primary' : 'default'" @click="viewMode = 'list'" icon="List">列表</el-button>
              </el-button-group>
              <el-button type="primary" icon="Upload" @click="uploadMaterial">上传素材</el-button>
            </div>
          </div>

          <div class="tags-bar">
            <span 
              v-for="tag in popularTags" 
              :key="tag"
              :class="['tag', { active: selectedTags.includes(tag) }]"
              @click="toggleTag(tag)"
            >{{ tag }}</span>
          </div>

          <div :class="['materials-container', viewMode]">
            <div v-for="material in filteredMaterials" :key="material.id" class="material-card" @click="selectMaterial(material)">
              <div class="material-cover">
                <img :src="material.imageUrl" />
                <div class="material-overlay">
                  <el-button size="small" icon="Download">下载</el-button>
                  <el-button size="small" icon="Star" :class="{ starred: material.starred }">收藏</el-button>
                  <el-button size="small" icon="Copy">复制</el-button>
                </div>
              </div>
              <div class="material-info">
                <h4>{{ material.name }}</h4>
                <p class="description">{{ material.description }}</p>
                <div class="material-meta">
                  <span class="category-tag">{{ getCategoryLabel(material.category) }}</span>
                  <span class="usage-count">使用 {{ material.usageCount }} 次</span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="filteredMaterials.length === 0" class="empty-state">
            <ImageIcon class="empty-icon" />
            <p>暂无素材</p>
            <el-button type="primary" @click="uploadMaterial">上传第一个素材</el-button>
          </div>

          <div class="pagination-bar">
            <el-pagination
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
              :current-page="currentPage"
              :page-sizes="[12, 24, 48]"
              :page-size="pageSize"
              :total="totalMaterials"
              layout="total, sizes, prev, pager, next, jumper"
            />
          </div>
        </el-main>
      </el-container>
    </el-container>

    <el-dialog title="素材详情" v-model="showDetail" width="600px">
      <div v-if="selectedMaterial" class="material-detail">
        <img :src="selectedMaterial.imageUrl" class="detail-image" />
        <div class="detail-info">
          <h3>{{ selectedMaterial.name }}</h3>
          <p class="detail-desc">{{ selectedMaterial.description }}</p>
          <div class="detail-meta">
            <div class="meta-item">
              <span class="meta-label">分类</span>
              <span class="meta-value">{{ getCategoryLabel(selectedMaterial.category) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">类型</span>
              <span class="meta-value">{{ selectedMaterial.type }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">组别</span>
              <span class="meta-value">{{ getGroupName(selectedMaterial.groupId) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">使用次数</span>
              <span class="meta-value">{{ selectedMaterial.usageCount }}</span>
            </div>
          </div>
          <div class="detail-tags">
            <span v-for="tag in selectedMaterial.tags" :key="tag" class="detail-tag">{{ tag }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
        <el-button type="primary" @click="useMaterial">使用素材</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { 
  PlayCircle, User, Search, FolderOpen, Image as ImageIcon,
  Package, Sparkles, FileText, LayoutGrid, List, Upload, Star, ArrowLeft
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const searchKeyword = ref('')
const selectedCategory = ref('all')
const selectedGroup = ref('')
const selectedTags = ref([])
const viewMode = ref('grid')
const currentPage = ref(1)
const pageSize = ref(12)

const showDetail = ref(false)
const selectedMaterial = ref(null)

const groups = ref([
  { id: 'group1', name: '默认组别' },
  { id: 'group2', name: '团队共享' },
  { id: 'group3', name: '个人收藏' }
])

const popularTags = ['动漫', '古风', '科幻', '校园', '战斗', '日常']

const materials = ref([
  { 
    id: '1', 
    name: '魔法少女角色', 
    description: '可爱的魔法少女角色设计，适合动漫风格创作',
    imageUrl: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=magical%20girl%20anime%20character%20design&image_size=square',
    category: 'character',
    type: 'image',
    groupId: 'group1',
    tags: ['动漫', '角色', '魔法'],
    usageCount: 28,
    starred: true
  },
  { 
    id: '2', 
    name: '日式校园背景', 
    description: '精美的日式校园场景，包含樱花树和教学楼',
    imageUrl: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=japanese%20school%20anime%20background%20cherry%20blossom&image_size=square',
    category: 'background',
    type: 'image',
    groupId: 'group2',
    tags: ['校园', '日式', '樱花'],
    usageCount: 45,
    starred: false
  },
  { 
    id: '3', 
    name: '魔法道具合集', 
    description: '各种魔法道具素材，包括魔杖、水晶球等',
    imageUrl: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=magic%20props%20wand%20crystal%20ball%20fantasy&image_size=square',
    category: 'prop',
    type: 'image',
    groupId: 'group1',
    tags: ['魔法', '道具', '幻想'],
    usageCount: 15,
    starred: true
  },
  { 
    id: '4', 
    name: '特效素材包', 
    description: '各种特效素材，包括魔法光效、粒子效果',
    imageUrl: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=magic%20effects%20particles%20glow%20anime&image_size=square',
    category: 'effect',
    type: 'image',
    groupId: 'group2',
    tags: ['特效', '光效', '粒子'],
    usageCount: 32,
    starred: false
  },
  { 
    id: '5', 
    name: '古风场景', 
    description: '中国古风场景，包含亭台楼阁和山水',
    imageUrl: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=ancient%20chinese%20palace%20garden%20anime&image_size=square',
    category: 'background',
    type: 'image',
    groupId: 'group3',
    tags: ['古风', '中国风', '场景'],
    usageCount: 22,
    starred: true
  },
  { 
    id: '6', 
    name: '科幻都市', 
    description: '未来科幻都市场景，充满科技感',
    imageUrl: 'https://neeko-copilot.bytedance.net/api/text_to_image?prompt=futuristic%20city%20cyberpunk%20anime%20style&image_size=square',
    category: 'background',
    type: 'image',
    groupId: 'group1',
    tags: ['科幻', '都市', '未来'],
    usageCount: 18,
    starred: false
  }
])

const filteredMaterials = computed(() => {
  let result = materials.value
  
  if (selectedCategory.value !== 'all') {
    result = result.filter(m => m.category === selectedCategory.value)
  }
  
  if (selectedGroup.value) {
    result = result.filter(m => m.groupId === selectedGroup.value)
  }
  
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(m => 
      m.name.toLowerCase().includes(keyword) ||
      m.description.toLowerCase().includes(keyword) ||
      m.tags.some(t => t.toLowerCase().includes(keyword))
    )
  }
  
  if (selectedTags.value.length > 0) {
    result = result.filter(m => 
      selectedTags.value.some(tag => m.tags.includes(tag))
    )
  }
  
  return result
})

const totalMaterials = computed(() => filteredMaterials.value.length)

const getCategoryLabel = (category) => {
  const labels = {
    character: '角色素材',
    background: '背景素材',
    prop: '道具素材',
    effect: '特效素材',
    script: '脚本素材',
    other: '其他'
  }
  return labels[category] || '其他'
}

const getGroupName = (groupId) => {
  const group = groups.value.find(g => g.id === groupId)
  return group ? group.name : '未分组'
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

const selectCategory = (category) => {
  selectedCategory.value = category
}

const filterByGroup = () => {}

const toggleTag = (tag) => {
  const index = selectedTags.value.indexOf(tag)
  if (index === -1) {
    selectedTags.value.push(tag)
  } else {
    selectedTags.value.splice(index, 1)
  }
}

const searchMaterials = () => {}

const selectMaterial = (material) => {
  selectedMaterial.value = material
  showDetail.value = true
}

const uploadMaterial = () => {
  ElMessage.info('上传素材功能开发中')
}

const useMaterial = () => {
  ElMessage.success('素材已添加到画布')
  showDetail.value = false
}

const handleSizeChange = (size) => {
  pageSize.value = size
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}
</script>

<style scoped>
.library-container {
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
  padding: 16px;
}

.search-box {
  margin-bottom: 20px;
}

.category-list h3, .group-filter h3 {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 12px;
  font-family: var(--font-heading);
}

.category-menu {
  border-right: none;
  margin-bottom: 20px;
}

.content-area {
  padding: 24px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.content-header h2 {
  font-family: var(--font-heading);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.tags-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.tag {
  background: var(--bg-card-hover);
  padding: 6px 14px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
  transition: all 0.2s;
  border: 1px solid transparent;
}

.tag:hover {
  border-color: var(--primary-color);
}

.tag.active {
  background: var(--primary-color);
  color: white;
}

.materials-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.materials-container.list {
  grid-template-columns: 1fr;
}

.material-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-slow);
}

.material-card:hover {
  transform: translateY(-4px);
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.material-cover {
  position: relative;
  height: 180px;
  overflow: hidden;
}

.material-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.material-overlay {
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

.material-card:hover .material-overlay {
  opacity: 1;
}

.material-info {
  padding: 16px;
}

.material-info h4 {
  margin-bottom: 8px;
  font-size: 15px;
  font-family: var(--font-heading);
}

.description {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.material-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category-tag {
  background: var(--bg-elevated);
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}

.usage-count {
  font-size: 12px;
  color: var(--text-muted);
}

.empty-state {
  text-align: center;
  padding: 60px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  color: var(--bg-elevated);
  margin-bottom: 20px;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

.material-detail {
  display: flex;
  gap: 24px;
}

.detail-image {
  width: 250px;
  height: 250px;
  object-fit: cover;
  border-radius: var(--radius-md);
}

.detail-info {
  flex: 1;
}

.detail-info h3 {
  margin-bottom: 12px;
  font-family: var(--font-heading);
}

.detail-desc {
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.detail-meta {
  margin-bottom: 20px;
}

.meta-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
}

.meta-label {
  color: var(--text-secondary);
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-tag {
  background: var(--primary-color);
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  color: white;
}
</style>
