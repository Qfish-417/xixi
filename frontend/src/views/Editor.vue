<template>
  <div class="editor-container">
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
        <div class="nav-center">
          <el-button-group>
            <el-button icon="Save" @click="saveProject">保存</el-button>
            <el-button icon="Download" @click="exportProject">导出</el-button>
            <el-button icon="Undo" @click="undoAction">撤销</el-button>
            <el-button icon="Redo" @click="redoAction">重做</el-button>
          </el-button-group>
        </div>
        <div class="nav-right">
          <span class="project-name">{{ projectName }}</span>
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
        <el-aside width="240px" class="sidebar">
          <div class="tool-panel">
            <div class="tool-actions">
              <el-button class="sidebar-btn primary-btn" @click="addGenerateBox">
                <Plus class="btn-icon" />
                <span>添加生成框</span>
              </el-button>
              <el-button class="sidebar-btn" @click="toggleLibrary">
                <FolderOpen class="btn-icon" />
                <span>素材库 ({{ imageElements.length }})</span>
              </el-button>
            </div>
          </div>

          <div v-if="showLibrary" class="library-panel">
            <div class="library-header">
              <h3>我的素材</h3>
              <span class="library-count">{{ imageElements.length }} 张</span>
            </div>
            <div class="library-grid">
              <div
                v-for="img in imageElements"
                :key="img.id"
                class="library-item"
                :class="{ active: selectedElementId === img.id }"
                @click="selectImageElement(img)"
              >
                <img :src="img.src" :alt="img.name" />
                <div class="library-item-info">
                  <span class="library-item-name">{{ img.name }}</span>
                  <span class="library-item-delete" @click.stop="deleteImageElement(img)">×</span>
                </div>
              </div>
              <div v-if="imageElements.length === 0" class="library-empty">
                <Image class="empty-icon" />
                <span>暂无素材</span>
                <span class="empty-hint">添加生成框并生成图片后<br />素材将显示在这里</span>
              </div>
            </div>
          </div>

          <div class="connection-panel">
            <h3>连接管理</h3>
            <div class="connection-info">
              <p>连接线数: {{ connections.length }}</p>
              <el-button size="small" @click="clearConnections">清空连接</el-button>
            </div>
          </div>
        </el-aside>

        <el-container>
          <el-main class="canvas-area">
            <div
              ref="canvasWrapper"
              class="canvas-wrapper"
              @mousedown="onCanvasWrapperMouseDown"
              @mousemove="onCanvasWrapperMouseMove"
              @mouseup="onCanvasWrapperMouseUp"
              @mouseleave="onCanvasWrapperMouseUp"
              @wheel="onCanvasWheel"
              @contextmenu.prevent
            >
              <svg class="connections-svg">
                <line
                  v-for="conn in connections"
                  :key="conn.id"
                  :x1="conn.x1"
                  :y1="conn.y1"
                  :x2="conn.x2"
                  :y2="conn.y2"
                  stroke="#6366f1"
                  stroke-width="2"
                  class="connection-line"
                  vector-effect="non-scaling-stroke"
                  @click="selectConnection(conn.id)"
                />
                <line
                  v-if="tempConnection"
                  :x1="tempConnection.x1"
                  :y1="tempConnection.y1"
                  :x2="tempConnection.x2"
                  :y2="tempConnection.y2"
                  stroke="#8b5cf6"
                  stroke-width="2"
                  stroke-dasharray="6 3"
                  class="temp-connection-line"
                  vector-effect="non-scaling-stroke"
                />
              </svg>
              
              <div
                class="canvas-container"
                :style="canvasContainerStyle"
                @click="onCanvasClick"
              >
                <div class="canvas-grid"></div>
                
                <!-- 元素层 -->
                <div
                  v-for="element in canvasElements"
                  :key="element.id"
                  :data-element-id="element.id"
                  :class="['canvas-element', { selected: selectedElementId === element.id, 'generate-box': element.type === 'generate', 'box-element': element.type === 'box', 'dragging': isDragging && selectedElementId === element.id }]"
                  :style="getElementStyle(element)"
                  @mousedown="onElementMouseDown($event, element)"
                  @dblclick.stop="onElementDoubleClick(element)"
                >
                  <div 
                    v-if="element.type === 'generate'" 
                    class="generate-box-inner"
                  >
                    <!-- 顶部拖动条 -->
                    <div class="generate-box-drag-bar" @mousedown.stop="onDragBarMouseDown($event, element)">
                      <span class="drag-handle">⠿</span>
                      <span class="box-title">{{ element.name }}</span>
                      <span class="box-delete" @click.stop="deleteGenerateBox(element)" title="删除">×</span>
                    </div>
                    
                    <div class="generate-box-content">
                      <div class="generate-box-image-section">
                        <template v-if="element.resultType === 'video' && element.images?.length">
                          <div class="video-result-wrapper">
                            <video :src="element.images[0]" controls class="video-result" />
                          </div>
                        </template>
                        <template v-else-if="!element.images || element.images.length === 0">
                          <div class="image-placeholder" @click.stop="triggerImageInputForElement(element)">
                            <component :is="element.genType === 'video' ? Video : Image" class="placeholder-icon" />
                            <span>{{ element.genType === 'video' ? '上传图片/视频作为生成素材' : '点击添加参考图片' }}</span>
                          </div>
                        </template>
                        <div v-else class="image-gallery">
                          <div
                            v-for="(img, idx) in element.images"
                            :key="idx"
                            class="gallery-thumb"
                            @click.stop="removeImage(element, idx)"
                          >
                            <img v-if="!img.startsWith('data:video')" :src="img" class="thumb-img" />
                            <video v-else :src="img" class="thumb-img" muted />
                            <span class="thumb-remove">×</span>
                          </div>
                          <div class="gallery-add" @click.stop="triggerImageInputForElement(element)">
                            <span>+</span>
                          </div>
                        </div>
                      </div>
                      <div class="generate-box-divider"></div>
                      <div class="generate-box-prompt-section">
                        <textarea 
                          v-model="element.prompt"
                          placeholder="输入提示词..."
                          class="prompt-input"
                          @focus="onTextareaFocus($event)"
                        ></textarea>
                        <div class="generate-box-type">
                          <span
                            :class="['type-tab', { active: element.genType === 'image' }]"
                            @click.stop="element.genType = 'image'"
                          >
                            <ImageIcon class="type-icon" />
                            图片
                          </span>
                          <span
                            :class="['type-tab', { active: element.genType === 'video' }]"
                            @click.stop="element.genType = 'video'"
                          >
                            <Video class="type-icon" />
                            视频
                          </span>
                        </div>
                        <div class="generate-box-footer">
                          <el-select v-model="element.resolution" class="resolution-select">
                            <el-option v-if="element.genType === 'image'" label="512×512" value="512x512" />
                            <el-option v-if="element.genType === 'image'" label="1024×1024" value="1024x1024" />
                            <el-option v-if="element.genType === 'image'" label="1920×1080" value="1920x1080" />
                            <el-option v-if="element.genType === 'video'" label="1920×1080" value="1920x1080" />
                            <el-option v-if="element.genType === 'video'" label="1280×720" value="1280x720" />
                            <el-option v-if="element.genType === 'video'" label="1080×1920" value="1080x1920" />
                          </el-select>
                          <el-button
                            type="primary"
                            :loading="element.isGenerating"
                            @click="generateFromBox(element)"
                          >
                            {{ element.isGenerating ? '生成中...' : '生成' }}
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <div v-else-if="element.type === 'image'" class="image-wrapper">
                    <img :src="element.src" :alt="element.name" class="element-image" draggable="false" />
                    <span class="image-delete-btn" @mousedown.stop @click.stop="deleteImageElement(element)">×</span>
                  </div>
                  
                  <div v-else-if="element.type === 'text'" class="text-element-content">
                    <span contenteditable="true" @blur="onTextBlur($event, element)" class="element-text">{{ element.content }}</span>
                  </div>
                  
                  <div v-else-if="element.type === 'box'" class="box-element-content">
                    <div class="box-border"></div>
                  </div>
                  
                  <div v-if="element.type === 'generate'" class="element-resize-handles">
                    <span class="handle top-left" @mousedown.stop="startResize($event, element, 'top-left')"></span>
                    <span class="handle top-right" @mousedown.stop="startResize($event, element, 'top-right')"></span>
                    <span class="handle bottom-left" @mousedown.stop="startResize($event, element, 'bottom-left')"></span>
                    <span class="handle bottom-right" @mousedown.stop="startResize($event, element, 'bottom-right')"></span>
                  </div>
                  
                  <div v-if="element.type === 'generate'" class="connection-points">
                    <span class="connection-point top" @mousedown.stop="startConnection($event, element, 'top')"></span>
                    <span class="connection-point bottom" @mousedown.stop="startConnection($event, element, 'bottom')"></span>
                    <span class="connection-point left" @mousedown.stop="startConnection($event, element, 'left')"></span>
                    <span class="connection-point right" @mousedown.stop="startConnection($event, element, 'right')"></span>
                  </div>
                </div>
              </div>
              
              <div class="canvas-controls">
                <el-button-group>
                  <el-button icon="ZoomIn" @click="zoomIn">放大</el-button>
                  <span class="zoom-value">{{ Math.round(zoom * 100) }}%</span>
                  <el-button icon="ZoomOut" @click="zoomOut">缩小</el-button>
                  <el-button icon="Maximize2" @click="resetZoom">重置</el-button>
                </el-button-group>
              </div>
            </div>
          </el-main>

          <el-aside width="300px" class="properties-panel">
            <h3>属性面板</h3>
            
            <div v-if="activeTool === 'brush'" class="tool-properties">
              <h4>画笔设置</h4>
              <el-form :model="brushSettings">
                <el-form-item label="画笔大小">
                  <el-slider v-model="brushSettings.size" :min="1" :max="50" />
                </el-form-item>
                <el-form-item label="画笔颜色">
                  <el-color-picker v-model="brushSettings.color" />
                </el-form-item>
                <el-form-item label="透明度">
                  <el-slider v-model="brushSettings.opacity" :min="10" :max="100" />
                </el-form-item>
              </el-form>
            </div>
            
            <div v-if="activeTool === 'text'" class="tool-properties">
              <h4>文字设置</h4>
              <el-form :model="textSettings">
                <el-form-item label="文字内容">
                  <el-input v-model="textSettings.content" />
                </el-form-item>
                <el-form-item label="字体大小">
                  <el-slider v-model="textSettings.size" :min="12" :max="72" />
                </el-form-item>
                <el-form-item label="文字颜色">
                  <el-color-picker v-model="textSettings.color" />
                </el-form-item>
              </el-form>
            </div>
            
            <div v-if="selectedElement" class="element-properties">
              <h4>元素属性</h4>
              <el-form :model="selectedElement">
                <el-form-item label="位置 X">
                  <el-input-number v-model="selectedElement.x" style="width: 100%" />
                </el-form-item>
                <el-form-item label="位置 Y">
                  <el-input-number v-model="selectedElement.y" style="width: 100%" />
                </el-form-item>
                <el-form-item label="宽度">
                  <el-input-number v-model="selectedElement.width" :min="1" style="width: 100%" />
                </el-form-item>
                <el-form-item label="高度">
                  <el-input-number v-model="selectedElement.height" :min="1" style="width: 100%" />
                </el-form-item>
                <el-form-item label="旋转角度">
                  <el-slider v-model="selectedElement.rotation" :min="-180" :max="180" />
                </el-form-item>
                <el-form-item label="透明度">
                  <el-slider v-model="selectedElement.opacity" :min="10" :max="100" />
                </el-form-item>
              </el-form>
              <el-button type="danger" icon="Trash" @click="deleteElement">删除元素</el-button>
            </div>
            
            <div v-if="activeTool === 'connect'" class="connection-tips">
              <h4>连线模式</h4>
              <p>点击元素上的圆点开始连接，然后点击目标元素完成连接</p>
            </div>
            
            <div class="project-info">
              <h4>画布信息</h4>
              <p>尺寸: {{ canvasWidth }} x {{ canvasHeight }}</p>
              <p>元素数: {{ canvasElements.length }}</p>
              <p>连接数: {{ connections.length }}</p>
              <p>缩放: {{ Math.round(zoom * 100) }}%</p>
            </div>
          </el-aside>
        </el-container>
      </el-container>
    </el-container>
    
    <!-- 图片上传弹窗 -->
    <el-dialog title="插入图片" :visible.sync="showImageUploadDialog" width="400px">
      <div class="upload-area" @click="triggerImageInput">
        <Image class="upload-icon" />
        <span>点击或拖拽上传图片</span>
        <input type="file" accept="image/*" id="imageUploadInput" @change="handleImageFileSelect" />
      </div>
      <div v-if="uploadedImageUrl" class="preview-area">
        <img :src="uploadedImageUrl" class="preview-image" />
        <div class="preview-info">
          <p>图片已准备好，点击确定插入到画布</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="showImageUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="insertImageToCanvas" :disabled="!uploadedImageUrl">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed, watch, getCurrentInstance } from 'vue'
