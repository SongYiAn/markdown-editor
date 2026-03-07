<template>
  <div v-if="noAccess" class="panel">
    <h2>需要加入协作</h2>
    <p class="muted">你当前没有访问该文档的权限。</p>
    <div class="row">
      <button class="primary" @click="joinByLink" :disabled="joining">加入协作</button>
      <button class="ghost" @click="copyLink">复制链接</button>
    </div>
    <p v-if="joinError" class="error">{{ joinError }}</p>
  </div>

  <div v-else class="editor-page">
    <div class="editor-header" ref="headerRef">
      <div class="row" style="align-items:center;">
        <RouterLink to="/documents" class="back-btn" title="返回文档列表">←</RouterLink>
        <div>
        <h2 v-if="!editingTitle" class="editable-title" @click="startEditTitle">{{ doc.title || '文档' }} <span class="edit-icon">✎</span></h2>
        <div v-else class="row title-edit-row">
          <input v-model.trim="titleInput" class="title-input" @keyup.enter="saveTitle" @keyup.escape="cancelEditTitle" ref="titleInputRef" />
          <button class="primary" @click="saveTitle" :disabled="savingTitle">保存</button>
          <button class="ghost" @click="cancelEditTitle">取消</button>
        </div>
        <div class="meta">版本：{{ version }} · 角色：{{ doc.role || '-' }}</div>
        <div class="meta">保存状态：{{ saveStatusText }}</div>
        </div>
      </div>
      <div class="row">
        <span class="badge" :class="connectionClass">{{ connectionText }}</span>
        <button class="ghost" @click="syncNow" :disabled="syncing">同步</button>
        <button class="ghost" :class="{ active: previewOpen }" @click="previewOpen = !previewOpen">预览</button>
        <button class="primary" @click="openSnapshotModal" :disabled="saving">保存</button>
        <button class="ghost" @click="toggleVersionDrawer">历史版本</button>
        <button class="ghost" @click="toggleMemberDrawer">成员</button>
      </div>
    </div>

    <Transition name="notice-slide">
      <p v-if="notice" class="notice">{{ notice }}</p>
    </Transition>
    <p v-if="error" class="error">{{ error }}</p>

    <!-- 编辑区 / 版本预览区 -->
    <div v-if="versionPreviewing" class="version-preview-main">
      <div class="version-preview-bar">
        <span>正在预览：<b>{{ versionPreviewName }}</b></span>
        <div class="row">
          <button class="primary" @click="restoreFromPreview">恢复此版本</button>
          <button class="ghost" @click="exitVersionPreview">退出预览</button>
        </div>
      </div>
      <div class="preview-body version-main-preview" v-html="versionPreviewHtml"></div>
    </div>

    <div v-else class="editor-area">
      <div class="editor-wrapper">
        <!-- Markdown 工具栏 -->
        <div class="md-toolbar" v-if="doc.role !== 'VIEWER' && !versionDrawerOpen">
          <button class="tb-btn" @mousedown.prevent @click="toggleWrap('**')" title="加粗 (Ctrl+B)"><b>B</b></button>
          <button class="tb-btn" @mousedown.prevent @click="toggleWrap('*')" title="斜体 (Ctrl+I)"><i>I</i></button>
          <button class="tb-btn" @mousedown.prevent @click="toggleWrap('~~')" title="删除线"><s>S</s></button>
          <button class="tb-btn" @mousedown.prevent @click="toggleWrap('`')" title="行内代码">&lt;/&gt;</button>
          <span class="tb-sep"></span>
          <button class="tb-btn" @mousedown.prevent @click="setLinePrefix('# ')" title="标题1">H1</button>
          <button class="tb-btn" @mousedown.prevent @click="setLinePrefix('## ')" title="标题2">H2</button>
          <button class="tb-btn" @mousedown.prevent @click="setLinePrefix('### ')" title="标题3">H3</button>
          <span class="tb-sep"></span>
          <button class="tb-btn" @mousedown.prevent @click="setLinePrefix('- ')" title="无序列表">● 列表</button>
          <button class="tb-btn" @mousedown.prevent @click="setLinePrefix('1. ')" title="有序列表">1. 列表</button>
          <button class="tb-btn" @mousedown.prevent @click="setLinePrefix('- [ ] ')" title="任务列表">☑ 待办</button>
          <button class="tb-btn" @mousedown.prevent @click="setLinePrefix('> ')" title="引用">❝ 引用</button>
          <span class="tb-sep"></span>
          <button class="tb-btn" @mousedown.prevent @click="insertBlock('codeblock')" title="代码块">代码块</button>
          <button class="tb-btn" @mousedown.prevent @click="insertBlock('table')" title="插入表格">▦ 表格</button>
          <button class="tb-btn" @mousedown.prevent @click="insertBlock('hr')" title="分割线">── 分割</button>
        </div>
        <!-- CodeMirror 编辑器容器 -->
        <div ref="editorRef" class="cm-host"></div>
        <!-- 底部状态栏：字数统计 -->
        <div class="editor-statusbar">
          <span>字符：{{ charCount }}</span>
          <span>字数：{{ wordCount }}</span>
          <span>行数：{{ lineCount }}</span>
          <span>当前行：{{ cursorLine }}</span>
        </div>
      </div>

      <!-- Markdown 实时预览面板（右侧） -->
      <Transition name="preview-right">
        <div v-if="previewOpen" class="preview-panel-right">
          <div class="preview-right-header">
            <span class="preview-title">Markdown 预览</span>
            <button class="drawer-close" style="position:static;" @click="previewOpen = false">×</button>
          </div>
          <div class="preview-body preview-right-body" v-html="preview"></div>
        </div>
      </Transition>
    </div>

    <!-- 历史版本抽屉 -->
    <Transition name="drawer">
      <aside v-if="versionDrawerOpen" class="drawer" :style="{ top: drawerTop + 'px' }">
        <button class="drawer-close" @click="closeVersionDrawer">×</button>
        <h3>历史版本</h3>
        <!-- Tab 切换 -->
        <div class="version-tabs">
          <button :class="['version-tab', { active: versionTab === 'auto' }]" @click="versionTab = 'auto'">编辑记录</button>
          <button :class="['version-tab', { active: versionTab === 'manual' }]" @click="versionTab = 'manual'">手动保存</button>
        </div>

        <!-- 手动保存 Tab -->
        <div v-if="versionTab === 'manual'">
          <div class="row" style="margin-bottom:12px;">
            <button class="ghost" @click="loadVersions">刷新</button>
          </div>
          <div class="list">
            <div
              v-for="v in manualVersions"
              :key="v.id"
              class="list-item version-item"
              :class="{ active: selectedVersionId === String(v.id) }"
              @click="previewVersion(v)"
            >
              <div>
                <div class="title">{{ v.name || ('版本 ' + v.versionNumber) }}</div>
                <div class="meta">{{ v.createdBy }} · {{ formatTime(v.createdAt) }}</div>
              </div>
              <button class="ghost version-restore-btn" @click.stop="restore(String(v.id))">恢复</button>
            </div>
          </div>
          <p v-if="manualVersions.length === 0" class="muted">暂无手动保存</p>
        </div>

        <!-- 编辑记录 Tab -->
        <div v-if="versionTab === 'auto'">
          <div class="row" style="margin-bottom:12px;">
            <button class="ghost" @click="loadVersions">刷新</button>
          </div>
          <div class="list">
            <div
              v-for="(v, index) in autoVersions"
              :key="v.id"
              class="list-item version-item"
              :class="{ active: selectedVersionId === String(v.id) }"
              @click="previewVersion(v)"
            >
              <div>
                <div class="title">
                  版本 {{ v.versionNumber }}
                  <span v-if="index === 0" class="current-badge">当前</span>
                </div>
                <div class="meta">{{ v.createdBy }} · {{ formatTime(v.createdAt) }}</div>
              </div>
              <button class="ghost version-restore-btn" @click.stop="restore(String(v.id))">恢复</button>
            </div>
          </div>
          <p v-if="autoVersions.length === 0" class="muted">暂无���辑记录</p>
        </div>
      </aside>
    </Transition>

    <!-- 成员管理抽屉 -->
    <Transition name="drawer">
      <aside v-if="memberManagerOpen" class="drawer" :style="{ top: drawerTop + 'px' }">
        <button class="drawer-close" @click="closeMemberManager">×</button>
        <h3>文档成员管理</h3>
        <div class="row">
          <button class="primary" @click="openAddMember">添加成员</button>
          <button class="ghost" @click="copyLink">邀请链接</button>
          <button class="ghost" @click="loadMembers">刷新</button>
        </div>
        <p v-if="memberLoading" class="muted">成员加载中...</p>
        <p v-else-if="memberError" class="error">{{ memberError }}</p>
        <div class="list">
          <div v-for="member in sortedMembers" :key="member.userId" class="list-item member-item">
            <div class="member-info">
              <div class="member-avatar">{{ (member.displayName || member.username || '?')[0].toUpperCase() }}</div>
              <div>
                <div class="title">{{ member.displayName }} <span class="uname">({{ member.username }})</span></div>
                <div class="meta">
                  <span :class="['role-tag', `role-${member.role.toLowerCase()}`]">{{ roleLabel(member.role) }}</span>
                  <span :class="['status-dot', onlineSet[member.userId] ? 'online' : 'offline']"></span>
                  <span class="meta">{{ getStatus(member.userId) }}</span>
                </div>
              </div>
            </div>
            <button
              v-if="doc.role === 'OWNER' && member.role !== 'OWNER'"
              class="btn-remove-member"
              @click="doRemoveMember(member)"
              title="移除成员"
            >×</button>
          </div>
        </div>
        <p v-if="!memberLoading && !memberError && members.length === 0" class="muted">暂无成员</p>
      </aside>
    </Transition>

    <!-- 保存命名弹窗 -->
    <div v-if="snapshotModalOpen" class="modal-mask" @click.self="closeSnapshotModal">
      <div class="modal">
        <div class="modal-header">
          <h3>保存</h3>
          <button class="icon-button" @click="closeSnapshotModal">×</button>
        </div>
        <input v-model.trim="snapshotName" placeholder="请输入保存名称" style="width:100%;margin-bottom:8px;" />
        <p v-if="snapshotError" class="error">{{ snapshotError }}</p>
        <div class="row end">
          <button class="ghost" @click="closeSnapshotModal">取消</button>
          <button class="primary" @click="confirmSnapshot" :disabled="saving">确定</button>
        </div>
      </div>
    </div>

    <!-- 添加成员弹窗 -->
    <div v-if="addMemberOpen" class="modal-mask" @click.self="closeAddMember">
      <div class="modal">
        <div class="modal-header">
          <h3>选择协作人</h3>
          <button class="icon-button" @click="closeAddMember">×</button>
        </div>
        <div class="row">
          <input v-model.trim="inviteForm.username" placeholder="请输入用户名" />
          <select v-model="inviteForm.role">
            <option value="EDITOR">可编辑</option>
            <option value="VIEWER">只查看</option>
          </select>
        </div>
        <p class="muted">当前版本仅支持按用户名邀请。</p>
        <p v-if="inviteError" class="error">{{ inviteError }}</p>
        <div class="row end">
          <button class="ghost" @click="closeAddMember">取消</button>
          <button class="primary" @click="inviteMember" :disabled="inviting">确定</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { marked } from 'marked'
