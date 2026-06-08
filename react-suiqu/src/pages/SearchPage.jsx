import { useState, useEffect } from 'react'
import { Table, Tag, Empty, Spin } from 'antd'
import { FileOutlined, SearchOutlined } from '@ant-design/icons'
import { searchFiles } from '../api/file'

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
      dataIndex: 'file_name',
      key: 'file_name',
      render: (text) => (
        <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <FileOutlined style={{ color: '#1890ff', fontSize: 20 }} />
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
      render: (t) => formatTime(t),
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