import { useRoute } from 'vue-router'
import {
  PlayCircle, User, MousePointer, Pencil, Type, Eraser,
  Sparkles, Image, Video, FileText, FolderOpen, Plus, Trash, Link, Square, ArrowLeft
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox, ElDialog } from 'element-plus'

const route = useRoute()
const { proxy } = getCurrentInstance()

const projectName = ref('未命名项目')
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

const activeTool = ref('select')
const zoom = ref(1)

// 画布尺寸
const canvasWidth = ref(1200)
const canvasHeight = ref(800)

// 画布偏移（用于平移）
const canvasOffset = reactive({ x: 600, y: 400 })

// 初始化时将画布中心对齐到视口中心
onMounted(() => {
  const wrapper = document.querySelector('.canvas-wrapper')
  if (wrapper) {
    canvasOffset.x = wrapper.clientWidth / 2
    canvasOffset.y = wrapper.clientHeight / 2
  }
  
  // 处理从首页传递的工具参数
  const tool = route.query.tool
  if (tool) {
    handleToolFromRoute(tool)
  }
})

// 处理从路由传递的工具类型
const handleToolFromRoute = (toolName) => {
  const toolNames = {
    text2image: '文生图',
    image2image: '图生图',
    image2video: '图生视频',
    script: '脚本生成'
  }
  
  ElMessage.info(`已打开【${toolNames[toolName] || toolName}】工具`)
  
  // 根据工具类型执行相应操作
  switch (toolName) {
    case 'text2image':
      // 添加一个文生图生成框
      addGenerateBoxWithType('text2image')
      break
    case 'image2image':
      // 添加一个图生图生成框
      addGenerateBoxWithType('image2image')
      break
    case 'image2video':
      // 添加一个图生视频生成框
      addGenerateBoxWithType('image2video')
      break
    case 'script':
      // 添加一个脚本生成框
      addGenerateBoxWithType('script')
      break
    default:
      addGenerateBox()
  }
}

