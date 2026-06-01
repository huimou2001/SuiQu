import request from '../api/request'

export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

export function register(username, password, email) {
  return request.post('/auth/register', { username, password, email })
}
