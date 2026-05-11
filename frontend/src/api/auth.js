import axios from 'axios'

const baseURL = '/api'

export const authAPI = {
  login: (data) => axios.post(`${baseURL}/auth/login`, data),
  register: (data) => axios.post(`${baseURL}/auth/register`, data),
  logout: () => axios.post(`${baseURL}/auth/logout`),
  getCurrentUser: () => axios.get(`${baseURL}/auth/me`)
}

axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