// 根据类型添加生成框
const addGenerateBoxWithType = (type) => {
  const id = Date.now()
  const types = {
    text2image: { name: '文生图', placeholder: '描述你想要的图片...' },
    image2image: { name: '图生图', placeholder: '上传图片并描述风格...' },
    image2video: { name: '图生视频', placeholder: '上传图片生成视频...' },
    script: { name: '脚本生成', placeholder: '描述剧本主题...' }
  }
  
  const typeInfo = types[type] || types.text2image
  
  const newElement = {
    id,
    type: 'generate',
    name: typeInfo.name,
    x: (canvasOffset.x - 175) / zoom.value,
    y: (canvasOffset.y - 140) / zoom.value,
    width: 350,
    height: 280,
    rotation: 0,
    opacity: 100,
    layer: 0,
    toolType: type,
    content: '',
    placeholder: typeInfo.placeholder,
    status: 'idle',
    generatedImage: null,
    generatedVideo: null
  }
  
  elements.value.push(newElement)
  selectedElementId.value = id
  
  ElMessage.success(`已添加${typeInfo.name}生成框`)
}

const brushSettings = reactive({
  size: 5,
  color: '#ffffff',
  opacity: 100
})

const textSettings = reactive({
  content: '双击编辑文字',
  size: 24,
  color: '#ffffff'
})

// 无限画布 - 无需传统图层系统
const selectedElementId = ref(null)
const selectedConnectionId = ref(null)

const canvas = ref(null)
let isDrawing = false
let isDragging = false
let isPanning = false
let isConnecting = false
let connectStartElement = null
let connectStartPosition = null

let dragStartX = 0
let dragStartY = 0
let panStartX = 0
let panStartY = 0
let panStartCameraX = 0
let panStartCameraY = 0

// 长按相关变量
let longPressTimer = null
let longPressStartTime = 0
let longPressStartX = 0
let longPressStartY = 0
let isLongPressMode = false
const LONG_PRESS_DELAY = 4000 // 4秒长按
const MOVE_THRESHOLD = 10 // 鼠标移动超过这个距离则取消长按

// 清空默认画布数据，让用户从零开始
const canvasElements = ref([])

const connections = ref([])

// 拖拽连线时的临时线
const tempConnection = ref(null)

// 获取 wrapper 相对视口的偏移
const getWrapperRect = () => {
  const el = document.querySelector('.canvas-wrapper')
  return el ? el.getBoundingClientRect() : { left: 0, top: 0 }
}

// 世界坐标 → SVG 坐标（SVG 原点 = wrapper 左上角）
const worldToScreen = (wx, wy) => ({
  x: wx * zoom.value + canvasOffset.x,
  y: wy * zoom.value + canvasOffset.y
})

// 获取元素连接点的世界坐标
const getConnectionPointPos = (el, position) => {
  const cx = el.x + el.width / 2
  const cy = el.y + el.height / 2
  switch (position) {
    case 'top': return { x: cx, y: el.y }
    case 'bottom': return { x: cx, y: el.y + el.height }
    case 'left': return { x: el.x, y: cy }
    case 'right': return { x: el.x + el.width, y: cy }
    default: return { x: cx, y: cy }
  }
}

// 自动选择最近连接点
const getNearestConnectionPoint = (el, targetWorldX, targetWorldY) => {
  const cx = el.x + el.width / 2
  const cy = el.y + el.height / 2
  const dx = targetWorldX - cx
  const dy = targetWorldY - cy
  const absDx = Math.abs(dx)
  const absDy = Math.abs(dy)

  if (absDx >= absDy) {
    return dx > 0 ? 'right' : 'left'
  } else {
    return dy > 0 ? 'bottom' : 'top'
  }
}

// 从连接点位置获取线段的另一端点世界坐标
const getEdgePoint = (el, position) => {
  return getConnectionPointPos(el, position)
}

// 获取与某个元素相连的所有元素
const getConnectedElements = (elementId) => {
  const connected = []
  connections.value.forEach(conn => {
    if (conn.fromId === elementId) {
      const el = canvasElements.value.find(e => e.id === conn.toId)
      if (el) connected.push(el)
    }
    if (conn.toId === elementId) {
      const el = canvasElements.value.find(e => e.id === conn.fromId)
      if (el) connected.push(el)
    }
  })
  return connected
}

// 素材库状态
const showLibrary = ref(false)
const imageElements = computed(() => canvasElements.value.filter(el => el.type === 'image'))

const toggleLibrary = () => {
  showLibrary.value = !showLibrary.value
}

const selectImageElement = (img) => {
  selectedElementId.value = img.id
  // 将画布平移到元素位置
  canvasOffset.x = -(img.x * zoom.value - window.innerWidth / 2) + (img.width * zoom.value) / 2
  canvasOffset.y = -(img.y * zoom.value - window.innerHeight / 2) + (img.height * zoom.value) / 2
}

// 图片上传弹窗状态
const showImageUploadDialog = ref(false)
const uploadedImageUrl = ref('')

const canvasContainerStyle = computed(() => ({
  transform: `translate(${canvasOffset.x}px, ${canvasOffset.y}px) scale(${zoom.value})`,
  transformOrigin: '0 0'
}))

const selectedElement = computed(() => {
  return canvasElements.value.find(el => el.id === selectedElementId.value) || null
})

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

const selectTool = (tool) => {
  activeTool.value = tool
  if (tool !== 'select') {
    selectedElementId.value = null
  }
  isConnecting = false
  connectStartElement = null
}

const zoomIn = () => {
  zoom.value = Math.min(zoom.value * 1.2, 3)
}

const zoomOut = () => {
  zoom.value = Math.max(zoom.value / 1.2, 0.25)
}

const resetZoom = () => {
  zoom.value = 1
  canvasOffset.x = 0
  canvasOffset.y = 0
}

const saveProject = () => {
  ElMessage.success('项目已保存')
}

const exportProject = () => {
  ElMessage.info('正在导出项目...')
}

const undoAction = () => {
  ElMessage.info('撤销操作')
}

const redoAction = () => {
  ElMessage.info('重做操作')
}

// 打开图片上传弹窗
const openImageUpload = () => {
  showImageUploadDialog.value = true
}

const triggerImageInput = () => {
  document.getElementById('imageUploadInput').click()
}

