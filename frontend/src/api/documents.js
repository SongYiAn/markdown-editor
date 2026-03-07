import client from './client'

// 获取文档列表
export async function listDocuments() {
  const { data } = await client.get('/api/documents')
  return data
}

// 创建文档
export async function createDocument(payload) {
  const { data } = await client.post('/api/documents', payload)
  return data
}

// 获取文档详情
export async function getDocument(id) {
  const { data } = await client.get(`/api/documents/${id}`)
  return data
}

// 提交一次编辑操作
export async function applyOperation(id, payload) {
  const { data } = await client.post(`/api/documents/${id}/ops`, payload)
  return data
}

// 保存快照（支持自定义名称）
export async function createSnapshot(id, name) {
  const { data } = await client.post(`/api/documents/${id}/versions/snapshot`, { name })
  return data
}

// 获取历史版本
export async function listVersions(id) {
  const { data } = await client.get(`/api/documents/${id}/versions`)
  return data
}

// 恢复历史版本
export async function restoreVersion(id, versionId) {
  const { data } = await client.post(`/api/documents/${id}/versions/${versionId}/restore`)
  return data
}

// 获取单个版本详情（含内容，用于预览）
export async function getVersionDetail(id, versionId) {
  const { data } = await client.get(`/api/documents/${id}/versions/${versionId}`)
  return data
}

// 添加成员
export async function addMember(id, payload) {
  const { data } = await client.post(`/api/documents/${id}/members`, payload)
  return data
}

// 移除成员（仅 OWNER）
export async function removeMember(id, targetUserId) {
  await client.delete(`/api/documents/${id}/members/${targetUserId}`)
}

// 获取成员列表
export async function listMembers(id) {
  const { data } = await client.get(`/api/documents/${id}/members`)
  return data
}

// 通过链接加入协作
export async function joinDocument(id) {
  const { data } = await client.post(`/api/documents/${id}/join`)
  return data
}

// 删除文档（仅 OWNER）
export async function deleteDocument(id) {
  await client.delete(`/api/documents/${id}`)
}

// 更新文档标题
export async function updateTitle(id, title) {
  const { data } = await client.post(`/api/documents/${id}/title`, { title })
  return data
}

