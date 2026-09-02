<template>
  <n-card title="C++ 环境检测">
    <n-space style="margin-bottom: 16px;" align="center">
      <n-tag :type="anyOnline ? 'success' : 'warning'">
        {{ anyOnline ? '● 设备在线' : '○ 设备离线' }}
      </n-tag>
      <span v-if="firstDevice" style="color: #888;">
        设备: {{ firstDevice.name }} ({{ firstDevice.os }})
      </span>
      <n-button @click="loadDevices" size="small">刷新状态</n-button>
      <n-button @click="loadPairingCode" size="small">生成配对码</n-button>
      <n-button type="primary" :loading="running" :disabled="!firstDevice" @click="runInspect">
        开始检测
      </n-button>
    </n-space>

    <n-alert v-if="pairingCode" type="info" title="配对码" closable style="margin-bottom: 16px;">
      <div style="font-size: 22px; font-weight: 700; letter-spacing: 4px; color: #2080f0;">
        {{ pairingCode }}
      </div>
      <p style="margin: 8px 0 0 0; color: #666; font-size: 13px;">
        在本机终端执行：
        <code style="background: #f0f0f0; padding: 2px 6px;">blog-agent pair --code {{ pairingCode }}</code>
        ，配对有效期 10 分钟。
      </p>
    </n-alert>

    <n-spin :show="running">
      <div v-if="results.length > 0" class="probe-grid">
        <div
          v-for="p in results"
          :key="p.name"
          class="probe-card"
          :class="{ ok: p.available, fail: !p.available }"
        >
          <div class="probe-header">
            <span class="probe-name">{{ p.name }}</span>
            <n-tag :type="p.available ? 'success' : 'default'" size="small">
              {{ p.available ? '已安装' : '未安装' }}
            </n-tag>
          </div>
          <div v-if="p.available" class="probe-body">
            <div class="version">{{ p.version }}</div>
            <div class="path" :title="p.path">{{ p.path }}</div>
          </div>
          <div v-else class="probe-body error">
            <div class="version">{{ p.error }}</div>
          </div>
        </div>
      </div>
      <n-empty v-else description="点击「开始检测」扫描本机 C++ 工具链" />
    </n-spin>

    <n-divider />
    <h3 style="margin: 0 0 12px; font-size: 16px;">我的设备</h3>
    <n-spin :show="devicesLoading">
      <n-data-table
        :columns="deviceColumns"
        :data="devices"
        :bordered="false"
        :pagination="false"
        size="small"
      />
    </n-spin>
  </n-card>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import {
  NCard, NSpace, NButton, NTag, NAlert, NSpin, NDataTable, NEmpty, NDivider,
  useMessage, type DataTableColumns
} from 'naive-ui'
import {
  createPairingCode,
  listDevices,
  runToolchainInspect,
  type DeviceStatus,
  type ToolchainProbe
} from '@/api/agent'

const message = useMessage()
const devices = ref<DeviceStatus[]>([])
const devicesLoading = ref(false)
const firstDevice = computed(() => devices.value[0] ?? null)
const anyOnline = computed(() => devices.value.some((d) => d.status === 'ONLINE'))

const pairingCode = ref<string | null>(null)
const running = ref(false)
const results = ref<ToolchainProbe[]>([])

async function loadDevices() {
  devicesLoading.value = true
  try {
    const res = await listDevices()
    devices.value = res.data
  } finally {
    devicesLoading.value = false
  }
}

async function loadPairingCode() {
  try {
    const res = await createPairingCode('My Workstation')
    pairingCode.value = res.data
    message.success('配对码已生成（10 分钟内有效）')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '生成失败')
  }
}

async function runInspect() {
  if (!firstDevice.value) {
    message.warning('暂无设备，请先生成配对码并配对本机 Agent')
    return
  }
  running.value = true
  results.value = []
  try {
    const res = await runToolchainInspect(firstDevice.value.deviceId, 20000)
    results.value = res.data.result.results ?? []
    message.success(`检测完成，共 ${results.value.length} 项`)
  } catch (e: any) {
    message.error(e?.response?.data?.message || '检测失败')
  } finally {
    running.value = false
  }
}

const deviceColumns: DataTableColumns<DeviceStatus> = [
  { title: '设备名', key: 'name' },
  {
    title: '状态',
    key: 'status',
    render: (row) =>
      h(NTag, { type: row.status === 'ONLINE' ? 'success' : 'default', size: 'small' }, { default: () => row.status })
  },
  { title: '系统', key: 'os', width: 100 },
  { title: 'Agent 版本', key: 'agentVersion', width: 120 },
  {
    title: '最后心跳',
    key: 'lastSeenAt',
    render: (row) => (row.lastSeenAt ? new Date(row.lastSeenAt).toLocaleString('zh-CN') : '—')
  }
]

onMounted(loadDevices)
</script>

<style scoped>
.probe-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}
.probe-card {
  border: 1px solid #e0e0e6;
  border-radius: 8px;
  padding: 12px 16px;
  background: #fff;
  transition: border-color 0.2s;
}
.probe-card.ok {
  border-left: 4px solid #18a058;
}
.probe-card.fail {
  border-left: 4px solid #999;
  opacity: 0.7;
}
.probe-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.probe-name {
  font-weight: 600;
  font-size: 15px;
}
.probe-body .version {
  font-family: 'Consolas', monospace;
  font-size: 13px;
  color: #2c3e50;
  word-break: break-all;
}
.probe-body.error .version {
  color: #999;
  font-size: 12px;
}
.probe-body .path {
  margin-top: 4px;
  font-size: 11px;
  color: #888;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>