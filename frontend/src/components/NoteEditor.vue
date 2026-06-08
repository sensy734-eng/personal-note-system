<template>
  <div class="editor-wrapper" style="border: 1px solid #ccc">
    <Toolbar
        style="border-bottom: 1px solid #ccc"
        :editor="editorRef"
        :defaultConfig="toolbarConfig"
        :mode="mode"
    />
    <Editor
        style="height: 400px; overflow-y: hidden;"
        v-model="valueHtml"
        :defaultConfig="editorConfig"
        :mode="mode"
        @onCreated="handleCreated"
        @onChange="handleChange"
    />
  </div>
</template>

<script setup>
import '@wangeditor/editor/dist/css/style.css' // 引入样式 (必需)
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

// 1. 定义组件的 Prop 和 Emits (用于父子组件通信)
const props = defineProps({
  modelValue: { type: String, default: '' }, // 接收 v-model 传来的 HTML 内容
  placeholder: { type: String, default: '请输入笔记内容...' }
})
const emit = defineEmits(['update:modelValue', 'textChange']) // textChange 用于传出纯文本

// 2. 初始化编辑器状态
const mode = ref('default')
const editorRef = shallowRef() // 使用 shallowRef 优化性能
const valueHtml = ref('')

// 3. 配置项 (WangEditor v5 支持高度自定义)
const toolbarConfig = {
  // 可以在此剔除不需要的菜单，比如 ['image', 'video']
  excludeKeys: []
}
const editorConfig = {
  placeholder: props.placeholder,
  MENU_CONF: {
    // 预留图片上传配置
    // uploadImage: { server: '/api/common/upload', fieldName: 'file' }
  }
}

// 4. 监听外部 Prop 变化，反显到编辑器中
watch(() => props.modelValue, (newVal) => {
  if (editorRef.value && newVal !== valueHtml.value) {
    valueHtml.value = newVal
  }
}, { immediate: true })

// 5.WangEditor 生命周期回调
const handleCreated = (editor) => {
  editorRef.value = editor // 记录编辑器实例
}

// 6.内容变化回调：向父组件传出数据
const handleChange = (editor) => {
  const html = editor.getHtml()
  const text = editor.getText() // 获取纯文本

  emit('update:modelValue', html) // 更新 v-model
  emit('textChange', text)       // 额外传出纯文本 (后端需要用它做摘要)
}

// 7.组件销毁时，必需销毁编辑器实例 (防止内存泄漏)
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})
</script>

<style scoped>
.editor-wrapper {
  z-index: 100; /* 防止被其他元素遮挡 */
}
</style>