// CodeMirror 6 核心
import { EditorView, keymap, lineNumbers, drawSelection, rectangularSelection, placeholder as cmPlaceholder } from '@codemirror/view'
import { EditorState, Compartment } from '@codemirror/state'
import { markdown, markdownLanguage } from '@codemirror/lang-markdown'
import { languages } from '@codemirror/language-data'
import { defaultKeymap, indentWithTab, history, historyKeymap } from '@codemirror/commands'
import { syntaxHighlighting, defaultHighlightStyle, indentOnInput, bracketMatching, foldGutter, foldKeymap } from '@codemirror/language'
import { searchKeymap, highlightSelectionMatches } from '@codemirror/search'
import { autocompletion, completionKeymap, closeBrackets, closeBracketsKeymap } from '@codemirror/autocomplete'
import {
  addMember,
  applyOperation,
  createSnapshot,
  getDocument,
  getVersionDetail,
  joinDocument,
  listMembers,
  listVersions,
  removeMember,
  restoreVersion,
  updateTitle,
} from '../api/documents'
import { connectStomp } from '../utils/stomp'
import { getAuth } from '../utils/storage'

const props = defineProps({
  id: { type: String, required: true },
})

const doc = ref({})
const content = ref('')
const version = ref(0)
const lastSyncedContent = ref('')
const error = ref('')
const notice = ref('')
const versions = ref([])
const selectedVersionId = ref('')
const saving = ref(false)
const syncing = ref(false)
const saveStatus = ref('idle')
const isOffline = ref(false)
const connection = ref('disconnected')
const noAccess = ref(false)
const joining = ref(false)
const joinError = ref('')
// 抽屉状态
const memberManagerOpen = ref(false)
const versionDrawerOpen = ref(false)
const drawerTop = ref(60)
// 快照弹窗状态
const snapshotModalOpen = ref(false)
const snapshotName = ref('')
const snapshotError = ref('')
// 预览面板
const previewOpen = ref(false)
// 版本内容预览
const versionPreviewContent = ref('')
const versionPreviewName = ref('')
const versionPreviewLoading = ref(false)
// 版本预览模式：点击版本时在编辑区展示
const versionPreviewing = ref(false)
// 版本抽屉 Tab
const versionTab = ref('auto') // 'auto' | 'manual'
// 添加成员弹窗
const addMemberOpen = ref(false)
const memberLoading = ref(false)
const memberError = ref('')
const inviteError = ref('')
const inviting = ref(false)
const members = ref([])
const onlineUsers = ref([])
const cursorMap = ref({})
const inviteForm = ref({ username: '', role: 'EDITOR' })
const editorRef = ref(null)
const headerRef = ref(null)
const titleInputRef = ref(null)
// 标题编辑
const editingTitle = ref(false)
const titleInput = ref('')
const savingTitle = ref(false)
// CodeMirror 实例
let cmView = null
const editableCompartment = new Compartment()
// 字数统计
const charCount = computed(() => content.value.length)
const wordCount = computed(() => {
  const text = content.value.trim()
  if (!text) return 0
  // 中文字符数 + 英文单词数
  const cnChars = (text.match(/[\u4e00-\u9fa5]/g) || []).length
  const enWords = text.replace(/[\u4e00-\u9fa5]/g, ' ').split(/\s+/).filter(Boolean).length
  return cnChars + enWords
})
const lineCount = computed(() => content.value.split('\n').length)
const cursorLine = ref(1)

