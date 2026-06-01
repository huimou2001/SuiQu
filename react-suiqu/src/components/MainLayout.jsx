import { useState } from 'react'
import { Layout, Menu, Input, Avatar, Dropdown, message } from 'antd'
import {
  FolderOutlined,
  SearchOutlined,
  LogoutOutlined,
  UserOutlined,
  CloudOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { removeToken } from '../utils/auth'

const { Header, Sider, Content } = Layout
const { Search } = Input

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const navigate = useNavigate()
  const location = useLocation()

  const menuKey = location.pathname === '/search' ? '/search' : '/files'

  const handleLogout = () => {
    removeToken()
    message.success('已退出登录')
    navigate('/login')
  }

  const handleSearch = (value) => {
    if (value.trim()) {
      setSearchKeyword(value.trim())
      navigate('/search')
    }
  }

  const handleMenuClick = ({ key }) => {
    if (key === '/files') {
      setSearchKeyword('')
    }
    navigate(key)
  }

  const userMenu = {
    items: [
      { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: handleLogout },
    ],
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed} theme="light">
        <div style={{
          height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center',
          borderBottom: '1px solid #f0f0f0',
        }}>
          <CloudOutlined style={{ fontSize: 28, color: '#1890ff' }} />
          {!collapsed && <span style={{ marginLeft: 10, fontSize: 18, fontWeight: 700, color: '#333' }}>SuiQu</span>}
        </div>
        <Menu mode="inline" selectedKeys={[menuKey]} onClick={handleMenuClick} items={[
          { key: '/files', icon: <FolderOutlined />, label: '我的文件' },
          { key: '/search', icon: <SearchOutlined />, label: '搜索文件' },
        ]} />
      </Sider>

      <Layout>
        <Header style={{
          background: '#fff', padding: '0 24px', display: 'flex',
          justifyContent: 'space-between', alignItems: 'center',
          borderBottom: '1px solid #f0f0f0', height: 64,
        }}>
          <Search
            placeholder="搜索文件..."
            allowClear
            onSearch={handleSearch}
            style={{ maxWidth: 400, width: '100%' }}
            size="middle"
          />
          <Dropdown menu={userMenu} placement="bottomRight">
            <Avatar icon={<UserOutlined />} style={{ cursor: 'pointer', backgroundColor: '#1890ff' }} />
          </Dropdown>
        </Header>

        <Content style={{ background: '#fff', minHeight: 280 }}>
          <Outlet context={{ searchKeyword, setSearchKeyword }} />
        </Content>
      </Layout>
    </Layout>
  )
}
