<template>
  <div class="page-card">
    <div class="page-head">
      <div>
        <h2 class="page-title">门岗核验</h2>
        <div class="page-subtitle">扫描访客通行二维码或手动输入通行码，自动登记入校/离校</div>
      </div>
      <el-tag :type="cameraOn ? 'success' : 'info'">{{ cameraOn ? '摄像头已开启' : '摄像头未开启' }}</el-tag>
    </div>

    <el-alert
      v-if="!cameraSupported"
      title="当前浏览器不支持摄像头扫码，请使用 Chrome/Edge，或手动输入通行码"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <div class="gate-grid">
      <el-card shadow="never">
        <template #header>
          <div class="card-head">
            <span>扫码核验</span>
            <el-button v-if="!cameraOn" type="primary" :icon="VideoCamera" @click="startCamera">开启摄像头</el-button>
            <el-button v-else type="danger" :icon="VideoCameraFilled" @click="stopCamera">关闭摄像头</el-button>
          </div>
        </template>
        <div class="video-wrap" :class="{ active: cameraOn }">
          <video ref="videoRef" muted playsinline />
          <div v-if="!cameraOn" class="video-placeholder">
            <el-icon :size="48"><Camera /></el-icon>
            <p>点击「开启摄像头」后，将访客二维码对准镜头</p>
          </div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header><span>手动核验</span></template>
        <el-form label-position="top">
          <el-form-item label="通行码">
            <el-input
              v-model.trim="passCode"
              size="large"
              clearable
              placeholder="如 DEMO-PASS-20261111-0001"
              @keyup.enter="manualCheck"
            />
          </el-form-item>
          <el-button type="primary" size="large" :loading="checking" style="width: 100%" @click="manualCheck">
            核验并登记
          </el-button>
        </el-form>

        <div v-if="result" class="check-result" :class="result.direction === 'IN' ? 'in' : 'out'">
          <div class="result-title">
            <el-icon :size="26"><CircleCheckFilled /></el-icon>
            {{ result.direction === 'IN' ? '入校核验通过' : '离校核验通过' }}
          </div>
          <p class="result-name">{{ result.visitorName }}</p>
          <p class="result-msg">{{ result.message }}</p>
          <p v-if="result.visitUserName" class="result-meta">被访人：{{ result.visitUserName }}</p>
          <p v-if="result.passExpireTime" class="result-meta">通行码有效期至：{{ result.passExpireTime }}</p>
        </div>
      </el-card>
    </div>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="card-head">
          <span>最近通行记录</span>
          <el-button link type="primary" :icon="Refresh" @click="loadRecords">刷新</el-button>
        </div>
      </template>
      <el-table :data="records" stripe border size="small">
        <el-table-column prop="visitorName" label="访客" min-width="110" />
        <el-table-column prop="passCode" label="通行码" min-width="200" show-overflow-tooltip />
        <el-table-column prop="gate" label="校门" width="110" />
        <el-table-column prop="inTime" label="入校时间" width="165" />
        <el-table-column prop="outTime" label="离校时间" width="165" />
        <el-table-column prop="operator" label="操作人" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'IN' ? 'warning' : 'success'" size="small">
              {{ row.status === 'IN' ? '在校' : '已离校' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, CircleCheckFilled, Refresh, VideoCamera, VideoCameraFilled } from '@element-plus/icons-vue'
import http from '@/api/http'

const videoRef = ref<HTMLVideoElement | null>(null)
const cameraOn = ref(false)
const cameraSupported = ref(typeof window !== 'undefined' && 'BarcodeDetector' in window)
const passCode = ref('')
const checking = ref(false)
const result = ref<any>(null)
const records = ref<any[]>([])

let stream: MediaStream | null = null
let timer: number | null = null
let lastCode = ''
let lastTime = 0
let detector: any = null

async function startCamera() {
  if (!cameraSupported.value) {
    ElMessage.warning('当前浏览器不支持摄像头扫码，请手动输入通行码')
    return
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
    cameraOn.value = true
    await new Promise((resolve) => setTimeout(resolve, 50))
    const video = videoRef.value
    if (!video) return
    video.srcObject = stream
    await video.play()
    const BarcodeDetector = (window as any).BarcodeDetector
    detector = new BarcodeDetector({ formats: ['qr_code'] })
    timer = window.setInterval(detectFrame, 500)
  } catch (e: any) {
    ElMessage.error(e?.message || '无法打开摄像头，请检查浏览器权限')
    stopCamera()
  }
}

function stopCamera() {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
  stream?.getTracks().forEach((track) => track.stop())
  stream = null
  if (videoRef.value) videoRef.value.srcObject = null
  cameraOn.value = false
}

async function detectFrame() {
  const video = videoRef.value
  if (!detector || !video || video.readyState < 2) return
  try {
    const codes = await detector.detect(video)
    if (codes && codes.length) {
      const value = String(codes[0].rawValue || '').trim()
      if (value) handleCode(value)
    }
  } catch (e) {
    // 单帧识别失败忽略，等待下一帧
  }
}

function manualCheck() {
  if (!passCode.value) {
    ElMessage.warning('请输入通行码')
    return
  }
  handleCode(passCode.value)
}

function handleCode(code: string) {
  const now = Date.now()
  if (code === lastCode && now - lastTime < 3000) return
  lastCode = code
  lastTime = now
  check(code)
}

async function check(code: string) {
  checking.value = true
  try {
    const data: any = await http.post('/visitor/check', { passCode: code, gate: '后台门岗', method: 'QR' })
    result.value = data
    passCode.value = ''
    ElMessage.success(data.direction === 'IN' ? '已登记入校' : '已登记离校')
    loadRecords()
  } catch (e: any) {
    result.value = null
    ElMessage.error(e?.message || '核验失败')
  } finally {
    checking.value = false
  }
}

async function loadRecords() {
  try {
    const data: any = await http.get('/visitor/records', { params: { page: 1, size: 10 } })
    records.value = data.records || []
  } catch (e) {
    records.value = []
  }
}

onMounted(loadRecords)
onUnmounted(stopCamera)
</script>

<style scoped>
.page-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.gate-grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 16px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.video-wrap {
  position: relative; background: #0b1b33; border-radius: 12px; overflow: hidden;
  aspect-ratio: 4 / 3; display: grid; place-items: center;
}
.video-wrap video { width: 100%; height: 100%; object-fit: cover; display: none; }
.video-wrap.active video { display: block; }
.video-placeholder { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: #94a3b8; text-align: center; padding: 20px; }
.video-placeholder p { margin: 0; font-size: 13px; }
.check-result { margin-top: 18px; border-radius: 12px; padding: 16px; border: 1px solid #c9efdd; background: #f0fbf6; }
.check-result.out { border-color: #d6e8ff; background: #f1f7ff; }
.result-title { display: flex; align-items: center; gap: 8px; font-weight: 700; color: #0f9d63; font-size: 15px; }
.check-result.out .result-title { color: #2563eb; }
.result-name { margin: 10px 0 4px; font-size: 22px; font-weight: 700; color: #0f2a52; }
.result-msg { margin: 0; font-size: 13px; color: #334155; }
.result-meta { margin: 6px 0 0; font-size: 12px; color: #64748b; }
@media (max-width: 900px) { .gate-grid { grid-template-columns: 1fr; } }
</style>
