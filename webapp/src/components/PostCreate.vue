<template>
  <div class="create-post-page">
    <h2>发帖</h2>
    <el-form :model="form" label-width="60px" class="post-form">
      <el-form-item label="标题">
        <el-input v-model="form.title" maxlength="50" show-word-limit placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="内容">
        <div style="border: 1px solid #ccc;">
          <Toolbar
            style="border-bottom: 1px solid #ccc"
            :editor="editorRef"
            :defaultConfig="toolbarConfig"
            :mode="mode"
          />
          <Editor
            style="height: 300px; overflow-y: auto;"
            v-model="form.content"
            :defaultConfig="editorConfig"
            :mode="mode"
            @onCreated="handleCreated"
          />
        </div>
      </el-form-item>
      <el-form-item label="板块">
        <el-select v-model="form.forum" placeholder="请选择板块">
          <el-option label="单排攻略" value="solo-guide" />
          <el-option label="组排攻略" value="team-guide" />
          <el-option label="强度讨论" value="meta-discuss" />
          <el-option label="闲聊" value="chat" />
          <el-option label="封神榜" value="hall-of-fame" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">发布</el-button>
        <el-button @click="handleCancel">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { ref, shallowRef, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPostApi } from '../utils/api'

const router = useRouter()
const loading = ref(false)
const editorRef = shallowRef()
const form = ref({
  title: '',
  content: '', // 富文本内容
  forum: ''
})

const toolbarConfig = {}
const editorConfig = { placeholder: '请输入内容...' }
const mode = 'default'

const handleCreated = (editor: any) => {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

async function handleSubmit() {
  if (!form.value.title.trim() || !form.value.content.trim() || !form.value.forum) {
    ElMessage.warning('请填写完整信息')
    return
  }
  loading.value = true
  try {
    const res = await createPostApi(form.value)
    if (res.code === 0 || res.code === '0') {
      ElMessage.success('发帖成功')
      router.push('/articles')
    } else {
      ElMessage.error(res.msg || '发帖失败')
    }
  } catch (e) {
    ElMessage.error('发帖失败')
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  router.back()
}
</script>

<style scoped>
.create-post-page {
  max-width: 900px;
  margin: 48px auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  padding: 32px 24px 24px 24px;
}
.post-form {
  margin-top: 24px;
}
/* 富文本内容左对齐 */
:deep(.w-e-text-container) {
  text-align: left;
}
</style> 