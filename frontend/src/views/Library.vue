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
              <el-menu-item index="image">
                <ImageIcon class="icon" />
                <span>图片素材</span>
              </el-menu-item>
              <el-menu-item index="video">
                <VideoIcon class="icon" />
                <span>视频素材</span>
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

          <div :class="['materials-container', viewMode]" v-loading="loading">
            <div v-for="material in filteredMaterials" :key="material.id" class="material-card" @click="selectMaterial(material)">
              <div class="material-cover">
                <img v-if="material.type === 'image'" :src="material.imageUrl || material.url" />
                <video v-else-if="material.type === 'video'" :src="material.url || material.imageUrl" controls preload="metadata" />
                <div v-if="material.type === 'video'" class="video-overlay">
                  <VideoIcon class="play-icon" />
                </div>
                <div class="material-overlay">
                  <el-button size="small" icon="Download">下载</el-button>
                  <el-button size="small" icon="Star" :class="{ starred: material.starred }">收藏</el-button>
                  <el-button size="small" icon="Delete" @click.stop="handleDeleteMaterial(material.id)">删除</el-button>
                </div>
              </div>
              <div class="material-info">
                <div class="material-name-wrapper">
                  <input 
                    v-if="editingId === material.id"
                    v-model="editName"
                    class="name-input"
                    @blur="saveMaterialName(material)"
                    @keyup.enter="saveMaterialName(material)"
                    @keyup.esc="cancelEdit(material)"
                    ref="nameInputRef"
                  />
                  <h4 v-else @click="startEdit(material)">{{ material.name }}</h4>
                </div>
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
import { ref, computed, onMounted } from 'vue'
import { 
  PlayCircle, User, Search, FolderOpen, Image as ImageIcon,
  Package, Sparkles, FileText, LayoutGrid, List, Upload, Star, ArrowLeft, Image, Video as VideoIcon
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { materialAPI } from '../api/material.js'

const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const searchKeyword = ref('')
const selectedCategory = ref('all')
const selectedGroup = ref('')
const selectedTags = ref([])
const viewMode = ref('grid')
const currentPage = ref(1)
const pageSize = ref(12)
const loading = ref(false)

const showDetail = ref(false)
const selectedMaterial = ref(null)

const groups = ref([])

const popularTags = ['动漫', '古风', '科幻', '校园', '战斗', '日常']

const materials = ref([])

// 编辑状态
const editingId = ref(null)
const editName = ref('')
const nameInputRef = ref(null)

const loadMaterials = async () => {
  loading.value = true
  try {
    const params = {}
    if (selectedCategory.value !== 'all') {
      params.type = selectedCategory.value
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    
    const response = await materialAPI.getMaterials(params)
    materials.value = response.data.materials || []
    
    materials.value = materials.value.map(m => ({
      ...m,
      imageUrl: m.url,
      category: m.type,
      tags: m.tags ? m.tags.split(',').filter(t => t) : []
    }))
  } catch (error) {
    console.error('加载素材失败:', error)
    ElMessage.error('加载素材库失败')
  } finally {
    loading.value = false
  }
}

const filteredMaterials = computed(() => {
  let result = materials.value
  
  if (selectedCategory.value !== 'all') {
    result = result.filter(m => m.type === selectedCategory.value)
  }
  
  if (selectedGroup.value) {
    result = result.filter(m => m.groupId === selectedGroup.value)
  }
  
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(m => 
      (m.name && m.name.toLowerCase().includes(keyword)) ||
      (m.description && m.description.toLowerCase().includes(keyword)) ||
      (m.originalPrompt && m.originalPrompt.toLowerCase().includes(keyword)) ||
      (m.tags && m.tags.some(t => t.toLowerCase().includes(keyword)))
    )
  }
  
  if (selectedTags.value.length > 0) {
    result = result.filter(m => 
      selectedTags.value.some(tag => m.tags && m.tags.includes(tag))
    )
  }
  
  return result
})

const totalMaterials = computed(() => filteredMaterials.value.length)

const getCategoryLabel = (category) => {
  const labels = {
    image: '图片素材',
    video: '视频素材',
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
  loadMaterials()
}

const filterByGroup = () => {
  loadMaterials()
}

const toggleTag = (tag) => {
  const index = selectedTags.value.indexOf(tag)
  if (index === -1) {
    selectedTags.value.push(tag)
  } else {
    selectedTags.value.splice(index, 1)
  }
}

const searchMaterials = () => {
  loadMaterials()
}

const selectMaterial = (material) => {
  selectedMaterial.value = material
  showDetail.value = true
}

const uploadMaterial = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*,video/*'
  input.multiple = false
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    try {
      const type = file.type.startsWith('image/') ? 'image' : 'video'
      const response = await materialAPI.uploadMaterial(file, type)
      ElMessage.success('上传成功')
      loadMaterials()
    } catch (error) {
      console.error('上传失败:', error)
      ElMessage.error('上传失败，请重试')
    }
  }
  input.click()
}

const useMaterial = () => {
  ElMessage.success('素材已添加到画布')
  showDetail.value = false
}

const handleDeleteMaterial = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个素材吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await materialAPI.deleteMaterial(id)
    ElMessage.success('删除成功')
    loadMaterials()
    showDetail.value = false
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}

// 开始编辑名称
const startEdit = (material) => {
  editingId.value = material.id
  editName.value = material.name
}

// 保存名称
const saveMaterialName = async (material) => {
  if (!editName.value.trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  
  try {
    await materialAPI.updateMaterialName(material.id, editName.value)
    material.name = editName.value
    ElMessage.success('名称修改成功')
  } catch (error) {
    console.error('修改名称失败:', error)
    ElMessage.error('修改名称失败')
  } finally {
    editingId.value = null
  }
}

// 取消编辑
const cancelEdit = () => {
  editingId.value = null
}

onMounted(() => {
  loadMaterials()
})
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
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
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
  height: 250px;
  overflow: hidden;
}

.material-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.material-cover video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.play-icon {
  width: 44px;
  height: 44px;
  color: white;
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
  cursor: pointer;
  transition: color var(--transition-fast);
}

.material-info h4:hover {
  color: var(--primary-color);
}

.material-name-wrapper {
  position: relative;
}

.name-input {
  width: 100%;
  padding: 4px 8px;
  font-size: 15px;
  font-family: var(--font-heading);
  background: var(--bg-card-hover);
  border: 1px solid var(--primary-color);
  border-radius: 4px;
  color: var(--text-primary);
  outline: none;
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
