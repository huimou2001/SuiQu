import { useState } from 'react'
import { Modal, Form, InputNumber, message, Typography, Input, Space, Button } from 'antd'
import { CopyOutlined } from '@ant-design/icons'
import { createShare } from '../api/share'

const { Text } = Typography

export default function ShareModal({ open, fileId, onClose }) {
  const [loading, setLoading] = useState(false)
  const [shareResult, setShareResult] = useState(null)

  const handleCreate = async (values) => {
    setLoading(true)
    try {
      const data = await createShare(fileId, values.expireHours)
      setShareResult(data)
      message.success('分享创建成功')
    } catch {
      // handled
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    setShareResult(null)
    onClose()
  }

  const copyLink = () => {
    if (shareResult?.shareUrl) {
      navigator.clipboard.writeText(shareResult.shareUrl)
      message.success('链接已复制')
    }
  }

  return (
    <Modal
      title="创建分享链接"
      open={open}
      onCancel={handleClose}
      footer={shareResult ? <Button onClick={handleClose}>关闭</Button> : null}
    >
      {shareResult ? (
        <div style={{ padding: '16px 0' }}>
          <p><Text strong>分享链接：</Text></p>
          <Space.Compact style={{ width: '100%' }}>
            <Input value={shareResult.shareUrl} readOnly />
            <Button icon={<CopyOutlined />} onClick={copyLink}>复制</Button>
          </Space.Compact>
          <p style={{ marginTop: 16 }}>
            <Text type="secondary">过期时间：{shareResult.expireTime || '永久'}</Text>
          </p>
        </div>
      ) : (
        <Form onFinish={handleCreate} layout="vertical" initialValues={{ expireHours: 24 }}>
          <Form.Item label="有效时长（小时）" name="expireHours" rules={[{ required: true }]}>
            <InputNumber min={1} max={720} style={{ width: '100%' }} />
          </Form.Item>
          <div style={{ textAlign: 'right' }}>
            <Space>
              <Button onClick={handleClose}>取消</Button>
              <Button type="primary" htmlType="submit" loading={loading}>创建分享</Button>
            </Space>
          </div>
        </Form>
      )}
    </Modal>
  )
}
