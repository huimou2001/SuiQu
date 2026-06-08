import request from '../api/request'

export function checkFile(md5) {
  return request.get('/file/check', { params: { md5 } })
}

export function uploadChunk(formData) {
  return request.post('/file/upload-chunk', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function mergeChunks(md5, fileName, totalChunks, parentId) {
  return request.post('/file/merge', { md5, fileName, totalChunks, parentId })
}

export function getFileList(parentId) {
  return request.get('/file/list', { params: { parentId } })
}

export function createDirectory(name, parentId) {
  return request.post('/file/create-dir', null, { params: { name, parentId } })
}

export function deleteFile(id) {
  return request.delete(`/file/${id}`)
}

export function updateDescription(id, description) {
  return request.put(`/file/description/${id}`, null, { params: { description } })
}

export function searchFiles(keyword) {
  return request.get('/search/files', { params: { keyword } })
}
