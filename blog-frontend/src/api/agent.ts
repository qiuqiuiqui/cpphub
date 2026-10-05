import request, { type ApiResult } from '@/utils/request'

export interface DeviceStatus {
  deviceId: string
  name: string
  os: string
  agentVersion: string
  status: 'ONLINE' | 'OFFLINE' | string
  lastSeenAt: string | null
  createdAt: string
}

export interface ToolchainProbe {
  name: string
  category?: string
  available: boolean
  version?: string
  path?: string
  error?: string
  instances?: Array<{
    path: string
    version: string
    source?: string
  }>
}

export interface ToolchainResult {
  command: string
  os: string
  results: ToolchainProbe[]
}

export const createPairingCode = (deviceName?: string) =>
  request.post<any, ApiResult<string>>('/admin/agent/pair-code', {
    deviceName: deviceName ?? 'My PC'
  })

export const listDevices = () =>
  request.get<any, ApiResult<DeviceStatus[]>>('/admin/agent/devices')

export const runToolchainInspect = (deviceId: string, timeoutMs = 20000) =>
  request.post<any, ApiResult<{ result: ToolchainResult }>>(
    `/admin/agent/devices/${deviceId}/run`,
    { command: 'TOOLCHAIN_INSPECT', params: {}, timeoutMs }
  )
