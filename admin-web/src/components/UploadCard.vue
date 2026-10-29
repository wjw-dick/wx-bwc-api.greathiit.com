<template>
  <div class="upload-card">
    <div>
      <div class="flex">
        <el-icon><Picture /></el-icon>
        <span>{{ label || '图片/附件上传' }}</span>
      </div>
      <div class="hint">支持拖拽、多图、缩略图，走对象存储；现场照片/整改前后/失物照片/证件扫描件分类上传</div>
      <div v-if="files.length" class="thumbs">
        <div v-for="(url, index) in files" :key="url" class="thumb">
          <el-image :src="url" fit="cover" :preview-src-list="files" :initial-index="index" />
          <el-icon class="remove" @click="remove(index)"><Close /></el-icon>
        </div>
      </div>
    </div>
    <el-upload
      :show-file-list="false"
      :http-request="doUpload"
      :multiple="multiple"
      :accept="accept"
    >
      <el-button type="primary" plain :loading="uploading">选择文件</el-button>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Picture } from '@element-plus/icons-vue'
import { uploadFile } from '@/api/http'

const props = defineProps<{
  modelValue?: string
  label?: string
  multiple?: boolean
  accept?: string
  bizType?: string
}>()
const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)

const files = computed(() => (props.modelValue ? props.modelValue.split(',').filter(Boolean) : []))

async function doUpload(options: any) {
  uploading.value = true
  try {
    const result: any = await uploadFile(options.file, props.bizType || 'common')
    const next = props.multiple ? [...files.value, result.url].join(',') : result.url
    emit('update:modelValue', next)
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

function remove(index: number) {
  const next = [...files.value]
  next.splice(index, 1)
  emit('update:modelValue', next.join(','))
}
</script>

<style scoped>
.thumbs { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.thumb { position: relative; width: 58px; height: 58px; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
.thumb .el-image { width: 100%; height: 100%; }
.remove {
  position: absolute; right: 2px; top: 2px; background: rgba(15, 23, 42, 0.65); color: #fff;
  border-radius: 50%; padding: 2px; cursor: pointer;
}
</style>