// 打开历史版本抽屉时禁用编辑器
watch(versionDrawerOpen, (open) => {
  setEditorReadonly(open || doc.value.role === 'VIEWER')
})
const { userId, displayName } = getAuth()
let stompClient = null
let ignoreInput = false
let pendingOpsQueue = []
let inFlight = null
let pendingDirty = false
let subscription = null
let presenceSubscription = null
let noticeTimer = null
let inputTimer = null
let cursorTimer = null
let heartbeatTimer = null
let statusTimer = null
let typingTimer = null
let retryTimer = null
let retryDelay = 1000
const maxRetryDelay = 15000
const onlineSet = ref({})
const activityMap = ref({})
const nowTick = ref(Date.now())
let handleOnline = null
let handleOffline = null

const connectionText = computed(() => {
  if (connection.value === 'connected') return '已连接'
  if (connection.value === 'reconnecting') return '重连中'
  return '未连接'
})
const connectionClass = computed(() => `status-${connection.value}`)
const saveStatusText = computed(() => {
  if (isOffline.value) return '离线，等待重试'
  if (retryTimer) return '重试中'
  if (saveStatus.value === 'saving') return '保存中'
  if (saveStatus.value === 'error') return '保存失败'
  return '已保存'
})

const sortedMembers = computed(() => {
  const list = [...members.value]
  list.sort((a, b) => {
    if (a.role === 'OWNER' && b.role !== 'OWNER') return -1
    if (a.role !== 'OWNER' && b.role === 'OWNER') return 1
    const aOnline = onlineSet.value[a.userId] ? 1 : 0
    const bOnline = onlineSet.value[b.userId] ? 1 : 0
    if (aOnline !== bOnline) return bOnline - aOnline
    return (a.userId || 0) - (b.userId || 0)
  })
  return list
})

// 基于 header 底部计算抽屉 top
const updateDrawerTop = () => {
  if (headerRef.value) {
    drawerTop.value = headerRef.value.getBoundingClientRect().bottom
  }
}

// ========== 抽屉互斥切换 ==========
const toggleMemberDrawer = async () => {
  if (memberManagerOpen.value) {
    memberManagerOpen.value = false
    return
  }
  versionDrawerOpen.value = false
  updateDrawerTop()
  memberManagerOpen.value = true
  inviteError.value = ''
  await loadMembers()
}

const closeMemberManager = () => {
  memberManagerOpen.value = false
}

const toggleVersionDrawer = async () => {
  if (versionDrawerOpen.value) {
    versionDrawerOpen.value = false
    return
  }
  memberManagerOpen.value = false
  updateDrawerTop()
  versionDrawerOpen.value = true
  await loadVersions()
}

const closeVersionDrawer = () => {
  versionDrawerOpen.value = false
  selectedVersionId.value = ''
  versionPreviewContent.value = ''
  versionPreviewName.value = ''
  versionPreviewing.value = false
}

// 版本预览渲染
const versionPreviewHtml = computed(() => marked.parse(versionPreviewContent.value || ''))

// 版本分类
const autoVersions = computed(() => versions.value.filter(v => v.autoSave))
const manualVersions = computed(() => versions.value.filter(v => !v.autoSave))

// 点击版本项 → 在编辑区展示预览
const previewVersion = async (v) => {
  const vid = String(v.id)
  if (selectedVersionId.value === vid && versionPreviewing.value) {
    exitVersionPreview()
    return
  }
  selectedVersionId.value = vid
  versionPreviewName.value = v.name || formatTime(v.createdAt)
  versionPreviewLoading.value = true
  versionPreviewing.value = true
  try {
    const detail = await getVersionDetail(props.id, vid)
    versionPreviewContent.value = detail.content || ''
  } catch (err) {
    versionPreviewContent.value = '加载失败'
  } finally {
    versionPreviewLoading.value = false
  }
}

// 退出版本预览，回到编辑模式
const exitVersionPreview = async () => {
  // 销毁旧 CodeMirror 实例
  if (cmView) {
    cmView.destroy()
    cmView = null
  }
  versionPreviewing.value = false
  selectedVersionId.value = ''
  versionPreviewContent.value = ''
  versionPreviewName.value = ''
  // 双重 nextTick 确保 v-if/v-else 完成切换且 ref 已绑定
  await nextTick()
  await nextTick()
  initCodeMirror()
}

// 从预览模式直接恢复
const restoreFromPreview = async () => {
  if (!selectedVersionId.value) return
  await restore(selectedVersionId.value)
}

// ========== 快照弹窗 ==========
const openSnapshotModal = () => {
  const hasContent = content.value && content.value.trim().length > 0
  const title = hasContent ? (doc.value.title || '文档') : '空白文档'
  const now = new Date()
  const dateStr = now.getFullYear() + '-' +
    String(now.getMonth() + 1).padStart(2, '0') + '-' +
    String(now.getDate()).padStart(2, '0') + ' ' +
    String(now.getHours()).padStart(2, '0') + ':' +
    String(now.getMinutes()).padStart(2, '0') + ':' +
    String(now.getSeconds()).padStart(2, '0')
  snapshotName.value = title + ' ' + dateStr
  snapshotError.value = ''
  snapshotModalOpen.value = true
}

const closeSnapshotModal = () => {
  snapshotModalOpen.value = false
}