const handleImageFileSelect = (event) => {
  const file = event.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      uploadedImageUrl.value = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

// 获取视口中心的辅助函数
const getViewportCenter = () => {
  const wrapper = document.querySelector('.canvas-wrapper')
  return {
    x: wrapper ? wrapper.clientWidth / 2 : 600,
    y: wrapper ? wrapper.clientHeight / 2 : 400
  }
}

// 将屏幕坐标转换为世界坐标
const screenToWorld = (screenX, screenY) => {
  return {
    x: (screenX - canvasOffset.x) / zoom.value,
    y: (screenY - canvasOffset.y) / zoom.value
  }
}

const insertImageToCanvas = () => {
  if (!uploadedImageUrl.value) return

  const center = getViewportCenter()
  const worldCenter = screenToWorld(center.x, center.y)

  const newImage = {
    id: Date.now(),
    type: 'image',
    src: uploadedImageUrl.value,
    name: `图片${canvasElements.value.filter(el => el.type === 'image').length + 1}`,
    x: worldCenter.x - 150,
    y: worldCenter.y - 100,
    width: 300,
    height: 200,
    rotation: 0,
    opacity: 100
  }
  
  canvasElements.value.push(newImage)
  selectedElementId.value = newImage.id
  uploadedImageUrl.value = ''
  showImageUploadDialog.value = false
  
  ElMessage.success('图片已插入画布')
}

// 添加方框元素
const addBoxElement = () => {
  const center = getViewportCenter()
  const worldCenter = screenToWorld(center.x, center.y)

  const newBox = {
    id: Date.now(),
    type: 'box',
    name: `方框${canvasElements.value.filter(el => el.type === 'box').length + 1}`,
    x: worldCenter.x - 100,
    y: worldCenter.y - 75,
    width: 200,
    height: 150,
    rotation: 0,
    opacity: 100,
    backgroundColor: 'rgba(99, 102, 241, 0.2)',
    borderColor: '#6366f1',
    borderWidth: 2
  }
  
  canvasElements.value.push(newBox)
  selectedElementId.value = newBox.id
  
  ElMessage.info('已添加方框')
}

const addGenerateBox = () => {
  const center = getViewportCenter()
  const worldCenter = screenToWorld(center.x, center.y)

  const newBox = {
    id: Date.now(),
    type: 'generate',
    name: `生成框${canvasElements.value.filter(el => el.type === 'generate').length + 1}`,
    x: worldCenter.x - 175,
    y: worldCenter.y - 140,
    width: 350,
    height: 320,
    rotation: 0,
    opacity: 100,
    prompt: '',
    images: [],
    resolution: '1024x1024',
    isGenerating: false,
    genType: 'image'
  }
  canvasElements.value.push(newBox)
  selectedElementId.value = newBox.id

  ElMessage.success('生成框已添加到画布')
}

const openImageToImage = () => {
  ElMessage.info('图生图功能开发中')
}

const openImageToVideo = () => {
  ElMessage.info('图生视频功能开发中')
}

const openScriptGenerator = () => {
  ElMessage.info('脚本生成功能开发中')
}

const openLibrary = () => {
  window.location.href = '/library'
}

const triggerImageInputForElement = (element) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = element.genType === 'video' ? 'video/*,image/*' : 'image/*'
  input.multiple = true
  input.onchange = (e) => {
    handleImageUpload(e, element)
  }
  input.click()
}

const handleImageUpload = (event, element) => {
  const files = event.target.files
  if (!files || files.length === 0) return

  if (!element.images) element.images = []

  Array.from(files).forEach(file => {
    const reader = new FileReader()
    reader.onload = (e) => {
      element.images.push(e.target.result)
    }
    reader.readAsDataURL(file)
  })

  ElMessage.success(`已添加 ${files.length} 张参考图片`)
}

const removeImage = (element, index) => {
  if (element.images && index !== undefined) {
    element.images.splice(index, 1)
  }
}

const generateFromBox = async (element) => {
  if (!element.prompt.trim() && (!element.images || element.images.length === 0)) {
    ElMessage.error('请输入提示词或添加参考图片')
    return
  }

  element.isGenerating = true

  try {
    const resolution = element.resolution.split('x')
    const width = parseInt(resolution[0])
    const height = parseInt(resolution[1])

    // 收集相连框的提示词作为参考
    const connected = getConnectedElements(element.id)
    let contextPrompts = ''
    if (connected.length > 0) {
      const refs = connected
        .filter(el => el.type === 'generate' && el.prompt.trim())
        .map(el => el.prompt.trim())
      if (refs.length > 0) {
        contextPrompts = `\n[参考关联场景: ${refs.join('; ')}]`
      }
    }

    let fullPrompt = element.prompt + contextPrompts
    const hasImages = element.images && element.images.length > 0
    const isVideo = element.genType === 'video'

    let resultUrl
    await new Promise(resolve => setTimeout(resolve, 2000))

    if (isVideo) {
      // 图生视频：传入参考图片和提示词
      const params = new URLSearchParams()
      params.set('prompt', fullPrompt)
      params.set('resolution', element.resolution)
      params.set('duration', '10')
      params.set('fps', '24')
      if (hasImages) {
        params.set('imageUrls', element.images.join(','))
      }
      resultUrl = `/api/tools/image-to-video?${params.toString()}`
    } else {
      // 文生图
      if (hasImages) {
        fullPrompt = `参考图片风格: ${fullPrompt || '保持图片风格'}`
      }
      resultUrl = `/api/tools/text-to-image?prompt=${encodeURIComponent(fullPrompt)}&resolution=${element.resolution}`
    }

    // 在原框下方新建一个生成框，存放生成结果
    const resultBox = {
      id: Date.now(),
      type: 'generate',
      name: `${element.name} 的结果`,
      x: element.x,
      y: element.y + element.height + 30,
      width: isVideo ? 400 : 350,
      height: isVideo ? 300 : 280,
      rotation: 0,
      opacity: 100,
      prompt: fullPrompt,
      images: [resultUrl],
      resolution: element.resolution,
      isGenerating: false,
      genType: isVideo ? 'video' : 'image',
      resultType: isVideo ? 'video' : 'image'
    }
    canvasElements.value.push(resultBox)

    // 原框 ↔ 结果框 连线
    createConnection(element.id, resultBox.id, 'bottom', 'top')

    ElMessage.success(isVideo ? '视频生成成功！' : '图片生成成功！')
  } catch (error) {
    ElMessage.error('生成失败，请重试')
  } finally {
    element.isGenerating = false
  }
}

const startConnection = (event, element, position) => {
  isConnecting = true
  connectStartElement = element
  connectStartPosition = position

  // 起点 = 连接点 → 转屏幕坐标
  const wp = getConnectionPointPos(element, position)
  const s = worldToScreen(wp.x, wp.y)
  tempConnection.value = { x1: s.x, y1: s.y, x2: s.x, y2: s.y }

  document.addEventListener('mousemove', onConnecting)
  document.addEventListener('mouseup', onConnectEnd)
}

const onConnecting = (e) => {
  if (!isConnecting || !tempConnection.value) return
  // 终点直接跟随鼠标（屏幕坐标）
  const wr = getWrapperRect()
  tempConnection.value.x2 = e.clientX - wr.left
  tempConnection.value.y2 = e.clientY - wr.top
}

