import { useState, useEffect, useCallback, useRef } from 'react'
import {
  Table, Button, Space, Modal, Input, Upload, Breadcrumb, message,
  Popconfirm, Progress, Tooltip, Typography,
} from 'antd'
import {
  FolderOpenOutlined, FileOutlined, DeleteOutlined,
  CloudUploadOutlined, FolderAddOutlined, ShareAltOutlined,
  ArrowUpOutlined, EditOutlined,
} from '@ant-design/icons'
import {
  getFileList, createDirectory, deleteFile, checkFile,
  uploadChunk, mergeChunks, updateDescription,
} from '../api/file'
import { computeMD5, getChunkCount, CHUNK_SIZE } from '../utils/uploader'
import ShareModal from '../components/ShareModal'

const { Search } = Input
const { Text } = Typography

function formatTime(t) {
  if (!t) return '-'
  if (Array.isArray(t)) {
    const [y, m, d, h, mi, s] = t
    return `${y}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')} ${String(h).padStart(2,'0')}:${String(mi).padStart(2,'0')}:${String(s).padStart(2,'0')}`
  }
  if (typeof t === 'string') {
    const d = new Date(t)
    if (!isNaN(d)) {
      const pad = (n) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    }
  }
  return t
}

function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function getFileIcon(record) {
  if (record.isDir === 1) return <FolderOpenOutlined style={{ color: '#faad14', fontSize: 20 }} />
  return <FileOutlined style={{ color: '#1890ff', fontSize: 20 }} />
}