const confirmSnapshot = async () => {
  if (!snapshotName.value) {
    snapshotError.value = '请输入保存名称'
    return
  }
  saving.value = true
  snapshotError.value = ''
  try {
    await createSnapshot(props.id, snapshotName.value)
    await loadVersions()
    showNotice('已保存')
    snapshotModalOpen.value = false
  } catch (err) {
    snapshotError.value = err?.response?.data?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

// Markdown 实时预览
const preview = computed(() => marked.parse(content.value || ''))
const showNotice = (text) => {
  notice.value = text
  if (noticeTimer) clearTimeout(noticeTimer)
  noticeTimer = setTimeout(() => {
    notice.value = ''
  }, 3000)
}

const loadDoc = async () => {
  error.value = ''
  noAccess.value = false
  try {
    const data = await getDocument(props.id)
    doc.value = data
    content.value = data.content || ''
    version.value = data.version || 0
    lastSyncedContent.value = data.content || ''
    doc.value.content = lastSyncedContent.value
    saveStatus.value = 'idle'
    setEditorContent(content.value)
  } catch (err) {
    const status = err?.response?.status
    if (status === 403) {
      noAccess.value = true
      return
    }
    error.value = err?.response?.data?.message || '加载失败'
  }
}

// 重新拉取最新内容
const syncNow = async () => {
  syncing.value = true
  pendingOpsQueue = []
  inFlight = null
  pendingDirty = false
  await loadDoc()
  setEditorContent(content.value)
  syncing.value = false
  showNotice('已同步最新内容')
}

// 发送队列中的下一条操作（保持顺序与版本一致）
const sendNextOp = async () => {
  if (inFlight || pendingOpsQueue.length === 0) return
  const next = pendingOpsQueue[0]
  const payload = { ...next, baseVersion: version.value }
  inFlight = payload
  syncing.value = true
  saveStatus.value = 'saving'
  await retryViaHttp(payload)
}

const shouldRetrySync = (err) => {
  const status = err?.response?.status
  if (!err?.response) return true
  return status >= 500
}

const scheduleRetry = () => {
  if (retryTimer || pendingOpsQueue.length === 0) return
  retryTimer = setTimeout(async () => {
    retryTimer = null
    retryDelay = Math.min(retryDelay * 2, maxRetryDelay)
    if (!inFlight) {
      await sendNextOp()
    }
    if (pendingOpsQueue.length > 0) {
      scheduleRetry()
    } else {
      retryDelay = 1000
    }
  }, retryDelay)
}

// HTTP 落库并更新版本号
const retryViaHttp = async (payload) => {
  try {
    const localContent = content.value
    const hadLocalChanges = localContent !== lastSyncedContent.value || pendingDirty
    const result = await applyOperation(props.id, payload)
    version.value = result.appliedVersion
    lastSyncedContent.value = result.content
    doc.value.content = result.content
    if (!hadLocalChanges) {
      content.value = result.content
      setEditorContent(result.content)
    }
    pendingOpsQueue.shift()
    inFlight = null
    syncing.value = pendingOpsQueue.length > 0
    saveStatus.value = 'idle'
    await sendNextOp()
    if (pendingDirty || content.value !== lastSyncedContent.value) {
      pendingDirty = false
      await enqueueDiff()
    }
  } catch (err) {
    const status = err?.response?.status
    inFlight = null
    syncing.value = false
    saveStatus.value = 'error'
    if (status === 409) {
      showNotice('版本冲突，已自动同步')
      await syncNow()
      return
    }
    if (shouldRetrySync(err)) {
      isOffline.value = !err?.response
      showNotice(isOffline.value ? '网络已断开，待恢复后自动重试' : '服务器繁忙，稍后自动重试')
      scheduleRetry()
      return
    }
    pendingOpsQueue = []
    error.value = err?.response?.data?.message || '同步失败'
  }
}

// 简化 diff：返回单段插入/删除
const computeDiff = (oldText, newText) => {
  if (oldText === newText) return null
  let start = 0
  while (start < oldText.length && start < newText.length && oldText[start] === newText[start]) {
    start += 1
  }
  let endOld = oldText.length - 1
  let endNew = newText.length - 1
  while (endOld >= start && endNew >= start && oldText[endOld] === newText[endNew]) {
    endOld -= 1
    endNew -= 1
  }
  const deleteLen = endOld - start + 1
  const insertText = newText.slice(start, endNew + 1)
  return { start, deleteLen: Math.max(0, deleteLen), insertText }
}

const pushOp = (op) => {
  const last = pendingOpsQueue[pendingOpsQueue.length - 1]
  if (!last) {
    pendingOpsQueue.push(op)
    return
  }
  if (last.type === 'INSERT' && op.type === 'INSERT') {
    const lastText = last.text || ''
    if (last.position + lastText.length === op.position) {
      last.text = lastText + (op.text || '')
      return
    }
  }
  if (last.type === 'DELETE' && op.type === 'DELETE') {
    if (op.position === last.position) {
      last.length += op.length
      return
    }
    if (op.position + op.length === last.position) {
      last.position = op.position
      last.length += op.length
      return
    }
  }
  pendingOpsQueue.push(op)
}

// 将 diff 转换为操作并入队
const enqueueDiff = async () => {
  if (inFlight || pendingOpsQueue.length > 0) {
    pendingDirty = true
    return
  }
  pendingDirty = false
  const oldText = lastSyncedContent.value || ''
  const newText = content.value
  const diff = computeDiff(oldText, newText)
  if (!diff) return
  saveStatus.value = 'saving'

  if (diff.deleteLen > 0 && diff.insertText.length > 0) {
    pushOp({
      type: 'DELETE',
      position: diff.start,
      length: diff.deleteLen,
    })
    pushOp({
      type: 'INSERT',
      position: diff.start,
      length: 0,
      text: diff.insertText,
    })
  } else if (diff.deleteLen > 0) {
    pushOp({
      type: 'DELETE',
      position: diff.start,
      length: diff.deleteLen,
    })
  } else {
    pushOp({
      type: 'INSERT',
      position: diff.start,
      length: 0,
      text: diff.insertText,
    })
  }

  await sendNextOp()
}

const publishTyping = () => {
  if (!stompClient || !stompClient.connected) return
  if (typingTimer) clearTimeout(typingTimer)
  typingTimer = setTimeout(() => {
    markActive(userId)
    onlineSet.value = { ...onlineSet.value, [userId]: true }
    stompClient.publishPresence(props.id, { type: 'typing' })
  }, 200)
}

const handleCursor = () => {
  if (!stompClient || !stompClient.connected) return
  if (cursorTimer) clearTimeout(cursorTimer)
  cursorTimer = setTimeout(() => {
    const pos = cmView ? cmView.state.selection.main.head : 0
    markActive(userId)
    onlineSet.value = { ...onlineSet.value, [userId]: true }
    stompClient.publishPresence(props.id, {
      type: 'cursor',
      cursor: typeof pos === 'number' ? pos : null,
    })
  }, 150)
}

// 输入事件：防抖生成操作 + 发送 presence + 自动保存
const handleInput = async () => {
  if (ignoreInput) return
  if (inputTimer) clearTimeout(inputTimer)
  inputTimer = setTimeout(() => {
    enqueueDiff()
  }, 300)
  handleCursor()
  publishTyping()
}

// ========== CodeMirror 初始化 ==========
// 自定义亮色主题
const cmLightTheme = EditorView.theme({
  '&': {
    fontSize: '15px',
    backgroundColor: '#fff',
  },
  '.cm-content': {
    fontFamily: '"Consolas", "Source Code Pro", monospace',
    padding: '16px 20px',
    minHeight: '800px',
    lineHeight: '1.8',
    caretColor: '#2563eb',
  },
  '.cm-gutters': {
    backgroundColor: '#f8fafc',
    color: '#94a3b8',
    border: 'none',
    paddingLeft: '4px',
  },
  '.cm-activeLineGutter': {
    backgroundColor: 'transparent',
    color: '#2563eb',
  },
  '.cm-activeLine': {
    backgroundColor: 'transparent',
  },
  '.cm-selectionBackground': {
    backgroundColor: '#accef7 !important',
  },
  '&.cm-focused .cm-selectionBackground': {
    backgroundColor: '#accef7 !important',
  },
  '.cm-cursor': {
    borderLeftColor: '#2563eb',
    borderLeftWidth: '2px',
  },
  // Markdown 标题高亮
  '.cm-header-1': { fontSize: '1.6em', fontWeight: '700', color: '#1e293b' },
  '.cm-header-2': { fontSize: '1.4em', fontWeight: '700', color: '#334155' },
  '.cm-header-3': { fontSize: '1.2em', fontWeight: '600', color: '#475569' },
  '.cm-header-4': { fontSize: '1.1em', fontWeight: '600', color: '#475569' },
  '.cm-strong': { fontWeight: '700', color: '#1e293b' },
  '.cm-em': { fontStyle: 'italic', color: '#6366f1' },
  '.cm-strikethrough': { textDecoration: 'line-through', color: '#94a3b8' },
  '.cm-link': { color: '#2563eb', textDecoration: 'underline' },
  '.cm-url': { color: '#64748b' },
  '.cm-quote': { color: '#2563eb', fontStyle: 'italic' },
  // 代码块
  '.cm-monospace': { fontFamily: '"Consolas", monospace', backgroundColor: '#f1f5f9', padding: '1px 4px', borderRadius: '3px' },
})


const initCodeMirror = () => {
  if (!editorRef.value || cmView) return
  const isReadonly = doc.value.role === 'VIEWER'
  const state = EditorState.create({
    doc: content.value || '',
    extensions: [
      lineNumbers(),
      drawSelection(),
      rectangularSelection(),
      indentOnInput(),
      bracketMatching(),
      closeBrackets(),
      foldGutter(),
      highlightSelectionMatches(),
      history(),
      autocompletion(),
      cmPlaceholder('开始编写 Markdown 内容...'),
      markdown({ base: markdownLanguage, codeLanguages: languages }),
      syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
      cmLightTheme,
      keymap.of([
        ...defaultKeymap,
        ...historyKeymap,
        ...searchKeymap,
        ...completionKeymap,
        ...closeBracketsKeymap,
        ...foldKeymap,
        indentWithTab,
        // Ctrl+S 保存快照
        { key: 'Mod-s', run: () => { openSnapshotModal(); return true } },
        // Ctrl+B 加粗
        { key: 'Mod-b', run: () => { toggleWrap('**'); return true } },
        // Ctrl+I 斜体
        { key: 'Mod-i', run: () => { toggleWrap('*'); return true } },
      ]),
      editableCompartment.of(EditorView.editable.of(!isReadonly)),
      // 监听文档变化
      EditorView.updateListener.of((update) => {
        if (update.docChanged) {
          const newDoc = update.state.doc.toString()
          if (newDoc !== content.value) {
            content.value = newDoc
            handleInput()
          }
        }
        // 更新光标行号
        if (update.selectionSet || update.docChanged) {
          const line = update.state.doc.lineAt(update.state.selection.main.head)
          cursorLine.value = line.number
        }
      }),
    ],
  })
  cmView = new EditorView({
    state,
    parent: editorRef.value,
  })
}

// 外部更新编辑器内容（收到远端同步时）
const setEditorContent = (text) => {
  if (!cmView) return
  const current = cmView.state.doc.toString()
  if (current === text) return
  ignoreInput = true
  // 保持光标位置
  const cursor = cmView.state.selection.main.head
  cmView.dispatch({
    changes: { from: 0, to: current.length, insert: text },
    selection: { anchor: Math.min(cursor, text.length) },
  })
  nextTick(() => { ignoreInput = false })
}

// 设置编辑器只读状态
const setEditorReadonly = (readonly) => {
  if (!cmView) return
  cmView.dispatch({
    effects: editableCompartment.reconfigure(EditorView.editable.of(!readonly)),
  })
}

// ========== Markdown 工具栏 ==========

/**
 * 切换行内包裹标记（加粗/斜体/删除线/行内代码）
 * - 选中文字 → 加上包裹 / 如果已有则去掉
 * - 未选中 → 在光标处插入一对标记，光标放中间
 * - 处理 * 和 ** 冲突：加粗用 **，斜体用 *，不互相干扰
 */
const toggleWrap = (marker) => {
  if (!cmView) return
  const state = cmView.state
  const { from, to } = state.selection.main
  const mLen = marker.length
  const docLen = state.doc.length

  if (from === to) {
    // 无选中：检查光标两侧是否已有该标记
    const bStart = Math.max(0, from - mLen)
    const aEnd = Math.min(docLen, from + mLen)
    const before = state.sliceDoc(bStart, from)
    const after = state.sliceDoc(from, aEnd)
    if (before === marker && after === marker) {
      // 光标两边恰好有一对标记 → 去掉
      cmView.dispatch({
        changes: [
          { from: bStart, to: from, insert: '' },
          { from: from, to: aEnd, insert: '' },
        ],
        selection: { anchor: bStart },
      })
    } else {
      // 插入一对标记，光标放中间
      cmView.dispatch({
        changes: { from, to: from, insert: marker + marker },
        selection: { anchor: from + mLen },
      })
    }
    cmView.focus()
    return
  }

  const selected = state.sliceDoc(from, to)

  // 精确匹配外部包裹（处理 * / ** 冲突）
  const outerFrom = from - mLen
  const outerTo = to + mLen
  if (outerFrom >= 0 && outerTo <= docLen) {
    const before = state.sliceDoc(outerFrom, from)
    const after = state.sliceDoc(to, outerTo)
    if (before === marker && after === marker) {
      // 还要检查不是更长标记的一部分（** vs *）
      let isExact = true
      if (marker === '*') {
        // 如果外面还有 *，说明其实是 ** 的一部分
        if (outerFrom > 0 && state.sliceDoc(outerFrom - 1, outerFrom) === '*') isExact = false
        if (outerTo < docLen && state.sliceDoc(outerTo, outerTo + 1) === '*') isExact = false
      }
      if (isExact) {
        cmView.dispatch({
          changes: [
            { from: outerFrom, to: from, insert: '' },
            { from: to, to: outerTo, insert: '' },
          ],
          selection: { anchor: outerFrom, head: outerFrom + selected.length },
        })
        cmView.focus()
        return
      }
    }
  }

  // 检查选中内容内部是否已有包裹
  if (selected.startsWith(marker) && selected.endsWith(marker) && selected.length > mLen * 2) {
    let isExact = true
    if (marker === '*') {
      // 内部以 ** 开头说明是加粗不是斜体
      if (selected.startsWith('**')) isExact = false
    }
    if (isExact) {
      const unwrapped = selected.slice(mLen, selected.length - mLen)
      cmView.dispatch({
        changes: { from, to, insert: unwrapped },
        selection: { anchor: from, head: from + unwrapped.length },
      })
      cmView.focus()
      return
    }
  }

  // 加上包裹，选中保持在原文字上
  cmView.dispatch({
    changes: { from, to, insert: marker + selected + marker },
    selection: { anchor: from + mLen, head: from + mLen + selected.length },
  })
  cmView.focus()
}

/**
 * 设置/切换行前缀（标题、列表、引用等）
 */
const setLinePrefix = (prefix) => {
  if (!cmView) return
  const state = cmView.state
  const { from, to } = state.selection.main
  const startLine = state.doc.lineAt(from)
  const endLine = state.doc.lineAt(to)
  const changes = []
  let removed = false

  const prefixRe = /^(#{1,6}\s|>\s|- \[[ x]\]\s|- |\* |\d+\.\s)/

  for (let i = startLine.number; i <= endLine.number; i++) {
    const line = state.doc.line(i)
    const text = line.text
    const match = text.match(prefixRe)

    if (match && match[0] === prefix) {
      changes.push({ from: line.from, to: line.from + prefix.length, insert: '' })
      removed = true
    } else if (match) {
      changes.push({ from: line.from, to: line.from + match[0].length, insert: prefix })
    } else {
      changes.push({ from: line.from, to: line.from, insert: prefix })
    }
  }

  if (changes.length > 0) {
    cmView.dispatch({ changes })
    const updatedLine = cmView.state.doc.line(startLine.number)
    if (removed) {
      cmView.dispatch({ selection: { anchor: updatedLine.from } })
    } else {
      const newMatch = updatedLine.text.match(prefixRe)
      cmView.dispatch({ selection: { anchor: updatedLine.from + (newMatch ? newMatch[0].length : 0) } })
    }
  }
  cmView.focus()
}

/**
 * 插入块级元素（代码块、链接、图片、表格、分割线）
 */
const insertBlock = (type) => {
  if (!cmView) return
  const state = cmView.state
  const { from, to } = state.selection.main
  const selected = state.sliceDoc(from, to)
  const line = state.doc.lineAt(from)
  const needNl = from > 0 && from !== line.from
  const nl = needNl ? '\n' : ''
  let insert = ''
  let cursorOffset = 0
  let selEndOffset = -1 // 如果 >= 0，表示选中范围终点

  switch (type) {
    case 'codeblock':
      if (selected) {
        insert = nl + '```\n' + selected + '\n```\n'
        cursorOffset = nl.length + 4
      } else {
        insert = nl + '```\n\n```\n'
        cursorOffset = nl.length + 4
      }
      break
    case 'table': {
      // 插入3x2表格，光标定位到第一个数据单元格
      const tableContent = '| 列1 | 列2 | 列3 |\n| --- | --- | --- |\n| 数据 | 数据 | 数据 |\n'
      insert = nl + tableContent
      // 光标在第三行 "| " 后面，即"数据"的位置
      const row1 = '| 列1 | 列2 | 列3 |\n'
      const row2 = '| --- | --- | --- |\n'
      cursorOffset = nl.length + row1.length + row2.length + 2 // "| " 后面
      break
    }
    case 'hr':
      insert = nl + '\n---\n\n'
      cursorOffset = insert.length
      break
    default:
      return
  }

  const sel = selEndOffset >= 0
    ? { anchor: from + cursorOffset, head: from + selEndOffset }
    : { anchor: from + cursorOffset }

  cmView.dispatch({
    changes: { from, to, insert },
    selection: sel,
  })
  cmView.focus()
}


// ========== 标题编辑 ==========
const startEditTitle = () => {
  if (doc.value.role === 'VIEWER') return
  editingTitle.value = true
  titleInput.value = doc.value.title || ''
  nextTick(() => {
    titleInputRef.value?.focus()
    titleInputRef.value?.select()
  })
}

const cancelEditTitle = () => {
  editingTitle.value = false
}

const saveTitle = async () => {
  if (!titleInput.value) return
  savingTitle.value = true
  try {
    const data = await updateTitle(props.id, titleInput.value)
    doc.value.title = data.title
    editingTitle.value = false
    showNotice('标题已更新')
  } catch (err) {
    error.value = err?.response?.data?.message || '更新标题失败'
  } finally {
    savingTitle.value = false
  }
}



// 角色显示名
const roleLabel = (role) => {
  if (role === 'OWNER') return '创建者'
  if (role === 'EDITOR') return '编辑者'
  if (role === 'VIEWER') return '只读'
  return role
}

// 移除成员（仅 OWNER 可操作）
const doRemoveMember = async (member) => {
  if (!confirm(`确定移除成员「${member.displayName}」吗？移除后对方将无法继续访问该文档。`)) return
  try {
    await removeMember(props.id, member.userId)
    await loadMembers()
    showNotice(`已移除成员「${member.displayName}」`)
  } catch (err) {
    error.value = err?.response?.data?.message || '移除失败'
  }
}

const openAddMember = () => {
  inviteError.value = ''
  addMemberOpen.value = true
}

const closeAddMember = () => {
  addMemberOpen.value = false
}

const joinByLink = async () => {
  joining.value = true
  joinError.value = ''
  try {
    const data = await joinDocument(props.id)
    doc.value = data
    content.value = data.content || ''
    version.value = data.version || 0
    noAccess.value = false
    await nextTick()
    if (!cmView) initCodeMirror()
    else setEditorContent(content.value)
    await loadMembers()
    await loadVersions()
    if (!stompClient || !stompClient.connected) {
      connect()
    }
  } catch (err) {
    joinError.value = err?.response?.data?.message || '加入失败'
  } finally {
    joining.value = false
  }
}

const copyLink = async () => {
  await navigator.clipboard.writeText(window.location.href)
  showNotice('链接已复制')
}

const loadMembers = async () => {
  memberLoading.value = true
  memberError.value = ''
  try {
    members.value = await listMembers(props.id)
  } catch (err) {
    const message = err?.response?.data?.message || '加载成员失败'
    memberError.value = message
    error.value = message
  } finally {
    memberLoading.value = false
  }
}

const inviteMember = async () => {
  inviteError.value = ''
  inviting.value = true
  try {
    await addMember(props.id, inviteForm.value)
    inviteForm.value.username = ''
    await loadMembers()
    showNotice('已发送邀请')
    addMemberOpen.value = false
  } catch (err) {
    inviteError.value = err?.response?.data?.message || '邀请失败'
  } finally {
    inviting.value = false
  }
}

// presence 快照与活跃状态
const updatePresenceSnapshot = (users) => {
  onlineUsers.value = users || []
  const map = {}
  const online = {}
  onlineUsers.value.forEach((user) => {
    map[user.userId] = user.cursor
    online[user.userId] = true
  })
  cursorMap.value = map
  onlineSet.value = online
}

const markActive = (userId) => {
  activityMap.value = {
    ...activityMap.value,
    [userId]: Date.now(),
  }
}

// 根据最近活跃时间展示状态
const getStatus = (userIdValue) => {
  const _ = nowTick.value
  if (!onlineSet.value[userIdValue]) return '离线'
  const last = activityMap.value[userIdValue] || 0
  if (Date.now() - last < 5000) return '输入中'
  return '在线'
}

// presence 心跳，维持在线状态
const startHeartbeat = () => {
  if (heartbeatTimer) return
  heartbeatTimer = setInterval(() => {
    if (stompClient && stompClient.connected) {
      stompClient.publishPresence(props.id, { type: 'ping' })
    }
  }, 10000)
}

// 定时刷新状态，避免一直停在“输入中”
const startStatusTick = () => {
  if (statusTimer) return
  statusTimer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
}

// 连接 WS：订阅操作与在线状态
const connect = () => {
  stompClient = connectStomp({
    onConnect: (client) => {
      connection.value = 'connected'
      subscription = client.subscribeDocument(props.id, (message) => {
        const sameAuthor = Number(message.authorId) === Number(userId)
        const hasLocalChanges =
          content.value !== lastSyncedContent.value || inFlight || pendingOpsQueue.length > 0 || pendingDirty
         ignoreInput = true
        lastSyncedContent.value = message.content
        doc.value.content = message.content
        version.value = message.appliedVersion
        if (!hasLocalChanges || sameAuthor) {
          content.value = message.content
          setEditorContent(message.content)
        }
         if (message.authorId != null) {
           onlineSet.value = { ...onlineSet.value, [message.authorId]: true }
           markActive(message.authorId)
         }
         setTimeout(() => {
           ignoreInput = false
         }, 0)
       })
      presenceSubscription = client.subscribePresence(props.id, (message) => {
        if (message.type === 'snapshot') {
          updatePresenceSnapshot(message.users)
        } else if (message.type === 'cursor') {
          cursorMap.value = {
            ...cursorMap.value,
            [message.userId]: message.cursor,
          }
          onlineSet.value = { ...onlineSet.value, [message.userId]: true }
          markActive(message.userId)
        } else if (message.type === 'typing') {
          onlineSet.value = { ...onlineSet.value, [message.userId]: true }
          markActive(message.userId)
        }
      })
      client.publishPresence(props.id, {
        type: 'join',
        displayName,
      })
      onlineSet.value = { ...onlineSet.value, [userId]: true }
      startHeartbeat()
      startStatusTick()
      syncNow()
    },
    onStompError: () => {
      showNotice('连接异常，建议点击同步按钮')
    },
    onWebSocketClose: () => {
      connection.value = 'reconnecting'
    },
  })
  stompClient.activate()
}

const loadVersions = async () => {
  try {
    versions.value = await listVersions(props.id)
    if (versions.value.length && !selectedVersionId.value) {
      selectedVersionId.value = String(versions.value[0].id)
    }
  } catch (err) {
    error.value = err?.response?.data?.message || '加载版本失败'
  }
}

const restore = async (versionId) => {
  try {
    const data = await restoreVersion(props.id, versionId)
    doc.value = data
    content.value = data.content || ''
    version.value = data.version || 0
    lastSyncedContent.value = data.content || ''
    // 销毁旧 CodeMirror 实例
    if (cmView) {
      cmView.destroy()
      cmView = null
    }
    // 退出版本预览和关闭抽屉，让编辑区 DOM 重新出现
    versionPreviewing.value = false
    selectedVersionId.value = ''
    versionPreviewContent.value = ''
    versionPreviewName.value = ''
    versionDrawerOpen.value = false
    // 双重 nextTick 确保 v-if/v-else 完成切换且 ref 已绑定
    await nextTick()
    await nextTick()
    initCodeMirror()
    showNotice('已恢复到选中版本')
    await loadVersions()
  } catch (err) {
    error.value = err?.response?.data?.message || '恢复失败'
  }
}


const formatTime = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleString()
}

onMounted(async () => {
   handleOnline = async () => {
      isOffline.value = false
      retryDelay = 1000
      if (pendingOpsQueue.length > 0 && !inFlight) {
        await sendNextOp()
      }
    }
   handleOffline = () => {
      isOffline.value = true
      showNotice('网络已断开，待恢复后自动重试')
    }
   isOffline.value = typeof navigator !== 'undefined' && !navigator.onLine
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
    // 滚动时实时更新抽屉位置，使其始终紧贴 header 下方
    window.addEventListener('scroll', updateDrawerTop, { passive: true })
    await loadDoc()
    if (!noAccess.value) {
      await nextTick()
      initCodeMirror()
      connect()
      await loadMembers()
      await loadVersions()
    }
  })

 onBeforeUnmount(() => {
  if (noticeTimer) clearTimeout(noticeTimer)
  if (inputTimer) clearTimeout(inputTimer)
  if (cursorTimer) clearTimeout(cursorTimer)
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  if (statusTimer) clearInterval(statusTimer)
  if (typingTimer) clearTimeout(typingTimer)
  if (retryTimer) clearTimeout(retryTimer)
  if (handleOnline) window.removeEventListener('online', handleOnline)
  if (handleOffline) window.removeEventListener('offline', handleOffline)
  window.removeEventListener('scroll', updateDrawerTop)
  if (presenceSubscription) {
    presenceSubscription.unsubscribe()
  }
  if (subscription) {
    subscription.unsubscribe()
  }
  if (stompClient) {
    try {
      stompClient.publishPresence(props.id, { type: 'leave' })
    } catch {
      // ignore disconnect errors
    }
    stompClient.deactivate()
  }
  // 销毁 CodeMirror 实例
  if (cmView) {
    cmView.destroy()
    cmView = null
  }
})
</script>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
}

.modal {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  width: min(420px, 90vw);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.icon-button {
  border: none;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
  line-height: 1;
}

/* 抽屉：固定定位在右侧，不遮挡主页面 */
.drawer {
  position: fixed;
  right: 0;
  bottom: 0;
  width: 320px;
  background: #fff;
  padding: 16px;
  border-left: 1px solid #e5e5e5;
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.08);
  overflow: auto;
  z-index: 90;
}

.drawer-close {
  position: absolute;
  top: 12px;
  right: 12px;
  border: none;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
  line-height: 1;
  color: #666;
}
.drawer-close:hover {
  color: #333;
}

.drawer h3 {
  margin: 0 0 12px 0;
  padding-right: 28px;
}

/* 抽屉滑动动画 */
.drawer-enter-active,
.drawer-leave-active {
  transition: transform 250ms ease, opacity 250ms ease;
}
.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.row.end {
  justify-content: flex-end;
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  position: sticky;
  top: 49px;
  z-index: 100;
  background: #f6f8fa;
  padding: 12px 24px;
  margin: -24px -24px 0 -24px;
  border-bottom: 1px solid #e2e8f0;
}
.editor-header .row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.editor-header h2 {
  margin: 0 0 4px 0;
}

/* 标题可点击编辑 */
/* 返回按钮 */
.back-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #e2e8f0;
  color: #334155;
  font-size: 18px;
  text-decoration: none;
  flex-shrink: 0;
  transition: background 150ms;
}
.back-btn:hover {
  background: #cbd5e1;
  text-decoration: none;
}

