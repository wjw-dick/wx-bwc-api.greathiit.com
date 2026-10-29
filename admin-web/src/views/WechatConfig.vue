<template>
  <div v-loading="loading">
    <div class="page-card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">微信配置</h2>
          <div class="page-subtitle">公众号 / 小程序 · 合法域名 wx-bwc-api.greathiit.com · AppSecret 加密存储</div>
        </div>
        <div class="spacer" />
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-alert type="success" show-icon :closable="false" class="mb-12"
        title="微信公众平台服务器配置"
        description="URL：https://wx-bwc-api.greathiit.com/api/wechat/callback ；Token 与 EncodingAESKey 需与公众号后台一致。" />

      <el-table :data="configs" border stripe>
        <el-table-column prop="appType" label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ row.appType === 'MINI' ? '小程序' : '公众号' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="appId" label="AppID" min-width="180" />
        <el-table-column prop="appSecretMask" label="AppSecret" min-width="140" />
        <el-table-column prop="serverUrl" label="服务器地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="authDomain" label="业务域名" min-width="190" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="edit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-row :gutter="16" class="mt-16">
      <el-col :xs="24" :lg="14">
        <div class="page-card">
          <h3 class="section-title">{{ form.id ? '编辑微信配置' : '新增微信配置' }}</h3>
          <el-form :model="form" label-position="top">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="应用类型">
                  <el-select v-model="form.appType" class="full">
                    <el-option label="公众号" value="MP" />
                    <el-option label="小程序" value="MINI" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="AppID"><el-input v-model="form.appId" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="AppSecret">
                  <el-input v-model="form.appSecret" type="password" show-password placeholder="留空或保持掩码不修改" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="Token"><el-input v-model="form.token" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="EncodingAESKey">
                  <el-input v-model="form.aesKey" type="password" show-password placeholder="留空不修改" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="服务器地址">
                  <el-input v-model="form.serverUrl" placeholder="https://wx-bwc-api.greathiit.com/api/wechat/callback" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="业务域名">
                  <el-input v-model="form.authDomain" placeholder="wx-bwc-api.greathiit.com" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="JS 安全域名">
                  <el-input v-model="form.jsDomain" placeholder="wx-bwc-api.greathiit.com" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
              </el-col>
            </el-row>
            <el-button type="primary" :loading="saving" :icon="Check" @click="save">保存配置</el-button>
          </el-form>
        </div>
      </el-col>
      <el-col :xs="24" :lg="10">
        <div class="page-card">
          <h3 class="section-title">公众号菜单同步</h3>
          <p class="muted">粘贴微信菜单 JSON（button 数组），同步失败时会返回明确错误，不影响系统运行。</p>
          <el-input v-model="menuJson" type="textarea" :rows="12" placeholder='{"button":[{"type":"view","name":"校园门户","url":"https://wx-bwc-api.greathiit.com/h5/"}]}' />
          <div class="flex mt-16">
            <el-button type="primary" :icon="Upload" :loading="syncing" @click="syncMenu">同步到微信</el-button>
            <el-button :icon="Document" @click="fillDefault">填充示例</el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card mt-16">
      <div class="toolbar">
        <div>
          <h3 class="section-title" style="margin:0">公众号运营 · 素材 / 标签 / 群发 / 订阅消息</h3>
          <div class="page-subtitle">真实调用微信接口，需先完成公众号认证、IP 白名单与素材权限配置</div>
        </div>
        <div class="spacer" />
        <el-select v-model="opsAppType" style="width:120px">
          <el-option label="公众号" value="MP" />
          <el-option label="小程序" value="MINI" />
        </el-select>
      </div>

      <el-tabs v-model="opsTab" @tab-change="onOpsTabChange">
        <el-tab-pane label="永久素材" name="material">
          <div class="flex mb-12" style="gap:8px;flex-wrap:wrap;align-items:center">
            <el-select v-model="materialType" style="width:120px">
              <el-option label="图片 image" value="image" />
              <el-option label="语音 voice" value="voice" />
              <el-option label="视频 video" value="video" />
              <el-option label="缩略图 thumb" value="thumb" />
            </el-select>
            <el-upload :show-file-list="false" :http-request="uploadMaterial">
              <el-button type="primary" :loading="uploading" :icon="UploadFilled">上传素材</el-button>
            </el-upload>
            <el-button :icon="Refresh" :loading="materialLoading" @click="loadMaterials">刷新列表</el-button>
          </div>
          <el-table :data="materials" border stripe size="small">
            <el-table-column prop="media_id" label="media_id" min-width="260" show-overflow-tooltip />
            <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="更新时间" width="180">
              <template #default="{ row }">{{ formatTime(row.update_time) }}</template>
            </el-table-column>
            <el-table-column prop="url" label="URL" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" @click="removeMaterial(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!materialLoading && !materials.length" description="暂无素材，或尚未配置微信 AppID/AppSecret" />
        </el-tab-pane>

        <el-tab-pane label="粉丝标签" name="tag">
          <div class="flex mb-12" style="gap:8px;flex-wrap:wrap;align-items:center">
            <el-input v-model="newTagName" placeholder="新标签名称" style="width:200px" />
            <el-button type="primary" :loading="tagSaving" @click="createTag">创建标签</el-button>
            <el-button :icon="Refresh" :loading="tagLoading" @click="loadTags">刷新</el-button>
          </div>
          <el-table :data="tags" border stripe size="small">
            <el-table-column prop="id" label="标签 ID" width="120" />
            <el-table-column prop="name" label="标签名称" min-width="180" />
            <el-table-column prop="count" label="粉丝数" width="120" />
          </el-table>
          <div class="flex mt-12" style="gap:8px;flex-wrap:wrap">
            <el-select v-model="tagFanForm.tagId" placeholder="选择标签" style="width:180px">
              <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
            <el-input v-model="tagFanForm.openids" placeholder="OpenID，多个用英文逗号分隔" style="width:360px" />
            <el-button :loading="tagFanSaving" @click="tagFans">批量打标签</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="按标签群发" name="mass">
          <el-form :model="massForm" label-position="top">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="群发范围">
                  <el-select v-model="massForm.tagId" clearable placeholder="全部粉丝" class="full">
                    <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="消息类型">
                  <el-select v-model="massForm.msgType" class="full">
                    <el-option label="文本 text" value="text" />
                    <el-option label="图文 mpnews" value="mpnews" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item :label="massForm.msgType === 'text' ? '文本内容' : '图文素材 media_id'">
              <el-input v-model="massForm.content" type="textarea" :rows="4" placeholder="群发前请确认已获得内容审核与用户授权" />
            </el-form-item>
            <el-button type="primary" :loading="massSending" @click="massSend">确认群发</el-button>
            <span class="muted" style="margin-left:12px">群发不可撤回，测试环境请使用测试号</span>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="订阅消息" name="subscribe">
          <el-form :model="subscribeForm" label-position="top">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="OpenID"><el-input v-model="subscribeForm.openid" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="模板 ID"><el-input v-model="subscribeForm.templateId" /></el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="跳转页面"><el-input v-model="subscribeForm.page" placeholder="pages/index/index" /></el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="模板数据 JSON">
                  <el-input v-model="subscribeForm.dataJson" type="textarea" :rows="6"
                    placeholder='{"thing1":{"value":"校园安全通知"},"time3":{"value":"2026-10-29 10:00"}}' />
                </el-form-item>
              </el-col>
            </el-row>
            <el-button type="primary" :loading="subscribeSending" @click="sendSubscribe">发送订阅消息</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Document, Refresh, Upload, UploadFilled } from '@element-plus/icons-vue'