const onConnectEnd = (e) => {
  if (isConnecting && connectStartElement) {
    const allTargets = document.elementsFromPoint(e.clientX, e.clientY)
    let targetId = null
    for (const el of allTargets) {
      const elId = el.closest('[data-element-id]')
      if (elId) {
        targetId = Number(elId.dataset.elementId)
        break
      }
    }
    if (targetId && targetId !== connectStartElement.id) {
      const targetEl = canvasElements.value.find(el => el.id === targetId)
      if (targetEl) {
        // 根据两框相对方位决定目标连接点，确保连线不穿框
        const fromCx = connectStartElement.x + connectStartElement.width / 2
        const fromCy = connectStartElement.y + connectStartElement.height / 2
        const toCx = targetEl.x + targetEl.width / 2
        const toCy = targetEl.y + targetEl.height / 2
        const dx = toCx - fromCx
        const dy = toCy - fromCy

        let toPosition
        if (Math.abs(dx) > Math.abs(dy)) {
          toPosition = dx > 0 ? 'left' : 'right'
        } else {
          toPosition = dy > 0 ? 'top' : 'bottom'
        }

        createConnection(connectStartElement.id, targetId, connectStartPosition, toPosition)
        ElMessage.success('连接成功')
      }
    }
  }
  finishConnecting()
}

const finishConnecting = () => {
  isConnecting = false
  connectStartElement = null
  tempConnection.value = null
  document.removeEventListener('mousemove', onConnecting)
  document.removeEventListener('mouseup', onConnectEnd)
}

const createConnection = (fromId, toId, fromPosition, toPosition) => {
  const fromEl = canvasElements.value.find(el => el.id === fromId)
  const toEl = canvasElements.value.find(el => el.id === toId)

  if (fromEl && toEl) {
    const fromPos = fromPosition || 'right'
    const toPos = toPosition || 'left'
    const p1 = getEdgePoint(fromEl, fromPos)
    const p2 = getEdgePoint(toEl, toPos)
    const s1 = worldToScreen(p1.x, p1.y)
    const s2 = worldToScreen(p2.x, p2.y)
    const newConnection = {
      id: Date.now(),
      fromId,
      toId,
      fromPosition: fromPos,
      toPosition: toPos,
      x1: s1.x,
      y1: s1.y,
      x2: s2.x,
      y2: s2.y
    }
    connections.value.push(newConnection)
  }
}

const selectConnection = (connId) => {
  selectedConnectionId.value = connId
}

const deleteImageElement = (element) => {
  connections.value = connections.value.filter(c => c.fromId !== element.id && c.toId !== element.id)
  canvasElements.value = canvasElements.value.filter(el => el.id !== element.id)
  if (selectedElementId.value === element.id) {
    selectedElementId.value = null
  }
  ElMessage.success('已删除')
}

const deleteGenerateBox = (element) => {
  ElMessageBox.confirm('确定删除该生成框吗？', '确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
    buttonSize: 'small'
  }).then(() => {
    // 删除关联的连线
    connections.value = connections.value.filter(c => c.fromId !== element.id && c.toId !== element.id)
    // 删除元素
    canvasElements.value = canvasElements.value.filter(el => el.id !== element.id)
    if (selectedElementId.value === element.id) {
      selectedElementId.value = null
    }
    ElMessage.success('已删除')
  }).catch(() => {})
}

const clearConnections = () => {
  connections.value = []
  ElMessage.info('已清空所有连接')
}

const deleteElement = () => {
  if (selectedElementId.value) {
    const element = canvasElements.value.find(el => el.id === selectedElementId.value)
    
    connections.value = connections.value.filter(
      conn => conn.fromId !== element.id && conn.toId !== element.id
    )
    
    canvasElements.value = canvasElements.value.filter(el => el.id !== selectedElementId.value)
    selectedElementId.value = null
    ElMessage.info('元素已删除')
  }
}

const getElementStyle = (element) => {
  const style = {
    left: `${element.x}px`,
    top: `${element.y}px`,
    width: `${element.width}px`,
    height: `${element.height}px`,
    transform: `rotate(${element.rotation}deg)`,
    opacity: element.opacity / 100
  }
  
  if (element.type === 'text') {
    style.fontSize = `${textSettings.size}px`
    style.color = textSettings.color
  }
  
  if (element.type === 'box') {
    style.backgroundColor = element.backgroundColor || 'rgba(99, 102, 241, 0.2)'
    style.border = `${element.borderWidth || 2}px solid ${element.borderColor || '#6366f1'}`
    style.borderRadius = '4px'
  }
  
  if (element.type === 'generate') {
    style.backgroundColor = 'rgba(99, 102, 241, 0.15)'
    style.border = '2px solid #6366f1'
    style.borderRadius = '8px'
    style.boxShadow = '0 0 20px rgba(99, 102, 241, 0.3)'
  }
  
  return style
}

const onCanvasWrapperMouseDown = (e) => {
  // 如果点击的是画布元素（生成框、图片等），交 onElementMouseDown 处理
  if (e.target.closest('.canvas-element')) return

  // 任意鼠标按钮（左/中/右）在空白区域拖拽 → 平移画布
  // Alt+左键 / 中键 / 右键 / 左键空白区 都支持
  isPanning = true
  panStartX = e.clientX - canvasOffset.x
  panStartY = e.clientY - canvasOffset.y
  e.preventDefault()
}

const onCanvasWrapperMouseMove = (e) => {
  if (isPanning) {
    canvasOffset.x = e.clientX - panStartX
    canvasOffset.y = e.clientY - panStartY
  }
}

const onCanvasWrapperMouseUp = () => {
  isPanning = false
}

const onCanvasWheel = (e) => {
  e.preventDefault()
  e.stopPropagation()

  if (e.ctrlKey) {
    // CTRL + 滚轮：缩放，以鼠标位置为中心
    const rect = e.currentTarget.getBoundingClientRect()
    const mouseX = e.clientX - rect.left
    const mouseY = e.clientY - rect.top

    const worldX = (mouseX - canvasOffset.x) / zoom.value
    const worldY = (mouseY - canvasOffset.y) / zoom.value

    const delta = e.deltaY > 0 ? 0.9 : 1.1
    zoom.value = Math.max(0.25, Math.min(3, zoom.value * delta))

    canvasOffset.x = mouseX - worldX * zoom.value
    canvasOffset.y = mouseY - worldY * zoom.value
  } else {
    // 普通滚轮：平滑平移
    // Shift + 滚轮 → 强制左右平移
    // 否则以 delta 实际方向为准（触控板支持双轴）
    if (e.shiftKey) {
      canvasOffset.x -= e.deltaY * 1.5
    } else {
      canvasOffset.x -= (e.deltaX || 0) * 1.5
      canvasOffset.y -= (e.deltaY || 0) * 1.5
    }
  }
}




// 获取画布容器引用
const canvasWrapper = ref(null)

const onCanvasMouseDown = (e) => {
  if (activeTool.value === 'brush') {
    isDrawing = true
    const wrapper = e.currentTarget.closest('.canvas-wrapper')
    const rect = wrapper.getBoundingClientRect()
    const x = (e.clientX - rect.left - canvasOffset.value.x) / zoom.value
    const y = (e.clientY - rect.top - canvasOffset.value.y) / zoom.value
    
    // 画笔功能暂不使用 canvas
    console.log('画笔起点:', x, y)
  }
}

