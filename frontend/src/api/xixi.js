import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const xixiAPI = {
  // 启动嘻嘻模式
  startSession(prompt, style, modelName) {
    return axios.post(`${API_BASE_URL}/xixi/start`, {
      prompt,
      style,
      modelName
    })
  },

  // 确认剧本
  confirmScript(sessionId, response, modelName) {
    return axios.post(`${API_BASE_URL}/xixi/confirm-script`, {
      sessionId,
      response,
      modelName
    })
  },

  // 确认视频流程
  confirmVideoFlow(sessionId, response, modelName) {
    return axios.post(`${API_BASE_URL}/xixi/confirm-video-flow`, {
      sessionId,
      response,
      modelName
    })
  },

  // 获取会话状态
  getSession(sessionId) {
    return axios.get(`${API_BASE_URL}/xixi/session/${sessionId}`)
  }
}

export default xixiAPI
