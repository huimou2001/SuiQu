import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Descriptions, Tag, Result, Button, Spin } from 'antd'
import {
  FileOutlined, FolderOpenOutlined, ClockCircleOutlined,
  ShareAltOutlined, DownloadOutlined,
} from '@ant-design/icons'
import { getShareInfo } from '../api/share'

function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

export default function SharePage() {
  const { shareId } = useParams()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [share, setShare] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!shareId) return
    setLoading(true)
    getShareInfo(shareId)
      .then((data) => setShare(data))
      .catch((err) => setError(err.message || '获取分享信息失败'))
      .finally(() => setLoading(false))
  }, [shareId])

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <Spin size="large" tip="加载中..." />
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <Result
          status="404"
          title="分享不存在或已失效"
          subTitle={error}
          extra={<Button type="primary" onClick={() => navigate('/login')}>返回登录</Button>}
        />
      </div>
    )
  }

  const isExpired = share?.expireTime && new Date(share.expireTime) < new Date()

  return (
    <div style={{
      display: 'flex', justifyContent: 'center', alignItems: 'center',
      minHeight: '100vh', background: '#f0f2f5',
    }}>
      <Card
        style={{ width: 520, borderRadius: 12, boxShadow: '0 4px 20px rgba(0,0,0,0.08)' }}
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <ShareAltOutlined style={{ fontSize: 20, color: '#1890ff' }} />
            <span>文件分享</span>
          </div>
        }
      >
        {isExpired ? (
          <Result status="warning" title="分享链接已过期" subTitle="该分享链接已过期，请联系分享者重新分享" />
        ) : (
          <>
            <div style={{ textAlign: 'center', padding: '24px 0' }}>
              {share.fileType === 'folder' || share.fileType === 'DIRECTORY'
                ? <FolderOpenOutlined style={{ fontSize: 64, color: '#faad14' }} />
                : <FileOutlined style={{ fontSize: 64, color: '#1890ff' }} />
              }
              <h3 style={{ marginTop: 16, fontSize: 20 }}>{share.fileName || '未知文件'}</h3>
            </div>

            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="文件名">{share.fileName}</Descriptions.Item>
              <Descriptions.Item label="文件大小">{formatSize(share.fileSize)}</Descriptions.Item>
              <Descriptions.Item label="文件类型">
                <Tag color="blue">{share.fileType || '未知'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="过期时间">
                <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                  <ClockCircleOutlined />
                  {share.expireTime || '永久有效'}
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color="green">有效</Tag>
              </Descriptions.Item>
            </Descriptions>
          </>
        )}
      </Card>
    </div>
  )
}
