import request from '@/utils/request'

export function getServices() {
  return request({
    url: '/services',
    method: 'get'
  })
}

export function getService(id) {
  return request({
    url: `/services/${id}`,
    method: 'get'
  })
}

export function getServiceByCode(code) {
  return request({
    url: `/services/code/${code}`,
    method: 'get'
  })
}

export function createService(data) {
  return request({
    url: '/services',
    method: 'post',
    data
  })
}

export function updateService(id, data) {
  return request({
    url: `/services/${id}`,
    method: 'put',
    data
  })
}

export function deleteService(id) {
  return request({
    url: `/services/${id}`,
    method: 'delete'
  })
}
