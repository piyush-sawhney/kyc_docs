import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:3000/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.response.use(
  (response) => {
    if (response.data && 'success' in response.data && response.data.success) {
      response.data = response.data.data
    }
    return response
  },
)

export default api
