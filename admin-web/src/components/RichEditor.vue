<template>
  <div class="rich-editor">
    <Toolbar class="rich-toolbar" :editor="editorRef" :default-config="toolbarConfig" mode="default" />
    <Editor
      v-model="valueHtml"
      class="rich-body"
      :default-config="editorConfig"
      mode="default"
      @on-created="handleCreated"
    />
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor } from '@wangeditor/editor'
import { uploadFile } from '@/api/http'

const props = defineProps<{ modelValue?: string; placeholder?: string }>()
const emit = defineEmits(['update:modelValue'])
const editorRef = shallowRef<IDomEditor>()
const valueHtml = ref(props.modelValue || '')

const toolbarConfig = {
  excludeKeys: ['group-video', 'fullScreen']
}
const editorConfig = {
  placeholder: props.placeholder || '请输入内容，支持图文混排、粘贴自动上传…',
  MENU_CONF: {
    uploadImage: {
      async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
        const result: any = await uploadFile(file, 'richtext')
        insertFn(result.url, result.fileName, result.url)
      }
    }
  }
}

watch(() => props.modelValue, (value) => {
  if (value !== valueHtml.value) valueHtml.value = value || ''
})
watch(valueHtml, (value) => emit('update:modelValue', value))

function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
}
onBeforeUnmount(() => editorRef.value?.destroy())
</script>
