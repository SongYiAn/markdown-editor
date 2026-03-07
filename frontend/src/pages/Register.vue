<template>
  <div class="panel">
    <h2>注册</h2>
    <form class="form" @submit.prevent="submit">
      <label class="field">
        <span>用户名</span>
        <input v-model.trim="form.username" required />
      </label>
      <label class="field">
        <span>显示名</span>
        <input v-model.trim="form.displayName" required />
      </label>
      <label class="field">
        <span>密码</span>
        <input v-model="form.password" type="password" required />
      </label>
      <button class="primary" :disabled="loading">注册</button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
    <p class="tip">已有账号？<RouterLink to="/login">去登录</RouterLink></p>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/auth'
import { setAuth } from '../utils/storage'

// 注册表单状态
const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  username: '',
  displayName: '',
  password: '',
})

// 提交注册
const submit = async () => {
  error.value = ''
  loading.value = true
  try {
    const data = await register(form)
    setAuth(data)
    router.push('/documents')
  } catch (err) {
    const apiError = err?.response?.data
    error.value = apiError?.details || apiError?.message || '注册失败'
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