import http from '@/api/http'

const loading = ref(false)
const saving = ref(false)
const syncing = ref(false)
const configs = ref<any[]>([])
const menuJson = ref('')
const opsAppType = ref('MP')
const opsTab = ref('material')
const materialType = ref('image')
const materials = ref<any[]>([])
const materialLoading = ref(false)
const uploading = ref(false)
const tags = ref<any[]>([])
const tagLoading = ref(false)
const tagSaving = ref(false)
const newTagName = ref('')
const tagFanSaving = ref(false)
const tagFanForm = reactive<any>({ tagId: null, openids: '' })
const massForm = reactive<any>({ tagId: null, msgType: 'text', content: '' })
const massSending = ref(false)
const subscribeSending = ref(false)
const subscribeForm = reactive<any>({
  openid: '', templateId: '', page: 'pages/index/index',
  dataJson: '{\n  "thing1": {"value": "校园安全通知"},\n  "thing2": {"value": "请及时查看详情"},\n  "time3": {"value": ""}\n}'
})
const form = reactive<any>({
  id: null, appType: 'MP', appId: '', appSecret: '', token: '', aesKey: '',
  serverUrl: 'https://wx-bwc-api.greathiit.com/api/wechat/callback',
  authDomain: 'wx-bwc-api.greathiit.com', jsDomain: 'wx-bwc-api.greathiit.com', remark: ''
})

async function load() {
  loading.value = true
  try {
    configs.value = (await http.get('/system/wechat/configs')) as any
  } finally {
    loading.value = false
  }
}

function edit(row: any) {
  Object.assign(form, {
    ...row,
    appSecret: row.appSecretMask || '',
    aesKey: ''
  })
}

async function save() {
  saving.value = true
  try {
    await http.put('/system/wechat/configs', form)
    ElMessage.success('微信配置已保存')
    await load()
  } finally {
    saving.value = false
  }
}

