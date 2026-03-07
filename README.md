# Markdown Editor (Backend)

Minimal Spring Boot backend for a collaborative Markdown editor.

最小可运行的 Spring Boot 后端：协同编辑、鉴权、版本管理。

## Features
- Token-based auth (register/login)
- Document CRUD (create, list, fetch)
- Operation-based updates with version check
- WebSocket STOMP broadcast for collaboration
- Version snapshot list and creation

## 快速开始
1. 准备 MySQL 并创建数据库 `markdown_editor`。
2. （可选）配置环境变量：
   - `DB_URL`
   - `DB_USER`
   - `DB_PASS`
3. 启动应用。

## Run
```powershell
cd D:\Soft\Java\markdown-editor
./mvnw spring-boot:run
```

## API (HTTP)
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/documents`
- `GET /api/documents`
- `GET /api/documents/{id}`
- `POST /api/documents/{id}/members`
- `GET /api/documents/{id}/members`
- `POST /api/documents/{id}/ops`
- `POST /api/documents/{id}/versions/snapshot`
- `GET /api/documents/{id}/versions`
- `POST /api/documents/{id}/versions/{versionId}/restore`

## WebSocket
- Endpoint: `/ws`
- App destination: `/app/document/{id}/op`
- Broadcast: `/topic/document/{id}/ops`

- Presence: `/topic/document/{id}/presence`
- App destination: `/app/document/{id}/presence`