/* 标题可点击编辑 */
.editable-title {
  cursor: pointer;
  margin: 0 0 4px 0;
}
.editable-title:hover {
  color: #2563eb;
}
.edit-icon {
  font-size: 14px;
  color: #94a3b8;
  margin-left: 4px;
}
.editable-title:hover .edit-icon {
  color: #2563eb;
}
.title-edit-row {
  margin-bottom: 4px;
}
.title-input {
  font-size: 18px;
  font-weight: 600;
  padding: 4px 8px;
  max-width: 300px;
}
.editor-header .meta {
  line-height: 1.4;
}

.drawer .row {
  margin-bottom: 12px;
}
.drawer .list {
  margin-top: 20px;
}
.drawer .muted,
.drawer .error {
  margin-top: 12px;
}

.version-item {
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
}
.version-item:hover {
  background: #f8fafc;
}
.version-item.active {
  background: #eff6ff;
}
.version-restore-btn {
  padding: 4px 10px;
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

/* 版本 Tab 切换 */
.version-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 12px;
  border-bottom: 2px solid #e2e8f0;
}
.version-tab {
  flex: 1;
  padding: 8px 0;
  border: none;
  background: transparent;
  font-weight: 600;
  font-size: 13px;
  color: #94a3b8;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 150ms;
}
.version-tab.active {
  color: #2563eb;
  border-bottom-color: #2563eb;
}
.version-tab:hover {
  color: #334155;
}