function fillDefault() {
  menuJson.value = JSON.stringify({
    button: [
      { type: 'view', name: '校园门户', url: 'https://wx-bwc-api.greathiit.com/h5/' },
      { type: 'view', name: '访客预约', url: 'https://wx-bwc-api.greathiit.com/h5/visitor' },
      { name: '更多服务', sub_button: [
        { type: 'view', name: '失物招领', url: 'https://wx-bwc-api.greathiit.com/h5/lost' },
        { type: 'view', name: '安全课堂', url: 'https://wx-bwc-api.greathiit.com/h5/education' }
      ] }
    ]
  }, null, 2)
}

async function syncMenu() {
  if (!menuJson.value.trim()) {
    ElMessage.warning('请先填写菜单 JSON')
    return
  }
  let menu: any
  try {
    menu = JSON.parse(menuJson.value)
  } catch (e) {
    ElMessage.error('菜单 JSON 格式不正确')
    return
  }
  syncing.value = true
  try {
    const result: any = await http.post('/system/wechat/menu/sync', { appType: form.appType, menu })
    ElMessage[result.success ? 'success' : 'warning'](result.message)
  } finally {
    syncing.value = false
  }
}

async function loadMaterials() {
  materialLoading.value = true
  try {
    const result: any = await http.get('/system/wechat/materials', {
      params: { type: materialType.value, offset: 0, count: 20, appType: opsAppType.value }
    })
    materials.value = result?.item || []
  } finally {
    materialLoading.value = false
  }
}

async function uploadMaterial(options: any) {
  if (opsAppType.value !== 'MP') {
    ElMessage.warning('永久素材仅公众号支持，请切换为公众号')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    const result: any = await http.post(
      `/system/wechat/materials/upload?type=${materialType.value}&appType=MP`,
      formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success('素材已上传，media_id：' + (result?.media_id || ''))
    await loadMaterials()
  } finally {
    uploading.value = false
  }
}

async function removeMaterial(row: any) {
  await http.delete(`/system/wechat/materials/${encodeURIComponent(row.media_id)}`, {
    params: { appType: opsAppType.value }
  })
  ElMessage.success('素材已删除')
  await loadMaterials()
}

async function loadTags() {
  tagLoading.value = true
  try {
    tags.value = (await http.get('/system/wechat/tags', {
      params: { appType: opsAppType.value }
    })) as any
  } finally {
    tagLoading.value = false
  }
}

async function createTag() {
  if (!newTagName.value.trim()) {
    ElMessage.warning('请填写标签名称')
    return
  }
  tagSaving.value = true
  try {
    await http.post('/system/wechat/tags', { name: newTagName.value.trim(), appType: opsAppType.value })
    ElMessage.success('标签已创建')
    newTagName.value = ''
    await loadTags()
  } finally {
    tagSaving.value = false
  }
}

async function tagFans() {
  const openids = tagFanForm.openids.split(',').map((s: string) => s.trim()).filter(Boolean)
  if (!tagFanForm.tagId || !openids.length) {
    ElMessage.warning('请选择标签并填写 OpenID')
    return
  }
  tagFanSaving.value = true
  try {
    await http.post('/system/wechat/tags/fans', {
      tagId: tagFanForm.tagId, openids, appType: opsAppType.value
    })
    ElMessage.success('已提交打标签请求')
  } finally {
    tagFanSaving.value = false
  }
}

async function massSend() {
  if (!massForm.content.trim()) {
    ElMessage.warning('请填写群发内容')
    return
  }
  massSending.value = true
  try {
    await http.post('/system/wechat/mass/send', {
      tagId: massForm.tagId || null, msgType: massForm.msgType,
      content: massForm.content.trim(), appType: opsAppType.value
    })
    ElMessage.success('群发任务已提交微信')
  } finally {
    massSending.value = false
  }
}

async function sendSubscribe() {
  if (!subscribeForm.openid.trim() || !subscribeForm.templateId.trim()) {
    ElMessage.warning('请填写 OpenID 与模板 ID')
    return
  }
  let data: any
  try {
    data = JSON.parse(subscribeForm.dataJson)
  } catch (e) {
    ElMessage.error('模板数据 JSON 格式不正确')
    return
  }
  subscribeSending.value = true
  try {
    const result: any = await http.post('/system/wechat/subscribe/send', {
      openid: subscribeForm.openid.trim(), templateId: subscribeForm.templateId.trim(),
      page: subscribeForm.page, data
    })
    ElMessage[result?.success ? 'success' : 'warning'](result?.message || '已提交')
  } finally {
    subscribeSending.value = false
  }
}

function formatTime(value: number) {
  if (!value) return '-'
  return new Date(value * 1000).toLocaleString()
}

function onOpsTabChange(name: any) {
  if (name === 'material') loadMaterials()
  if (name === 'tag' || name === 'mass') loadTags()
}

onMounted(load)
</script>

<style scoped>
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 12px; }
</style>
