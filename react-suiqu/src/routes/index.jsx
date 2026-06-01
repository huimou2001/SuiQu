import { Routes, Route, Navigate, useOutletContext } from 'react-router-dom'
import { isLoggedIn } from '../utils/auth'
import LoginPage from '../pages/LoginPage'
import MainLayout from '../components/MainLayout'
import FilePage from '../pages/FilePage'
import SearchPage from '../pages/SearchPage'
import SharePage from '../pages/SharePage'

function ProtectedRoute({ children }) {
  if (!isLoggedIn()) {
    return <Navigate to="/login" replace />
  }
  return children
}

function SearchPageWrapper() {
  const context = useOutletContext()
  return <SearchPage keyword={context.searchKeyword} />
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={
        isLoggedIn() ? <Navigate to="/files" replace /> : <LoginPage />
      } />
      <Route path="/share/:shareId" element={<SharePage />} />
      <Route path="/" element={
        <ProtectedRoute><MainLayout /></ProtectedRoute>
      }>
        <Route index element={<Navigate to="/files" replace />} />
        <Route path="files" element={<FilePage />} />
        <Route path="search" element={<SearchPageWrapper />} />
      </Route>
      <Route path="*" element={<Navigate to="/files" replace />} />
    </Routes>
  )
}
