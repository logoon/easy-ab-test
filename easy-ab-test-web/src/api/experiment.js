import request from '@/utils/request'

export function getExperimentsByService(serviceId) {
  return request({
    url: `/experiments/service/${serviceId}`,
    method: 'get'
  })
}

export function getRunningExperiments(serviceId) {
  return request({
    url: `/experiments/service/${serviceId}/running`,
    method: 'get'
  })
}

export function getExperiment(id) {
  return request({
    url: `/experiments/${id}`,
    method: 'get'
  })
}

export function createExperiment(data) {
  return request({
    url: '/experiments',
    method: 'post',
    data
  })
}

export function updateExperiment(id, data) {
  return request({
    url: `/experiments/${id}`,
    method: 'put',
    data
  })
}

export function deleteExperiment(id) {
  return request({
    url: `/experiments/${id}`,
    method: 'delete'
  })
}

export function updateExperimentStatus(id, status) {
  return request({
    url: `/experiments/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function getExperimentGroup(experimentId, userId) {
  return request({
    url: `/experiments/${experimentId}/group`,
    method: 'get',
    params: { userId }
  })
}