/* 当前版本标签 */
.current-badge {
  display: inline-block;
  background: #dcfce7;
  color: #166534;
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
  margin-left: 6px;
  vertical-align: middle;
}

/* 版本预览 — 替代编辑区 */
.version-preview-main {
  display: flex;
  flex-direction: column;
  gap: 0;
  max-width: 794px;
  width: 100%;
  margin: 0 auto;
}
.version-preview-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fef3c7;
  border: 1px solid #fbbf24;
  border-radius: 8px 8px 0 0;
  font-size: 14px;
  color: #92400e;
}
.version-preview-bar .row {
  gap: 8px;
}
.version-main-preview {
  min-height: 420px;
  max-height: none;
  border: 1px solid #e2e8f0;
  border-top: none;
  border-radius: 0 0 8px 8px;
}

/* 预览按钮激活状态 */
button.ghost.active {
  background: #2563eb;
  color: #fff;
}

/* 编辑区容器：左右并排，整体居中保持对称 */
.editor-area {
  display: flex;
  justify-content: center;
  gap: 24px;
  align-items: flex-start;
  width: 100%;
  overflow-x: auto;
}

/* 右侧预览面板：与编辑区同形状 */
.preview-panel-right {
  width: 794px;
  min-width: 794px;
  min-height: 1123px;
  background: #fff;
  border: 1px solid #d0d7de;
  border-radius: 2px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.preview-right-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  flex-shrink: 0;
}

