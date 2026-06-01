import request from '../api/request'

export function createShare(fileId, expireHours) {
  return request.post('/share/create', { fileId, expireHours })
}

export function getShareInfo(shareId) {
  return request.get(`/share/${shareId}`)
}
