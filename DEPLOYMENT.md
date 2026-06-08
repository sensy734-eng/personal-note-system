# 个人学习笔记管理系统部署说明

## 推荐方案

- 前端：Vercel，部署 `frontend` 目录。
- 后端：Render Web Service，部署 `backend` 目录，使用 Docker。
- 数据库：Aiven MySQL Free Tier，导入根目录的 `note_system.sql`。

## 1. 数据库

1. 在 Aiven 创建 MySQL 服务。
2. 新建或使用默认数据库 `note_system`。
3. 导入 `note_system.sql`。
4. 记录连接信息：Host、Port、User、Password、Database。

演示账号：

- 管理员：`admin / password`
- 普通用户：`student / password`

## 2. 后端 Render

Render 新建 Web Service 时建议使用：

- Root Directory：`backend`
- Runtime：Docker
- Plan：Free

环境变量：

| 变量 | 示例 | 说明 |
|---|---|---|
| `DB_HOST` | `xxx.aivencloud.com` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `note_system` | 数据库名 |
| `DB_USER` | `avnadmin` | 数据库用户名 |
| `DB_PASSWORD` | `******` | 数据库密码 |
| `JWT_SECRET` | `change-this-to-a-long-random-secret` | JWT 签名密钥 |
| `UPLOAD_PATH` | `/tmp/uploads/` | 上传文件目录 |
| `IMAGE_BASE_URL` | `https://<render-domain>/uploads/` | 图片访问前缀 |
| `FRONTEND_URL` | `https://<vercel-domain>` | 允许跨域的前端地址 |

健康检查地址：

```text
https://<render-domain>/health
```

## 3. 前端 Vercel

Vercel 导入仓库时建议使用：

- Root Directory：`frontend`
- Build Command：`npm run build`
- Output Directory：`dist`

环境变量：

```text
VITE_API_BASE_URL=https://<render-domain>/api
```

部署完成后，如果 Render 后端的 `FRONTEND_URL` 或 `IMAGE_BASE_URL` 使用了占位域名，需要回填真实地址并重新部署后端。

## 4. 答辩验证流程

1. 打开 Vercel 前端地址。
2. 使用 `admin / password` 登录，验证管理员面板、系统统计和用户列表。
3. 使用 `student / password` 登录，验证普通用户无法看到管理员入口。
4. 演示笔记新增、编辑、删除、恢复、永久删除。
5. 演示分页、搜索、分类、标签、星标筛选。
6. 演示单篇导出、分类导出、头像上传和统计看板。

## 5. 注意事项

- Render 免费服务存在冷启动，答辩前先访问一次后端 `/health`。
- Aiven 免费数据库容量适合课程演示，不适合生产长期大量数据。
- Render 的 `/tmp/uploads/` 是临时目录，服务重启后上传头像可能丢失；课程演示可以接受，生产应改用对象存储。
- 修改 Vercel 或 Render 环境变量后需要重新部署。
