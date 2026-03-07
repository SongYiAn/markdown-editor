<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { authState, clearAuth } from './utils/storage'

// 顶部导航依赖登录状态
const router = useRouter()
const isAuthed = computed(() => Boolean(authState.value.token))

// 退出登录
const logout = () => {
  clearAuth()
  router.push('/login')
}
</script>

<template>
  <div class="app">
    <header class="topbar">
      <div class="brand">Markdown 协同编辑</div>
      <nav class="nav">
        <RouterLink v-if="isAuthed" to="/documents">文档</RouterLink>
        <RouterLink v-if="!isAuthed" to="/login">登录</RouterLink>
        <RouterLink v-if="!isAuthed" to="/register">注册</RouterLink>
        <button v-if="isAuthed" class="link" @click="logout">退出</button>
      </nav>
    </header>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>
