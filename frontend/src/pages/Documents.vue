<template>
  <div class="docs-page">
    <div class="docs-header">
      <div class="docs-title-row">
        <h2>我的文档</h2>
        <span class="doc-count">{{ filteredDocs.length }} 篇</span>
      </div>
      <form class="create-form" @submit.prevent="create">
        <input v-model.trim="title" placeholder="输入新文档标题..." class="create-input" required />
        <button class="primary" :disabled="creating">
          <span>＋ 新建文档</span>
        </button>
      </form>
    </div>

    <div class="search-bar">
      <input v-model.trim="search" placeholder="🔍 搜索文档..." class="search-input" />
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="loading" class="loading-state">
      <p class="muted">加载中...</p>
    </div>

    <div v-else-if="filteredDocs.length === 0" class="empty-state">
      <div class="empty-icon">📄</div>
      <p v-if="search">没有找到包含「{{ search }}」的文档</p>
      <p v-else>还没有文档，新建一个开始吧！</p>
    </div>

    <div v-else class="doc-grid">
      <div v-for="doc in filteredDocs" :key="doc.id" class="doc-card">
        <div class="doc-card-body">
          <div class="doc-card-top">
            <span :class="['role-badge', `role-${doc.role.toLowerCase()}`]">{{ roleLabel(doc.role) }}</span>
          </div>
          <div class="doc-title">{{ doc.title }}</div>
          <div class="doc-meta">
            <span title="最后更新时间">🕐 {{ formatRelativeTime(doc.updatedAt) }}</span>
          </div>
        </div>
        <div class="doc-card-footer">
          <RouterLink class="btn-open" :to="`/documents/${doc.id}`">打开编辑</RouterLink>
          <button v-if="doc.role === 'OWNER'" class="btn-delete" @click="remove(doc)" title="删除文档">🗑</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { createDocument, deleteDocument, listDocuments } from '../api/documents'

// 文档列表数据
const docs = ref([])
// 新文档标题
const title = ref('')
const search = ref('')
const error = ref('')
const creating = ref(false)
const loading = ref(false)

// 搜索过滤
const filteredDocs = computed(() => {
  if (!search.value) return docs.value
  const kw = search.value.toLowerCase()
  return docs.value.filter(d => d.title.toLowerCase().includes(kw))
})

// 角色显示名
const roleLabel = (role) => {
  if (role === 'OWNER') return '创建者'
  if (role === 'EDITOR') return '编辑者'
  if (role === 'VIEWER') return '只读'
  return role
}

// 加载列表
const load = async () => {
  loading.value = true
  error.value = ''
  try {
    docs.value = await listDocuments()
  } catch (err) {
    error.value = err?.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

// 删除文档
const remove = async (doc) => {
  if (!confirm(`确定删除文档「${doc.title}」吗？删除后不可恢复。`)) return
  try {
    await deleteDocument(doc.id)
    await load()
  } catch (err) {
    error.value = err?.response?.data?.message || '删除失败'
  }
}

// 创建文档并刷新列表
const create = async () => {
  if (!title.value) return
  creating.value = true
  try {
    await createDocument({ title: title.value })
    title.value = ''
    await load()
  } catch (err) {
    error.value = err?.response?.data?.message || '创建失败'
  } finally {
    creating.value = false
  }
}

// 相对时间格式化
const formatRelativeTime = (value) => {
  if (!value) return ''
  const now = Date.now()
  const ts = new Date(value).getTime()
  const diff = now - ts
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / 86400000)} 天前`
  return new Date(value).toLocaleDateString()
}

onMounted(load)
</script>

<style scoped>
.docs-page {
  max-width: 960px;
  margin: 0 auto;
}

.docs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.docs-title-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.docs-title-row h2 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.doc-count {
  font-size: 13px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 999px;
}

.create-form {
  display: flex;
  gap: 8px;
}

.create-input {
  width: 240px;
  padding: 9px 14px;
  border-radius: 8px;
  border: 1px solid #d0d7de;
  font-size: 14px;
}

.search-bar {
  margin-bottom: 20px;
}

.search-input {
  width: 100%;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  font-size: 14px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.search-input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37,99,235,0.1);
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.doc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.doc-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(15,23,42,0.06);
  display: flex;
  flex-direction: column;
  transition: box-shadow 200ms, transform 200ms;
  overflow: hidden;
}

.doc-card:hover {
  box-shadow: 0 6px 20px rgba(15,23,42,0.12);
  transform: translateY(-2px);
}

.doc-card-body {
  padding: 16px 16px 10px;
  flex: 1;
}

.doc-card-top {
  margin-bottom: 8px;
}

.role-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
}

.role-owner {
  background: #dbeafe;
  color: #1d4ed8;
}

.role-editor {
  background: #dcfce7;
  color: #166534;
}

.role-viewer {
  background: #f1f5f9;
  color: #475569;
}

.doc-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  font-size: 12px;
  color: #94a3b8;
}

.doc-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}

.btn-open {
  display: inline-block;
  background: #2563eb;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 7px;
  text-decoration: none;
  transition: background 150ms;
}

.btn-open:hover {
  background: #1d4ed8;
  text-decoration: none;
}

.btn-delete {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #dc2626;
  padding: 4px 6px;
  border-radius: 6px;
  transition: background 150ms;
}

.btn-delete:hover {
  background: #fee2e2;
}
</style>