const onCanvasMouseMove = (e) => {
  if (activeTool.value === 'brush' && isDrawing) {
    const wrapper = e.currentTarget.closest('.canvas-wrapper')
    const rect = wrapper.getBoundingClientRect()
    const x = (e.clientX - rect.left - canvasOffset.value.x) / zoom.value
    const y = (e.clientY - rect.top - canvasOffset.value.y) / zoom.value
    
    console.log('画笔移动:', x, y)
  }
}

const onCanvasMouseUp = () => {
  isDrawing = false
}

const onCanvasClick = (e) => {
  // 点击到画布元素上不取消选中
  if (e.target.closest('.canvas-element')) return

  if (activeTool.value === 'select') {
    selectedElementId.value = null
  } else if (activeTool.value === 'text') {
    // 文字工具：点击画布添加文字元素
    const wrapper = e.currentTarget.closest('.canvas-wrapper')
    const rect = wrapper.getBoundingClientRect()
    const x = (e.clientX - rect.left - canvasOffset.value.x) / zoom.value
    const y = (e.clientY - rect.top - canvasOffset.value.y) / zoom.value

    const newText = {
      id: Date.now(),
      type: 'text',
      content: textSettings.content || '双击编辑文字',
      x: x,
      y: y,
      width: 200,
      height: textSettings.size + 10,
      rotation: 0,
      opacity: 100
    }

    canvasElements.value.push(newText)
    selectedElementId.value = newText.id
    activeTool.value = 'select'
    ElMessage.info('已添加文字，双击可编辑')
  }
}

const startResize = (e, element, handle) => {
  if (activeTool.value !== 'select') return

  const startX = element.x
  const startY = element.y
  const startW = element.width
  const startH = element.height
  const startMouseX = e.clientX
  const startMouseY = e.clientY

  const onMouseMove = (e) => {
    const dx = (e.clientX - startMouseX) / zoom.value
    const dy = (e.clientY - startMouseY) / zoom.value

    if (handle.includes('right')) {
      element.width = Math.max(100, startW + dx)
    }
    if (handle.includes('bottom')) {
      element.height = Math.max(80, startH + dy)
    }
    if (handle.includes('left')) {
      element.width = Math.max(100, startW - dx)
      element.x = startX + (startW - element.width)
    }
    if (handle.includes('top')) {
      element.height = Math.max(80, startH - dy)
      element.y = startY + (startH - element.height)
    }
  }

  const onMouseUp = () => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

const onTextareaFocus = (e) => {
  console.log('[onTextareaFocus] 输入框获得焦点:', e.target)
  e.target.style.outline = '2px solid #6366f1'
}

const onDragBarMouseDown = (e, element) => {
  if (activeTool.value !== 'select') return

  selectedElementId.value = element.id
  isDragging = true

  // 记录鼠标相对元素左上角的偏移量（屏幕像素）
  const elementScreenX = element.x * zoom.value + canvasOffset.x
  const elementScreenY = element.y * zoom.value + canvasOffset.y
  const offsetX = e.clientX - elementScreenX
  const offsetY = e.clientY - elementScreenY

  const onMouseMove = (e) => {
    if (!isDragging || !selectedElement.value) return

    // 鼠标位置减去偏移量 → 元素新位置的屏幕坐标 → 转世界坐标
    const newScreenX = e.clientX - offsetX
    const newScreenY = e.clientY - offsetY
    selectedElement.value.x = (newScreenX - canvasOffset.x) / zoom.value
    selectedElement.value.y = (newScreenY - canvasOffset.y) / zoom.value

    connections.value.forEach(conn => {
      const fromEl = conn.fromId === selectedElement.value.id ? selectedElement.value
        : canvasElements.value.find(e => e.id === conn.fromId)
      const toEl = conn.toId === selectedElement.value.id ? selectedElement.value
        : canvasElements.value.find(e => e.id === conn.toId)
      if (fromEl && toEl) {
        const p1 = getEdgePoint(fromEl, conn.fromPosition || 'right')
        const p2 = getEdgePoint(toEl, conn.toPosition || 'left')
        const s1 = worldToScreen(p1.x, p1.y)
        const s2 = worldToScreen(p2.x, p2.y)
        conn.x1 = s1.x
        conn.y1 = s1.y
        conn.x2 = s2.x
        conn.y2 = s2.y
      }
    })
  }

  const onMouseUp = () => {
    isDragging = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

const onElementMouseDown = (e, element) => {
  if (activeTool.value !== 'select') return

  const target = e.target
  const interactiveTags = ['INPUT', 'TEXTAREA', 'BUTTON', 'SELECT', 'OPTION']

  // 点击交互元素（输入框、按钮等）只选中，不拖拽
  if (interactiveTags.includes(target.tagName)) {
    selectedElementId.value = element.id
    return
  }

  // 点击拖拽条由 onDragBarMouseDown 处理
  if (target.closest('.generate-box-drag-bar')) {
    return
  }

  selectedElementId.value = element.id

  // 开始拖拽——使用鼠标偏移量，确保元素不跳变
  isDragging = true
  const elementScreenX = element.x * zoom.value + canvasOffset.x
  const elementScreenY = element.y * zoom.value + canvasOffset.y
  const offsetX = e.clientX - elementScreenX
  const offsetY = e.clientY - elementScreenY

  const onMouseMove = (e) => {
    if (!isDragging || !selectedElement.value) return

    const newScreenX = e.clientX - offsetX
    const newScreenY = e.clientY - offsetY
    selectedElement.value.x = (newScreenX - canvasOffset.x) / zoom.value
    selectedElement.value.y = (newScreenY - canvasOffset.y) / zoom.value

    connections.value.forEach(conn => {
      const fromEl = conn.fromId === selectedElement.value.id ? selectedElement.value
        : canvasElements.value.find(e => e.id === conn.fromId)
      const toEl = conn.toId === selectedElement.value.id ? selectedElement.value
        : canvasElements.value.find(e => e.id === conn.toId)
      if (fromEl && toEl) {
        const p1 = getEdgePoint(fromEl, conn.fromPosition || 'right')
        const p2 = getEdgePoint(toEl, conn.toPosition || 'left')
        const s1 = worldToScreen(p1.x, p1.y)
        const s2 = worldToScreen(p2.x, p2.y)
        conn.x1 = s1.x
        conn.y1 = s1.y
        conn.x2 = s2.x
        conn.y2 = s2.y
      }
    })
  }

  const onMouseUp = () => {
    isDragging = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

const onElementDoubleClick = (element) => {
  if (element.type === 'generate') {
    selectedElementId.value = element.id
  }
}

const onTextBlur = (event, element) => {
  element.content = event.target.innerText || '双击编辑文字'
}

// 平移/缩放时更新所有连线的屏幕坐标
const updateAllConnectionPositions = () => {
  connections.value.forEach(conn => {
    const fromEl = canvasElements.value.find(e => e.id === conn.fromId)
    const toEl = canvasElements.value.find(e => e.id === conn.toId)
    if (fromEl && toEl) {
      const p1 = getEdgePoint(fromEl, conn.fromPosition || 'right')
      const p2 = getEdgePoint(toEl, conn.toPosition || 'left')
      const s1 = worldToScreen(p1.x, p1.y)
      const s2 = worldToScreen(p2.x, p2.y)
      conn.x1 = s1.x
      conn.y1 = s1.y
      conn.x2 = s2.x
      conn.y2 = s2.y
    }
  })
}

watch([zoom, () => canvasOffset.x, () => canvasOffset.y], updateAllConnectionPositions, { deep: true })

onMounted(() => {
  console.log('无限画布系统已初始化')
})
</script>

<style scoped>
.editor-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-dark);
}

.editor-container > .el-container {
  flex: 1;
  min-height: 0;
}

.editor-container > .el-container > .el-container {
  flex: 1;
  min-width: 0;
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

.nav-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.project-name {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-family: var(--font-heading);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: white;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: background var(--transition-base);
}

.user-profile:hover {
  background: rgba(255, 255, 255, 0.1);
}

.icon {
  width: 20px;
  height: 20px;
}

/* ===== Sidebar ===== */
.sidebar {
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.tool-panel {
  padding: 16px;
}

.tool-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-btn {
  width: 100% !important;
  height: 48px !important;
  border-radius: var(--radius-md) !important;
  border: 1px solid var(--border-light) !important;
  background: var(--bg-card-hover) !important;
  color: var(--text-primary) !important;
  font-size: 14px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: flex-start !important;
  gap: 10px !important;
  padding: 0 16px !important;
  transition: all var(--transition-base) !important;
}

.sidebar-btn:hover {
  border-color: rgba(99, 102, 241, 0.4) !important;
  background: var(--bg-elevated) !important;
  transform: translateY(-1px);
}

.sidebar-btn.primary-btn {
  background: var(--gradient-primary) !important;
  border: none !important;
  color: white !important;
  font-weight: 600 !important;
}

.sidebar-btn.primary-btn:hover {
  box-shadow: var(--shadow-glow) !important;
}

.btn-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

/* ===== Library Panel ===== */
.library-panel {
  padding: 0 16px 16px;
  border-bottom: 1px solid var(--border-color);
}

.library-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.library-header h3 {
  font-size: 13px;
  color: var(--text-secondary);
  font-family: var(--font-heading);
  font-weight: 600;
}

.library-count {
  font-size: 11px;
  color: var(--text-muted);
  background: var(--bg-card-hover);
  padding: 2px 8px;
  border-radius: 10px;
}

.library-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 280px;
  overflow-y: auto;
}

.library-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all var(--transition-fast);
  background: rgba(255, 255, 255, 0.02);
}

.library-item:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-color);
}

.library-item.active {
  border-color: var(--primary-color);
  background: rgba(99, 102, 241, 0.1);
}

.library-item img {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.library-item-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-width: 0;
}

.library-item-name {
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.library-item-delete {
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--transition-fast);
}

.library-item-delete:hover {
  background: rgba(239, 68, 68, 0.7);
  color: white;
}

.library-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 24px 16px;
  color: var(--text-muted);
  font-size: 13px;
}

