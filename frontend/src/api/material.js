import axios from 'axios'
import '../api/auth.js'

const API_BASE_URL = '/api'

export const materialAPI = {
  getMaterials: (params = {}) => {
    const queryString = new URLSearchParams(params).toString()
    return axios.get(`${API_BASE_URL}/materials${queryString ? '?' + queryString : ''}`)
  },

  getMaterial: (id) => {
    return axios.get(`${API_BASE_URL}/materials/${id}`)
  },

  deleteMaterial: (id) => {
    return axios.delete(`${API_BASE_URL}/materials/${id}`)
  },

  updateMaterial: (id, data) => {
    return axios.put(`${API_BASE_URL}/materials/${id}`, data)
  },

  updateMaterialName: (id, name) => {
    return axios.put(`${API_BASE_URL}/materials/${id}/name`, { name })
  },

  getStats: () => {
    return axios.get(`${API_BASE_URL}/materials/stats`)
  },

  uploadMaterial: (file, type) => {
    const formData = new FormData()
    formData.append('file', file)
    if (type) {
      formData.append('type', type)
    }
    return axios.post(`${API_BASE_URL}/materials/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}