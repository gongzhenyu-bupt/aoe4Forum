<template>
  <div class="avatar-upload-page">
    <h2>上传头像</h2>
    <el-upload
      class="avatar-uploader"
      :show-file-list="false"
      :http-request="customUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :before-upload="beforeUpload"
      name="multipartFile"
      accept="image/*"
    >
      <img v-if="imageUrl" :src="imageUrl" class="avatar" />
      <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
    </el-upload>
    <div v-if="imageUrl" class="result-path">
      <span>图片地址：</span>
      <a :href="imageUrl" target="_blank">{{ imageUrl }}</a>
    </div>
    <el-button v-if="imageUrl" type="primary" @click="onConfirmAvatar" style="margin-top: 16px;">确认修改头像</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { uploadAvatarApi } from '../utils/api'
import { confirmAvatarApi } from '../utils/api'

const imageUrl = ref('')
// 假设用户id为1，实际可通过props或store传入
const userId = 1
let lastUploadedPath = ''

// 用于将本地路径转换为可访问的URL（如有需要可根据实际API_BASE_URL拼接）
function getImageUrl(path: string): string {
  // 如果是以http/https开头，直接返回
  if (/^https?:\/\//.test(path)) return path
  // 如果是本地路径，转换为后端可访问的静态资源路径（假设后端映射了 /static 或 /tempImg）
  // 这里只做简单处理，实际可根据后端静态资源映射规则调整
  const filename = path.replace(/\\/g, '/').split('/').pop()
  return filename ? `http://101.126.22.249:7071/tempImg/${filename}` : ''
}

function handleSuccess(response: any) {
  // code为'0'或'200'都视为成功
  if ((response.code === '0' || response.code === '200') && response.data && response.data.path) {
    imageUrl.value = getImageUrl(response.data.path)
    lastUploadedPath = response.data.path
    ElMessage.success(response.message || '上传成功')
  } else {
    ElMessage.error(response.message || response.msg || '上传失败')
  }
}

function handleError() {
  ElMessage.error('上传失败，请重试')
}

function beforeUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB！')
  }
  return isImage && isLt2M
}

// 使用api.ts中的uploadAvatarApi进行上传
const customUpload = async (option: any) => {
  try {
    const response = await uploadAvatarApi(option.file)
    option.onSuccess(response)
  } catch (error) {
    option.onError(error)
  }
}

async function onConfirmAvatar() {
  if (!lastUploadedPath) {
    ElMessage.error('请先上传头像')
    return
  }
  try {
    const res = await confirmAvatarApi(lastUploadedPath, userId)
    if (res.code === '0' || res.code === '200') {
      ElMessage.success(res.message || '头像更换成功')
    } else {
      ElMessage.error(res.message || res.msg || '头像更换失败')
    }
  } catch (e) {
    ElMessage.error('头像更换失败')
  }
}
</script>

<style scoped>
.avatar-upload-page {
  max-width: 400px;
  margin: 40px auto;
  text-align: center;
}
.avatar-uploader {
  display: inline-block;
  margin-bottom: 16px;
}
.avatar-uploader-icon {
  font-size: 32px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  cursor: pointer;
}
.avatar {
  width: 100px;
  height: 100px;
  display: block;
  border-radius: 50%;
  object-fit: cover;
  margin: 0 auto;
}
.result-path {
  margin-top: 16px;
  word-break: break-all;
}
</style> 