.library-empty .empty-icon {
  width: 36px;
  height: 36px;
  opacity: 0.3;
  margin-bottom: 4px;
}

.empty-hint {
  font-size: 11px;
  text-align: center;
  line-height: 1.5;
  opacity: 0.6;
}

.connection-panel {
  padding: 16px;
  border-top: 1px solid var(--border-color);
}

.connection-panel h3 {
  margin-bottom: 12px;
  font-size: 14px;
  color: var(--text-secondary);
  font-family: var(--font-heading);
}

.connection-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.connection-info p {
  font-size: 13px;
  color: var(--text-secondary);
}

/* ===== Canvas ===== */
.canvas-area {
  flex: 1;
  overflow: hidden !important;
  background: #0a0a1a;
  position: relative;
  height: 100%;
}

.canvas-wrapper {
  width: 100%;
  height: 100%;
  overflow: hidden;
  cursor: grab;
  position: relative;
  touch-action: none;
  overscroll-behavior: none;
}

.canvas-wrapper:active {
  cursor: grabbing;
}

.connections-svg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 30;
  overflow: visible;
}

.connection-line {
  pointer-events: stroke;
  cursor: pointer;
}

.connection-line:hover {
  stroke: #8b5cf6;
}

.temp-connection-line {
  pointer-events: none;
  opacity: 0.8;
}

.canvas-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 2;
}

.canvas-grid {
  position: absolute;
  top: -25000px;
  left: -25000px;
  width: 50000px;
  height: 50000px;
  background-image:
    linear-gradient(rgba(255,255,255,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.04) 1px, transparent 1px);
  background-size: 20px 20px;
  pointer-events: none;
}

/* ===== Canvas Elements ===== */
.canvas-element {
  position: absolute;
  cursor: default;
  transition: box-shadow 0.2s;
  z-index: 10;
}

.canvas-element.dragging {
  cursor: grabbing;
}

.canvas-element.selected {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}

.canvas-element.generate-box {
  background: rgba(99, 102, 241, 0.15);
  border: 2px solid var(--primary-color);
  border-radius: var(--radius-sm);
  box-shadow: 0 0 20px rgba(99, 102, 241, 0.3);
}

.canvas-element.box-element {
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.3);
  border-radius: var(--radius-sm);
}

.generate-box-inner {
  width: calc(100% - 8px);
  height: calc(100% - 8px);
  margin: 4px;
  display: flex;
  flex-direction: column;
  background: rgba(99, 102, 241, 0.05);
  border-radius: 6px;
}

.generate-box-drag-bar {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  background: rgba(99, 102, 241, 0.2);
  cursor: grab;
  border-bottom: 1px solid rgba(99, 102, 241, 0.3);
  border-radius: 6px 6px 0 0;
  user-select: none;
}

.generate-box-drag-bar:hover {
  background: rgba(99, 102, 241, 0.3);
}

.generate-box-drag-bar:active {
  cursor: grabbing;
}

.drag-handle {
  margin-right: 8px;
  color: var(--primary-color);
  font-size: 12px;
}

.box-title {
  font-size: 12px;
  color: var(--text-secondary);
  font-family: var(--font-heading);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.box-delete {
  width: 24px;
  height: 24px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.5);
  border-radius: 6px;
  font-size: 16px;
  font-weight: 700;
  line-height: 24px;
  text-align: center;
  cursor: pointer;
  flex-shrink: 0;
  margin-left: 4px;
  transition: all var(--transition-fast);
}

.box-delete:hover {
  background: rgba(239, 68, 68, 0.85);
  color: white;
}

.generate-box-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.generate-box-image-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  min-height: 50px;
  cursor: pointer;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  border: 2px dashed #374151;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  pointer-events: auto;
  z-index: 20;
  transition: border-color var(--transition-fast);
}

.image-placeholder:hover {
  border-color: var(--primary-color);
}

.placeholder-icon {
  width: 36px;
  height: 36px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.image-placeholder span {
  color: var(--text-muted);
  font-size: 11px;
}

.reference-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  pointer-events: auto;
  z-index: 20;
}

/* ===== Image Gallery ===== */
.image-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 6px;
  width: 100%;
  min-height: 50px;
  align-content: flex-start;
}

