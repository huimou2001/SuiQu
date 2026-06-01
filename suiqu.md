# SuiQu 网盘系统 - 接口文档

## 通用说明

### 统一响应格式

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | Integer | 状态码，200=成功，500=失败 |
| `msg` | String | 错误信息（失败时返回） |
| `data` | T | 业务数据 |

### 认证方式

除 `/auth/**` 和 `GET /share/{shareId}` 外，其余接口均需在请求头中携带 JWT Token：

```
Authorization: Bearer <token>
```

---

## 1. 认证模块 (`/auth`)

### 1.1 用户登录

- **URL**: `POST /auth/login`
- **是否需要认证**: 否

**请求体 (JSON)**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | String | 是 | 用户名 |
| `password` | String | 是 | 密码 |

**响应数据**: `String` — JWT Token

---

### 1.2 用户注册

- **URL**: `POST /auth/register`
- **是否需要认证**: 否

**请求体 (JSON)**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | String | 是 | 用户名 |
| `password` | String | 是 | 密码 |
| `email` | String | 否 | 邮箱 |

**响应数据**: `"ok"`

---

## 2. 文件模块 (`/file`)

### 2.1 秒传与断点续传检查

- **URL**: `GET /file/check`
- **是否需要认证**: 是

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `md5` | String | 是 | 文件 MD5 值 |

**响应数据**: `CheckUploadVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `isExist` | Boolean | 是否秒传成功 |
| `uploadedChunks` | List\<Integer\> | 已上传的分片索引列表（用于断点续传） |

---

### 2.2 上传分片

- **URL**: `POST /file/upload-chunk`
- **是否需要认证**: 是

**请求参数 (multipart/form-data)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | MultipartFile | 是 | 分片文件 |
| `md5` | String | 是 | 文件 MD5 值 |
| `index` | Integer | 是 | 当前分片索引 |

**响应数据**: `"ok"`

---

### 2.3 合并分片

- **URL**: `POST /file/merge`
- **是否需要认证**: 是

**请求体 (JSON)**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `md5` | String | 是 | 文件 MD5 值 |
| `fileName` | String | 是 | 文件名 |
| `totalChunks` | Integer | 是 | 分片总数 |
| `parentId` | Long | 是 | 所属目录 ID |

**响应数据**: `"ok"`

---

### 2.4 获取文件列表

- **URL**: `GET /file/list`
- **是否需要认证**: 是

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `parentId` | Long | 是 | 父目录 ID（根目录传 0 或 -1） |

**响应数据**: 文件列表（由当前登录用户的文件构成）

---

### 2.5 创建文件夹

- **URL**: `POST /file/create-dir`
- **是否需要认证**: 是

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 是 | 文件夹名称 |
| `parentId` | Long | 是 | 父目录 ID |

**响应数据**: `"ok"`

---

### 2.6 删除文件

- **URL**: `DELETE /file/{id}`
- **是否需要认证**: 是

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | Long | 是 | 文件记录 ID |

**响应数据**: `"删除成功"`

---

## 3. 分享模块 (`/share`)

### 3.1 创建分享链接

- **URL**: `POST /share/create`
- **是否需要认证**: 是

**请求体 (JSON)**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fileId` | Long | 是 | 文件 ID |
| `expireHours` | Integer | 是 | 有效时长（单位：小时） |

**响应数据**: `ShareVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 分享记录 ID |
| `shareUrl` | String | 分享链接 |
| `expireTime` | LocalDateTime | 过期时间 |
| `status` | Integer | 状态 |
| `fileName` | String | 文件名 |
| `fileSize` | Long | 文件大小 |
| `fileType` | String | 文件类型 |

---

### 3.2 查看分享详情

- **URL**: `GET /share/{shareId}`
- **是否需要认证**: 否（公共接口）

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `shareId` | String | 是 | 分享标识 |

**响应数据**: `ShareVO`（分享不存在或已失效时返回错误提示 `"分享链接已失效"`）

---

## 4. 搜索模块 (`/search`)

### 4.1 搜索文件

- **URL**: `GET /search/files`
- **是否需要认证**: 是

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 是 | 搜索关键词 |

**搜索逻辑**: 基于 Elasticsearch，按当前用户过滤，对文件名（`name`）和描述（`description`）进行混合查询（multiMatch + wildcard 模糊匹配），name/description 使用 ik 分词器。

**响应数据**: `List<FileIndex>`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 文件 ID |
| `name` | String | 文件名 |
| `description` | String | 文件描述 |
| `userId` | Long | 所属用户 ID |
| `createTime` | LocalDateTime | 创建时间 |
