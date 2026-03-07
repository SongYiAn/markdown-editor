import client from './client'

// 登录
export async function login(payload) {
  const { data } = await client.post('/api/auth/login', payload)
  return data
}

// 注册
export async function register(payload) {
  const { data } = await client.post('/api/auth/register', payload)
  return data
}
