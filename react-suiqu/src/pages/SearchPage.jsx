import { useState, useEffect } from 'react'
import { Table, Input, Tag, Empty, Spin } from 'antd'
import { FileOutlined, FolderOpenOutlined, SearchOutlined } from '@ant-design/icons'
import { searchFiles } from '../api/file'

function getFileIcon(type) {
  if (type === 'folder' || type === 'DIRECTORY') return <FolderOpenOutlined style={{ color: '#faad14', fontSize: 20 }} />
  return <FileOutlined style={{ color: '#1890ff', fontSize: 20 }} />
}

function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

export default function SearchPage({ keyword }) {
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)

  useEffect(() => {
    if (!keyword?.trim()) {
      setResults([])
      setSearched(false)
      return
    }

    setLoading(true)
    setSearched(true)
    searchFiles(keyword.trim())
      .then((data) => setResults(Array.isArray(data) ? data : []))
      .catch(() => setResults([]))
      .finally(() => setLoading(false))
  }, [keyword])

  const columns = [
    {
      title: '文件名',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {getFileIcon(record.type || record.fileType)}
          {text}
        </span>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      render: (text) => text || <Tag>无</Tag>,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 200,
      render: (t) => t || '-',
    },
  ]

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" tip="搜索中..." /></div>
  }

  if (!searched) {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <SearchOutlined style={{ fontSize: 64, color: '#d9d9d9' }} />
        <p style={{ marginTop: 16, color: '#999', fontSize: 16 }}>输入关键词搜索文件</p>
      </div>
    )
  }

  return (
    <div style={{ padding: 24 }}>
      <h3 style={{ marginBottom: 16 }}>
        搜索 "<span style={{ color: '#1890ff' }}>{keyword}</span>" 的结果（共 {results.length} 条）
      </h3>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={results}
        locale={{ emptyText: <Empty description="没有找到匹配的文件" /> }}
        pagination={{ pageSize: 20 }}
      />
    </div>
  )
}