.preview-right-body {
  padding: 60px 72px;
  flex: 1;
  overflow: auto;
}

/* 右侧预览展开/收起动画 */
.preview-right-enter-active {
  transition: all 350ms ease-out;
}
.preview-right-leave-active {
  transition: all 280ms ease-in;
}
.preview-right-enter-from {
  opacity: 0;
  min-width: 0;
  width: 0;
  padding: 0;
  overflow: hidden;
}
.preview-right-leave-to {
  opacity: 0;
  min-width: 0;
  width: 0;
  padding: 0;
  overflow: hidden;
}

.preview-title {
  font-weight: 600;
  font-size: 14px;
  color: #334155;
}

.preview-body {
  padding: 16px 20px;
  min-height: 200px;
  overflow: auto;
  line-height: 1.7;
  font-size: 15px;
  color: #1f2328;
}

/* 预览区内的 Markdown 渲染样式 */
.preview-body h1 { font-size: 2em; margin: 0.5em 0 0.3em; border-bottom: 1px solid #e2e8f0; padding-bottom: 0.3em; }
.preview-body h2 { font-size: 1.5em; margin: 0.5em 0 0.3em; border-bottom: 1px solid #e2e8f0; padding-bottom: 0.3em; }
.preview-body h3 { font-size: 1.25em; margin: 0.5em 0 0.3em; }
.preview-body h4 { font-size: 1em; margin: 0.5em 0 0.3em; }
.preview-body p { margin: 0.6em 0; }
.preview-body ul, .preview-body ol { padding-left: 1.5em; margin: 0.5em 0; }
.preview-body li { margin: 0.2em 0; }
.preview-body blockquote {
  margin: 0.5em 0;
  padding: 0.5em 1em;
  border-left: 4px solid #2563eb;
  background: #f1f5f9;
  color: #475569;
}
.preview-body code {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: "Consolas", monospace;
  font-size: 0.9em;
  color: #e11d48;
}
.preview-body pre {
  background: #1e293b;
  color: #e2e8f0;
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0.6em 0;
}
.preview-body pre code {
  background: transparent;
  color: inherit;
  padding: 0;
}
.preview-body table {
  border-collapse: collapse;
  width: 100%;
  margin: 0.6em 0;
}
.preview-body th, .preview-body td {
  border: 1px solid #e2e8f0;
  padding: 8px 12px;
  text-align: left;
}
.preview-body th {
  background: #f8fafc;
  font-weight: 600;
}
.preview-body a {
  color: #2563eb;
  text-decoration: none;
}
.preview-body a:hover {
  text-decoration: underline;
}
.preview-body img {
  max-width: 100%;
  border-radius: 6px;
}
.preview-body hr {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 1em 0;
}


/* 提示条淡入滑下 / 淡出滑上 动画（含高度过渡避免编辑区跳动） */
.notice-slide-enter-active {
  transition: all 300ms ease-out;
  overflow: hidden;
}
.notice-slide-leave-active {
  transition: all 250ms ease-in;
  overflow: hidden;
}
.notice-slide-enter-from {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
  margin-top: 0;
  margin-bottom: 0;
  transform: translateY(-8px);
}
.notice-slide-enter-to {
  opacity: 1;
  max-height: 60px;
  transform: translateY(0);
}
.notice-slide-leave-from {
  opacity: 1;
  max-height: 60px;
  transform: translateY(0);
}
.notice-slide-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
  margin-top: 0;
  margin-bottom: 0;
  transform: translateY(-8px);
}

/* 成员列表项 */
.member-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
}
.member-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.member-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}
.uname {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 400;
}
.role-tag {
  display: inline-block;
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 999px;
  margin-right: 6px;
}
.role-owner { background: #dbeafe; color: #1d4ed8; }
.role-editor { background: #dcfce7; color: #166534; }
.role-viewer { background: #f1f5f9; color: #475569; }
.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}
.status-dot.online { background: #22c55e; }
.status-dot.offline { background: #d1d5db; }
.btn-remove-member {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #dc2626;
  font-size: 16px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  flex-shrink: 0;
  transition: background 150ms;
}
.btn-remove-member:hover {
  background: #fee2e2;
}

/* ===== CodeMirror 编辑器容器 ===== */
.editor-wrapper {
  display: flex;
  flex-direction: column;
  width: 794px;
  min-width: 794px;
  flex-shrink: 0;
}

.cm-host {
  border: 1px solid #d0d7de;
  border-radius: 0 0 2px 2px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.08);
  background: #fff;
  overflow: hidden;
  min-height: 1000px;
}

/* 去掉 CodeMirror 默认的聚焦边框 */
.cm-host :deep(.cm-editor) {
  outline: none;
}
.cm-host :deep(.cm-editor.cm-focused) {
  outline: none;
}

/* Markdown 工具栏 */
.md-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  padding: 6px 10px;
  background: #f8fafc;
  border: 1px solid #d0d7de;
  border-bottom: none;
  border-radius: 2px 2px 0 0;
}

.tb-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 30px;
  padding: 0 7px;
  border: none;
  border-radius: 5px;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  transition: all 120ms;
  white-space: nowrap;
}
.tb-btn:hover {
  background: #e2e8f0;
  color: #1e293b;
}
.tb-btn:active {
  background: #cbd5e1;
}

.tb-sep {
  width: 1px;
  height: 20px;
  background: #e2e8f0;
  margin: 0 4px;
  flex-shrink: 0;
}

/* 底部状态栏 */
.editor-statusbar {
  display: flex;
  gap: 18px;
  padding: 5px 14px;
  font-size: 12px;
  color: #94a3b8;
  background: #f8fafc;
  border: 1px solid #d0d7de;
  border-top: none;
  border-radius: 0 0 4px 4px;
}

/* 响应式：小屏幕下编辑器自适应宽度 */
@media (max-width: 860px) {
  .editor-wrapper {
    width: 100%;
    min-width: 0;
  }
  .cm-host :deep(.cm-content) {
    padding: 20px 16px;
  }
  .preview-panel-right {
    width: 100% !important;
    min-width: 0 !important;
  }
}

</style>