.gallery-thumb {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
  transition: border-color var(--transition-fast);
}

.gallery-thumb:hover {
  border-color: #ef4444;
}

.gallery-thumb:hover .thumb-remove {
  opacity: 1;
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-remove {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 20px;
  height: 20px;
  background: #ef4444;
  color: white;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
  opacity: 0;
  cursor: pointer;
  transition: opacity var(--transition-fast);
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
}

.gallery-add {
  width: 48px;
  height: 48px;
  border: 2px dashed var(--border-color);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: border-color var(--transition-fast);
}

.gallery-add:hover {
  border-color: var(--primary-color);
}

.gallery-add span {
  color: var(--text-muted);
  font-size: 20px;
  font-weight: 300;
  line-height: 1;
}

.generate-box-divider {
  height: 1px;
  background: rgba(99, 102, 241, 0.2);
  margin: 0 8px;
}

.generate-box-prompt-section {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.prompt-input {
  flex: 1;
  min-height: 50px;
  max-height: 80px;
  background: var(--bg-card-hover);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 6px 8px;
  color: white;
  font-size: 12px;
  resize: none;
  z-index: 20;
  position: relative;
  transition: border-color var(--transition-fast);
}

.prompt-input:focus {
  outline: none;
  border-color: var(--primary-color);
}

.prompt-input::placeholder {
  color: var(--text-muted);
}

/* ===== Type Tabs ===== */
.generate-box-type {
  display: flex;
  gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid rgba(99, 102, 241, 0.15);
}

.type-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.type-tab:hover {
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.04);
}

.type-tab.active {
  color: var(--primary-light);
  background: rgba(99, 102, 241, 0.15);
}

.type-icon {
  width: 14px;
  height: 14px;
}

/* ===== Video Result ===== */
.video-result-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
}

.video-result {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 4px;
  background: #000;
}

.generate-box-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  pointer-events: auto;
  gap: 6px;
  padding: 6px 8px;
}

.resolution-select {
  width: 110px;
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.image-wrapper:hover .image-delete-btn {
  opacity: 1;
}

.element-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.image-delete-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 28px;
  height: 28px;
  background: rgba(239, 68, 68, 0.9);
  color: white;
  border-radius: 50%;
  font-size: 16px;
  font-weight: 700;
  line-height: 28px;
  text-align: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity var(--transition-fast);
  z-index: 25;
  backdrop-filter: blur(4px);
}

.image-delete-btn:hover {
  background: #ef4444;
  transform: scale(1.15);
}

.text-element-content {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-height: 40px;
}

.element-text {
  color: #fff;
  outline: none;
  text-align: center;
  padding: 4px;
}

.box-element-content {
  width: 100%;
  height: 100%;
}

.box-border {
  width: 100%;
  height: 100%;
}

/* ===== Resize Handles ===== */
.element-resize-handles {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.handle {
  position: absolute;
  width: 10px;
  height: 10px;
  background: var(--primary-color);
  border-radius: 50%;
  pointer-events: auto;
  border: 2px solid white;
}

.handle.top-left {
  top: -5px;
  left: -5px;
  cursor: nwse-resize;
}

.handle.top-right {
  top: -5px;
  right: -5px;
  cursor: nesw-resize;
}

.handle.bottom-left {
  bottom: -5px;
  left: -5px;
  cursor: nesw-resize;
}

.handle.bottom-right {
  bottom: -5px;
  right: -5px;
  cursor: nwse-resize;
}

/* ===== Connection Points ===== */
.connection-points {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.connection-point {
  position: absolute;
  width: 12px;
  height: 12px;
  background: var(--primary-color);
  border: 2px solid white;
  border-radius: 50%;
  pointer-events: auto;
  cursor: crosshair;
  transition: transform var(--transition-fast);
}

.connection-point:hover {
  transform: scale(1.3);
}

.connection-point.top {
  top: -6px;
  left: 50%;
  transform: translateX(-50%);
}

.connection-point.top:hover {
  transform: translateX(-50%) scale(1.3);
}

.connection-point.bottom {
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
}

.connection-point.bottom:hover {
  transform: translateX(-50%) scale(1.3);
}

.connection-point.left {
  left: -6px;
  top: 50%;
  transform: translateY(-50%);
}

.connection-point.left:hover {
  transform: translateY(-50%) scale(1.3);
}

.connection-point.right {
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
}

.connection-point.right:hover {
  transform: translateY(-50%) scale(1.3);
}

/* ===== Canvas Controls ===== */
.canvas-controls {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(26, 26, 46, 0.92);
  backdrop-filter: blur(12px);
  padding: 8px 16px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-lg);
  z-index: 50;
}

.zoom-value {
  font-size: 13px;
  color: var(--text-secondary);
  min-width: 50px;
  text-align: center;
  font-family: var(--font-heading);
}

/* ===== Properties Panel ===== */
.properties-panel {
  background: var(--bg-card);
  border-left: 1px solid var(--border-color);
  padding: 16px;
  overflow-y: auto;
}

.properties-panel h3 {
  margin-bottom: 16px;
  font-size: 14px;
  color: var(--text-secondary);
  font-family: var(--font-heading);
  font-weight: 600;
}

.tool-properties,
.element-properties {
  background: var(--bg-card-hover);
  padding: 16px;
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
  border: 1px solid var(--border-light);
}

.tool-properties h4,
.element-properties h4 {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-primary);
  font-family: var(--font-heading);
}

.connection-tips {
  background: rgba(99, 102, 241, 0.1);
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
  border: 1px solid rgba(99, 102, 241, 0.15);
}

.connection-tips h4 {
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--primary-color);
  font-family: var(--font-heading);
}

.connection-tips p {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
}

/* ===== Minimap ===== */
.minimap {
  background: var(--bg-card-hover);
  padding: 16px;
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
  border: 1px solid var(--border-light);
}

.minimap h4 {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-secondary);
  font-family: var(--font-heading);
}

.minimap-canvas {
  position: relative;
  width: 100%;
  height: 120px;
  background: var(--bg-card);
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border-light);
}

.minimap-element {
  position: absolute;
  border-radius: 2px;
  opacity: 0.7;
}

.minimap-viewport {
  position: absolute;
  border: 2px solid var(--primary-color);
  border-radius: 2px;
  background: rgba(99, 102, 241, 0.1);
  pointer-events: none;
}

.project-info {
  background: var(--bg-card-hover);
  padding: 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-light);
}

.project-info h4 {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-secondary);
  font-family: var(--font-heading);
}

.project-info p {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

/* ===== Upload Dialog ===== */
.upload-area {
  border: 2px dashed var(--border-color);
  border-radius: var(--radius-sm);
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: border-color var(--transition-base);
}

.upload-area:hover {
  border-color: var(--primary-color);
}

.upload-icon {
  width: 48px;
  height: 48px;
  color: var(--text-muted);
  margin-bottom: 12px;
}

#imageUploadInput {
  display: none;
}

.preview-area {
  margin-top: 16px;
}

.preview-image {
  width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: var(--radius-sm);
}

.preview-info {
  text-align: center;
  margin-top: 12px;
  color: var(--text-secondary);
  font-size: 13px;
}
</style>