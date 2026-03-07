import { ref } from 'vue'

const KEY = 'md_collab_auth'

// 未登录时的默认状态
const emptyAuth = { token: '', userId: null, displayName: '', expiresAt: null }

// 从 localStorage 读取登录信息
const readAuth = () => {
  const raw = localStorage.getItem(KEY)
  if (!raw) {
    return { ...emptyAuth }
  }
  try {
    return JSON.parse(raw)
  } catch {
    return { ...emptyAuth }
  }
}

// 响应式登录状态
export const authState = ref(readAuth())

// 获取当前登录信息
export function getAuth() {
  return authState.value
}

// 保存登录信息
export function setAuth(payload) {
  localStorage.setItem(KEY, JSON.stringify(payload))
  authState.value = payload
}

// 清理登录信息
export function clearAuth() {
  localStorage.removeItem(KEY)
  authState.value = { ...emptyAuth }
}
