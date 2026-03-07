import { Client } from '@stomp/stompjs'
import { getAuth } from './storage'

// STOMP 连接封装：自动处理鉴权、订阅与发布
export function connectStomp({ onMessage, onConnect, onStompError, onWebSocketClose }) {
  const { token } = getAuth()
  const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const defaultHost = window.location.host
  const devHost = defaultHost === 'localhost:5173' || defaultHost === '127.0.0.1:5173'
  const brokerURL = devHost ? `${scheme}://localhost:8080/ws` : `${scheme}://${defaultHost}/ws`
  const client = new Client({
    brokerURL,
    // 重连间隔（毫秒）
    reconnectDelay: 3000,
    // 连接时携带 token
    connectHeaders: token
      ? { Authorization: `Bearer ${token}`, 'X-Auth-Token': token }
      : {},
    onConnect: () => {
      if (onConnect) onConnect(client)
    },
    onStompError: (frame) => {
      if (onStompError) onStompError(frame)
    },
    onWebSocketClose: (event) => {
      if (onWebSocketClose) onWebSocketClose(event)
    },
  })

  // 订阅文档操作广播
  client.subscribeDocument = (documentId, handler) => {
    return client.subscribe(`/topic/document/${documentId}/ops`, (frame) => {
      try {
        handler(JSON.parse(frame.body))
      } catch {
        // ignore invalid payload
      }
    })
  }

  // 发送文档操作
  client.publishOperation = (documentId, payload) => {
    client.publish({
      destination: `/app/document/${documentId}/op`,
      body: JSON.stringify(payload),
    })
  }

  // 订阅在线状态
  client.subscribePresence = (documentId, handler) => {
    return client.subscribe(`/topic/document/${documentId}/presence`, (frame) => {
      try {
        handler(JSON.parse(frame.body))
      } catch {
        // ignore invalid payload
      }
    })
  }

  // 发送在线状态消息（自动附带 token）
  client.publishPresence = (documentId, payload) => {
    const data = { ...payload }
    if (token && !data.token) {
      data.token = token
    }
    client.publish({
      destination: `/app/document/${documentId}/presence`,
      body: JSON.stringify(data),
    })
  }

  return client
}
