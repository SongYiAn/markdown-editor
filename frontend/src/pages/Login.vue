<template>
  <div class="panel">
    <h2>登录</h2>
    <form class="form" @submit.prevent="submit">
      <label class="field">
        <span>用户名</span>
        <input v-model.trim="form.username" required />
      </label>
      <label class="field">
        <span>密码</span>
        <input v-model="form.password" type="password" required />
      </label>
      <button class="primary" :disabled="loading">登录</button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
    <p class="tip">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'
import { setAuth } from '../utils/storage'

// 登录表单状态
const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  username: '',
  password: '',
})

// 提交登录
const submit = async () => {
  error.value = ''
  loading.value = true
  try {
    const data = await login(form)
    setAuth(data)
    router.push('/documents')
  } catch (err) {
    error.value = err?.response?.data?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tip {
  margin-top: 12px;
  font-size: 13px;
  color: #6b7280;
  text-align: center;
}
</style>