export default function FilePage({ searchKeyword, onSearchClear }) {
  const [files, setFiles] = useState([])
  const [loading, setLoading] = useState(false)
  const [pathStack, setPathStack] = useState([{ id: 0, name: '全部文件' }])
  const [dirModalOpen, setDirModalOpen] = useState(false)
  const [dirName, setDirName] = useState('')
  const [uploadModalOpen, setUploadModalOpen] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [uploading, setUploading] = useState(false)
  const [shareModalOpen, setShareModalOpen] = useState(false)
  const [shareFileId, setShareFileId] = useState(null)
  const fileListRef = useRef(null)

  const currentParentId = pathStack[pathStack.length - 1].id

  const fetchFiles = useCallback(async () => {
    setLoading(true)
    try {
      const data = await getFileList(currentParentId)
      setFiles(Array.isArray(data) ? data : [])
    } catch {
      // handled
    } finally {
      setLoading(false)
    }
  }, [currentParentId])

  useEffect(() => {
    if (!searchKeyword) {
      fetchFiles()
    }
  }, [fetchFiles, searchKeyword])

  const handleOpenDir = (record) => {
    setPathStack([...pathStack, { id: record.id, name: record.fileName }])
  }

  const handleBreadcrumbClick = (index) => {
    setPathStack(pathStack.slice(0, index + 1))
    if (onSearchClear) onSearchClear()
  }

  const handleCreateDir = async () => {
    if (!dirName.trim()) {
      message.warning('请输入文件夹名称')
      return
    }
    try {
      await createDirectory(dirName.trim(), currentParentId)
      message.success('创建成功')
      setDirModalOpen(false)
      setDirName('')
      fetchFiles()
    } catch {
      // handled
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteFile(id)
      message.success('删除成功')
      fetchFiles()
    } catch {
      // handled
    }
  }

  const handleUpload = async (options) => {
    const { file } = options
    setUploading(true)
    setUploadProgress(0)

    try {
      const md5 = await computeMD5(file, (p) => setUploadProgress(Math.round(p * 0.3)))

      const checkResult = await checkFile(md5)
      if (checkResult?.isExist) {
        message.success('秒传成功')
        fetchFiles()
        return
      }

      const totalChunks = getChunkCount(file)
      const uploadedSet = new Set(checkResult?.uploadedChunks || [])

      for (let i = 0; i < totalChunks; i++) {
        if (uploadedSet.has(i)) continue

        const start = i * CHUNK_SIZE
        const end = Math.min(start + CHUNK_SIZE, file.size)
        const chunk = file.slice(start, end)

        const formData = new FormData()
        formData.append('file', chunk)
        formData.append('md5', md5)
        formData.append('index', i)

        await uploadChunk(formData)
        setUploadProgress(30 + Math.round(((i + 1) / totalChunks) * 60))
      }

      await mergeChunks(md5, file.name, totalChunks, currentParentId)
      setUploadProgress(100)
      message.success('上传成功')
      fetchFiles()
    } catch {
      message.error('上传失败')
    } finally {
      setTimeout(() => {
        setUploading(false)
        setUploadProgress(0)
        setUploadModalOpen(false)
      }, 800)
    }
  }

  const [descModalOpen, setDescModalOpen] = useState(false)
  const [descFileId, setDescFileId] = useState(null)
  const [descValue, setDescValue] = useState('')

  const handleDescription = (record) => {
    setDescFileId(record.id)
    setDescValue(record.description || '')
    setDescModalOpen(true)
  }

  const handleDescriptionSave = async () => {
    try {
      await updateDescription(descFileId, descValue)
      message.success('描述更新成功')
      setDescModalOpen(false)
      setDescFileId(null)
      fetchFiles()
    } catch {
      // handled
    }
  }

  const handleShare = (record) => {
    setShareFileId(record.id)
    setShareModalOpen(true)
  }

  const columns = [
    {
      title: '文件名',
      dataIndex: 'fileName',
      key: 'fileName',
      render: (text, record) => {
        const isDir = record.isDir === 1
        return (
          <Space>
            {getFileIcon(record)}
            {isDir ? (
              <a onClick={() => handleOpenDir(record)} style={{ fontWeight: 500 }}>{text}</a>
            ) : (
              <span>{text}</span>
            )}
          </Space>
        )
      },
    },
    {
      title: '大小',
      dataIndex: 'size',
      key: 'size',
      width: 130,
      render: (size, record) => {
        if (record.isDir === 1) return '-'
        return formatSize(size)
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 200,
      render: (text) => text ? <span style={{ color: '#666' }}>{text}</span> : <span style={{ color: '#ccc' }}>无</span>,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 200,
      render: (t) => formatTime(t),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_, record) => (
        <Space>
          {record.isDir !== 1 && (
            <>
              <Tooltip title="添加/修改描述">
                <Button type="link" icon={<EditOutlined />} onClick={() => handleDescription(record)} />
              </Tooltip>
              <Tooltip title="分享">
                <Button type="link" icon={<ShareAltOutlined />} onClick={() => handleShare(record)} />
              </Tooltip>
            </>
          )}
          <Popconfirm title="确定删除吗？" onConfirm={() => handleDelete(record.id)}>
            <Tooltip title="删除">
              <Button type="link" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Breadcrumb
          items={pathStack.map((item, index) => ({
            title: index < pathStack.length - 1
              ? <a onClick={() => handleBreadcrumbClick(index)}>{item.name}</a>
              : <Text strong>{item.name}</Text>,
          }))}
        />
        <Space>
          {pathStack.length > 1 && (
            <Button icon={<ArrowUpOutlined />} onClick={() => handleBreadcrumbClick(pathStack.length - 2)}>
              上级目录
            </Button>
          )}
          <Button icon={<FolderAddOutlined />} onClick={() => setDirModalOpen(true)}>新建文件夹</Button>
          <Button type="primary" icon={<CloudUploadOutlined />} onClick={() => setUploadModalOpen(true)}>上传文件</Button>
        </Space>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={files}
        loading={loading}
        pagination={false}
        onRow={(record) => {
          return record.isDir === 1 ? { onDoubleClick: () => handleOpenDir(record), style: { cursor: 'pointer' } } : {}
        }}
      />

      <Modal title="新建文件夹" open={dirModalOpen} onOk={handleCreateDir} onCancel={() => { setDirModalOpen(false); setDirName('') }}>
        <Input placeholder="请输入文件夹名称" value={dirName} onChange={(e) => setDirName(e.target.value)} onPressEnter={handleCreateDir} />
      </Modal>

      <Modal title="上传文件" open={uploadModalOpen} onCancel={() => { if (!uploading) setUploadModalOpen(false) }} footer={null}>
        {uploading ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Progress percent={uploadProgress} style={{ maxWidth: 400 }} />
            <p style={{ marginTop: 16, color: '#888' }}>正在上传，请勿关闭...</p>
          </div>
        ) : (
          <Upload.Dragger customRequest={handleUpload} showUploadList={false} multiple={false} accept="*">
            <p className="ant-upload-drag-icon"><CloudUploadOutlined style={{ fontSize: 48, color: '#1890ff' }} /></p>
            <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
            <p className="ant-upload-hint">支持大文件分片上传、秒传和断点续传</p>
          </Upload.Dragger>
        )}
      </Modal>

      <Modal title="添加描述" open={descModalOpen} onOk={handleDescriptionSave} onCancel={() => { setDescModalOpen(false); setDescValue(''); setDescFileId(null) }}>
        <Input.TextArea rows={3} placeholder="请输入文件描述，用于搜索" value={descValue} onChange={(e) => setDescValue(e.target.value)} />
      </Modal>

      <ShareModal
        open={shareModalOpen}
        fileId={shareFileId}
        onClose={() => { setShareModalOpen(false); setShareFileId(null) }}
      />
    </div>
  )
}
