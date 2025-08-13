<template>
  <div class="avatar-edit-page">
  
    <h2>修改头像</h2>
    <div class="avatar-preview">
      <img :src="previewUrl || currentAvatar" alt="头像预览" class="avatar-img" />
    </div>
    <input type="file" accept="image/*" @change="onFileChange" />
    <button class="upload-btn" :disabled="!file" @click="uploadAvatar">上传头像</button>
    <button class="confirm-btn" :disabled="!uploadedPath" @click="confirmAvatar">确认修改</button>
    <div v-if="msg" class="msg">{{ msg }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { uploadAvatarApi, confirmAvatarApi, getProfileApi } from '../utils/api'
import { useRouter } from 'vue-router'

const file = ref<File | null>(null)
const previewUrl = ref('')
const uploadedPath = ref('')
const currentAvatar = ref('')
const msg = ref('')
const router = useRouter()
const userId = ref<number | null>(null)

onMounted(async () => {
  // 获取当前用户头像和id
  const res = await getProfileApi()
  if ((res.code === 0 || res.code === '0') && res.data) {
    currentAvatar.value = res.data.avatar
    userId.value = res.data.id || res.data.Id
    // 新增：同步userid
    if (res.data.id) localStorage.setItem('userid', res.data.id)
  }
})

function onFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files && target.files[0]) {
    file.value = target.files[0]
    previewUrl.value = URL.createObjectURL(file.value)
    uploadedPath.value = ''
    msg.value = ''
  }
}

async function uploadAvatar() {
  if (!file.value) return
  try {
    const res = await uploadAvatarApi(file.value)
    if ((res.code === 0 || res.code === '0') && res.data && res.data.path) {
      uploadedPath.value = res.data.path
      msg.value = '上传成功，请点击确认修改'
    } else {
      msg.value = res.msg || '上传失败'
    }
  } catch (e) {
    msg.value = '上传失败'
  }
}

async function confirmAvatar() {
  if (!uploadedPath.value || !userId.value) return
  try {
    const res = await confirmAvatarApi(uploadedPath.value, userId.value)
    if ((res.code === 0 || res.code === '0')) {
      msg.value = '修改成功，返回个人主页...'
      setTimeout(() => {
        // 跳转到 /userid
        const uid = userId.value || localStorage.getItem('userid')
        if (uid) {
          router.push('/' + uid)
        } else {
          router.push('/')
        }
      }, 1200)
    } else {
      msg.value = res.msg || '修改失败'
    }
  } catch (e) {
    msg.value = '修改失败'
  }
}
</script>

<style scoped>
.avatar-edit-page {
  max-width: 400px;
  margin: 48px auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  padding: 32px 24px 24px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.avatar-preview {
  margin-bottom: 18px;
}
.avatar-img {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #eee;
  background: #f5f7fa;
}
.upload-btn, .confirm-btn {
  margin: 12px 8px 0 8px;
  padding: 8px 24px;
  border: none;
  border-radius: 6px;
  background: #7c6fa7;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.2s;
}
.upload-btn:disabled, .confirm-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.msg {
  margin-top: 16px;
  color: #7c6fa7;
  font-size: 15px;
}
</style> 