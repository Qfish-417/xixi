import axios from 'axios'

const baseURL = '/api'

export const agentAPI = {
  master: (data) => axios.post(`${baseURL}/agent/master`, data),
  analyze: (data) => axios.post(`${baseURL}/agent/analyze`, data),
  execute: (data) => axios.post(`${baseURL}/agent/execute`, data),
  xixi: (data) => axios.post(`${baseURL}/agent/xixi`, data),
  tools: () => axios.get(`${baseURL}/agent/tools`),
  models: () => axios.get(`${baseURL}/agent/models`),
  config: () => axios.get(`${baseURL}/agent/config`),
  updateConfig: (data) => axios.put(`${baseURL}/agent/config`, data)
}

export const libraryAPI = {
  materials: (params) => axios.get(`${baseURL}/library/materials`, { params }),
  createMaterial: (data) => axios.post(`${baseURL}/library/materials`, data),
  getMaterial: (id) => axios.get(`${baseURL}/library/materials/${id}`),
  updateMaterial: (id, data) => axios.put(`${baseURL}/library/materials/${id}`, data),
  deleteMaterial: (id) => axios.delete(`${baseURL}/library/materials/${id}`),
  
  groups: (params) => axios.get(`${baseURL}/library/groups`, { params }),
  createGroup: (data) => axios.post(`${baseURL}/library/groups`, data),
  getGroup: (id) => axios.get(`${baseURL}/library/groups/${id}`),
  updateGroup: (id, data) => axios.put(`${baseURL}/library/groups/${id}`, data),
  deleteGroup: (id) => axios.delete(`${baseURL}/library/groups/${id}`)
}
