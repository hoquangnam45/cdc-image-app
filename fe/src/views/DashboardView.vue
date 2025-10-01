<template>
  <div class="container">
    <h2>Dashboard</h2>
    <div class="upload">
      <h3>Upload image</h3>
      <input type="file" @change="onFileChange" />
      <button :disabled="!selectedFile" @click="requestSignedUrl">Upload</button>
      <div v-if="signedUrl">
        <p>Signed URL acquired. Uploading...</p>
        <progress :value="progress" max="100"></progress>
      </div>
      <div v-if="message">{{ message }}</div>
    </div>

    <div class="images">
      <h3>Your images</h3>
      <button @click="loadImages">Refresh list</button>
      <div v-if="images.length === 0">No images yet.</div>
      <div class="grid">
        <div class="card" v-for="img in images" :key="img.id">
          <div>
            <div><strong>{{ img.fileName }}</strong></div>
            <div>Status: {{ img.status }}</div>
          </div>
          <div v-if="img.uploadedImageDownloadUrl">
            <a :href="img.uploadedImageDownloadUrl" target="_blank">View original</a>
          </div>
          <div v-if="img.thumbnails?.length">
            <div class="thumbs">
              <a v-for="t in img.thumbnails" :key="t.id" :href="t.thumbnailDownloadUrl" target="_blank">
                <img :src="t.thumbnailDownloadUrl" alt="thumb" />
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import axios from 'axios'
import { onMounted, ref } from 'vue'

axios.defaults.withCredentials = true

const selectedFile = ref(null)
const signedUrl = ref('')
const progress = ref(0)
const message = ref('')
const images = ref([])
let pollTimerId = null

const onFileChange = (e) => {
  const file = e.target.files?.[0]
  selectedFile.value = file || null
}

const requestSignedUrl = async () => {
  message.value = ''
  progress.value = 0
  signedUrl.value = ''
  try {
    if (!selectedFile.value) return
    const res = await axios.post('/api/image/upload', [selectedFile.value.name])
    const data = res.data?.data || []
    if (data.length === 0) return
    const url = data[0]?.signedUrl
    signedUrl.value = url
    await uploadFile(url)
    startPolling()
  } catch (e) {
    message.value = e?.response?.data?.message || e.message || 'Failed to get signed URL'
  }
}

const uploadFile = async (url) => {
  try {
    const resp = await fetch(url, {
      method: 'PUT',
      headers: {
        'x-goog-if-generation-match': '0',
        'x-goog-meta-fileName': selectedFile.value?.name || '',
        'Content-Type': selectedFile.value?.type || 'application/octet-stream',
      },
      body: selectedFile.value,
    })
    if (!resp.ok) throw new Error('Upload failed')
    progress.value = 100
    message.value = 'Uploaded. Processing...'
  } catch (e) {
    message.value = e.message
  }
}

const loadImages = async () => {
  try {
    const res = await axios.get('/api/image/list')
    images.value = res.data?.data || []
  } catch (e) {
    // ignore
  }
}

const startPolling = () => {
  if (pollTimerId) clearInterval(pollTimerId)
  pollTimerId = setInterval(loadImages, 3000)
}

onMounted(() => {
  loadImages()
  startPolling()
})
</script>

<style scoped>
.container { max-width: 900px; margin: 20px auto; }
.upload { margin-bottom: 24px; display: flex; gap: 8px; align-items: center; }
.images .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.card { border: 1px solid #ddd; padding: 8px; border-radius: 8px; display: flex; flex-direction: column; gap: 6px; }
.thumbs img { width: 100px; height: 100px; object-fit: cover; }
</style>


