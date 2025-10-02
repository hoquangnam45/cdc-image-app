<template>
  <div class="container">
    <h2>Dashboard</h2>
    <div class="upload-section">
      <div class="upload-header">
        <div class="upload-icon">📸</div>
        <h3>Upload Image</h3>
        <p class="upload-description">Select an image file to upload and process</p>
      </div>
      
      <div class="upload-card" :class="{ 'has-file': selectedFiles.length > 0, 'uploading': uploadingFiles.size > 0 }">
        <div class="file-input-area">
          <input type="file" id="fileInput" @change="onFileChange" accept="image/*" multiple />
          <label for="fileInput" class="file-label">
            <div class="file-icon">
              <span v-if="selectedFiles.length === 0">📁</span>
              <span v-else>🖼️</span>
            </div>
            <div class="file-text">
              <div class="file-title">
                <span v-if="selectedFiles.length === 0">Choose image files</span>
                <span v-else-if="selectedFiles.length === 1">{{ selectedFiles[0].name }}</span>
                <span v-else>{{ selectedFiles.length }} files selected</span>
              </div>
              <div class="file-subtitle">
                <span v-if="selectedFiles.length === 0">Click to browse or drag & drop</span>
                <span v-else>Click to change files</span>
              </div>
            </div>
            <div class="file-size" v-if="selectedFiles.length > 0">
              {{ formatBytes(selectedFiles.reduce((total, file) => total + file.size, 0)) }}
            </div>
          </label>
        </div>
        
        <button class="upload-btn" :disabled="selectedFiles.length === 0" @click="uploadAllFiles">
          <span v-if="uploadingFiles.size === 0">
            <span class="btn-icon">⬆️</span>
            <span class="btn-text">{{ selectedFiles.length > 0 ? `Upload ${selectedFiles.length} file${selectedFiles.length > 1 ? 's' : ''}` : 'Select Files First' }}</span>
          </span>
          <span v-else>
            <span class="btn-icon spinning">⏳</span>
            <span class="btn-text">Uploading {{ uploadingFiles.size }} file{{ uploadingFiles.size > 1 ? 's' : '' }}...</span>
          </span>
        </button>
      </div>
      
      <!-- File list -->
      <div v-if="selectedFiles.length > 0" class="file-list">
        <div class="file-list-header">
          <span>Selected Files ({{ selectedFiles.length }})</span>
          <button class="clear-btn" @click="clearFiles">Clear All</button>
        </div>
        <div class="file-items">
          <div v-for="(file, index) in selectedFiles" :key="index" class="file-item" :class="{ 'uploading': uploadingFiles.has(file.name) }">
            <div class="file-info">
              <span class="file-name">{{ file.name }}</span>
              <span class="file-size">{{ formatBytes(file.size) }}</span>
            </div>
            <div class="file-status">
              <span v-if="uploadingFiles.has(file.name)" class="status-uploading">⏳ Uploading...</span>
              <span v-else class="status-ready">✅ Ready</span>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="signedUrl" class="progress-section">
        <div class="progress-header">
          <span class="progress-label">Upload Progress</span>
          <span class="progress-percentage">{{ progress }}%</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progress + '%' }"></div>
        </div>
      </div>
      
      <div v-if="message" class="message-section" :class="{ 'error': message.includes('Failed') || message.includes('Error'), 'success': message.includes('Uploaded') || message.includes('success') }">
        <div class="message-icon">
          <span v-if="message.includes('Failed') || message.includes('Error')">❌</span>
          <span v-else-if="message.includes('Uploaded') || message.includes('success')">✅</span>
          <span v-else>ℹ️</span>
        </div>
        <div class="message-text">{{ message }}</div>
      </div>
    </div>

    <div class="images">
      <h3>Your images</h3>
      <button @click="loadImages">Refresh list</button>
      <div v-if="images.length === 0">No images yet.</div>
      <div class="grid">
        <div class="card" v-for="img in images" :key="img.id">
          <div class="header">
            <div class="title">{{ img.fileName }}</div>
            <div class="status">Status: {{ statusText(img) }}</div>
          </div>
          <div class="media" v-if="img.status === 'UPLOADED' && img.downloadUrl">
            <a :href="img.downloadUrl" target="_blank"><img :src="img.downloadUrl" alt="uploaded" /></a>
          </div>
          <div class="media placeholder" v-else-if="isPendingAndNotExpired(img)">
            <div class="ph-text">Preview will appear after upload</div>
          </div>
          <div class="media placeholder disabled" v-else>
            <div class="ph-text">{{ expiredLabel(img) }}</div>
          </div>
          <div class="actions" v-if="img.status === 'UPLOADED' && img.downloadUrl">
            <a class="btn small" :href="img.downloadUrl" download>Download</a>
          </div>
          <div class="row-actions" v-if="img.status !== 'UPLOADED'">
            <button class="btn danger small" @click="onDelete(img.id)" :disabled="deletingIds.has(img.id)">
              {{ deletingIds.has(img.id) ? 'Deleting…' : 'Delete' }}
            </button>
          </div>
          <ul class="meta">
            <li><strong>ID:</strong> {{ img.id }}</li>
            <li v-if="img.fileType"><strong>Type:</strong> {{ img.fileType }}</li>
            <li v-if="img.width && img.height"><strong>Size:</strong> {{ img.width }}×{{ img.height }}</li>
            <li v-if="img.fileSize != null"><strong>Bytes:</strong> {{ formatBytes(img.fileSize) }}</li>
            <li v-if="img.fileHash"><strong>Hash:</strong> {{ img.fileHash }}</li>
            <li v-if="img.createdAt"><strong>Created:</strong> {{ formatDate(img.createdAt) }}</li>
            <li v-if="img.updatedAt"><strong>Updated:</strong> {{ formatDate(img.updatedAt) }}</li>
            <li v-if="isPendingAndNotExpired(img)"><strong>URL Expires:</strong> {{ formatDate(img.expiredAt) }}</li>
          </ul>
          <div v-if="img.thumbnails?.length">
            <button class="expandBtn" @click="toggleExpand(img.id)">
              {{ isExpanded(img.id) ? 'Hide' : 'Show' }} thumbnails ({{ img.thumbnails.length }})
            </button>
            <div v-if="isExpanded(img.id)" class="thumbs">
              <div class="thumb" v-for="t in img.thumbnails" :key="t.id">
                <template v-if="t.status === 'COMPLETED' && t.downloadUrl">
                  <a :href="t.downloadUrl" target="_blank" class="thumb-image">
                    <img :src="t.downloadUrl" alt="thumb" />
                  </a>
                </template>
                <template v-else>
                  <div class="thumb-image thumb-placeholder disabled">
                    <div class="ph-text small">{{ thumbLabel(t) }}</div>
                  </div>
                </template>
                <div class="thumb-brief">
                  <span class="pill" :data-status="t.status">{{ t.status }}</span>
                </div>
                <div class="thumb-actions" v-if="t.status === 'COMPLETED' && t.downloadUrl">
                  <a class="btn xsmall" :href="t.downloadUrl" download>Download</a>
                </div>
                <details class="thumb-details">
                  <summary>More</summary>
                  <div class="thumb-meta">
                    <div v-if="t.width && t.height"><strong>Size:</strong> {{ t.width }}×{{ t.height }}</div>
                    <div v-if="t.fileSize != null"><strong>Bytes:</strong> {{ formatBytes(t.fileSize) }}</div>
                    <div v-if="t.fileType"><strong>Type:</strong> {{ t.fileType }}</div>
                    <div v-if="t.fileHash"><strong>Hash:</strong> {{ t.fileHash }}</div>
                    <div v-if="t.createdAt"><strong>Created:</strong> {{ formatDate(t.createdAt) }}</div>
                  </div>
                </details>
              </div>
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
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

axios.defaults.withCredentials = true

const router = useRouter()
const authStore = useAuthStore()

const selectedFiles = ref([])
const signedUrl = ref('')
const progress = ref(0)
const message = ref('')
const uploadingFiles = ref(new Set())
const images = ref([])
let pollTimerId = null
const expanded = ref(new Set())
const deletingIds = ref(new Set())

const onFileChange = (e) => {
  const files = Array.from(e.target.files || [])
  selectedFiles.value = files
  // Reset upload state when new files are selected
  signedUrl.value = ''
  message.value = ''
  progress.value = 0
  uploadingFiles.value.clear()
}

const clearFiles = () => {
  selectedFiles.value = []
  uploadingFiles.value.clear()
  message.value = ''
  progress.value = 0
  // Reset file input
  const fileInput = document.getElementById('fileInput')
  if (fileInput) fileInput.value = ''
}

const uploadAllFiles = async () => {
  if (selectedFiles.value.length === 0) {
    message.value = 'Please select files first'
    return
  }
  
  message.value = ''
  progress.value = 0
  
  try {
    // Upload files sequentially to avoid overwhelming the server
    for (const file of selectedFiles.value) {
      await uploadSingleFile(file)
    }
    
    message.value = `Successfully uploaded ${selectedFiles.value.length} file${selectedFiles.value.length > 1 ? 's' : ''}`
    clearFiles()
    startPolling()
  } catch (e) {
    message.value = e?.response?.data?.message || e.message || 'Upload failed'
  }
}

const uploadSingleFile = async (file) => {
  try {
    uploadingFiles.value.add(file.name)
    
    // Get signed URL for this file
    const res = await axios.post('/api/image/upload', [file.name])
    const data = res.data?.data || []
    if (data.length === 0) {
      throw new Error('No upload URL received from server')
    }
    
    const url = data[0]?.uploadUrl
    if (!url) {
      throw new Error('Invalid upload URL received')
    }
    
    // Upload the file
    await uploadFileToUrl(url, file)
    
    uploadingFiles.value.delete(file.name)
  } catch (e) {
    uploadingFiles.value.delete(file.name)
    throw e
  }
}

const uploadFileToUrl = async (url, file) => {
  try {
    const resp = await fetch(url, {
      method: 'PUT',
      headers: {
        'x-goog-if-generation-match': '0',
        'x-goog-meta-fileName': file.name || '',
        'Content-Type': file.type || 'application/octet-stream',
      },
      body: file,
    })
    if (!resp.ok) throw new Error('Upload failed')
  } catch (e) {
    throw e
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
  pollTimerId = setInterval(loadImages, 10000)
}

onMounted(() => {
  // Check if user is authenticated before loading images
  if (!authStore.isAuthenticated) {
    router.push('/login')
    return
  }
  
  loadImages()
  startPolling()
})

const formatBytes = (n) => {
  if (n == null) return ''
  const units = ['B','KB','MB','GB']
  let i = 0
  let v = n
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++ }
  return `${v.toFixed(1)} ${units[i]}`
}

const formatDate = (iso) => {
  try { return new Date(iso).toLocaleString() } catch (_) { return iso }
}

const toggleExpand = (id) => {
  const s = new Set(expanded.value)
  if (s.has(id)) s.delete(id); else s.add(id)
  expanded.value = s
}
const isExpanded = (id) => expanded.value.has(id)

const isPendingAndNotExpired = (img) => {
  if (!img?.expiredAt) return false
  if (img?.status !== 'PENDING') return false
  try { return new Date(img.expiredAt).getTime() > Date.now() } catch (_) { return false }
}

const expiredLabel = (img) => {
  if (img?.status === 'PENDING') return '⛔ Upload URL expired'
  if (img?.status === 'UPLOADED') return ''
  return '⚠️ Not available'
}

const thumbLabel = (t) => {
  if (t?.status === 'PENDING') return '⏳ Processing'
  if (t?.status === 'FAILED') return '❌ Failed'
  return '⚠️ Not available'
}

const statusText = (img) => {
  if (img?.status === 'PENDING' && !isPendingAndNotExpired(img)) return 'EXPIRED'
  return img?.status
}

const onDelete = async (id) => {
  if (!confirm('Delete this image?')) return
  const s = new Set(deletingIds.value); s.add(id); deletingIds.value = s
  try {
    await axios.delete(`/api/image/${id}`)
    images.value = images.value.filter(x => x.id !== id)
  } catch (e) {
    alert(e?.response?.data?.message || e.message || 'Delete failed')
  } finally {
    const s2 = new Set(deletingIds.value); s2.delete(id); deletingIds.value = s2
  }
}
</script>

<style scoped>
.container { max-width: 900px; margin: 20px auto; }

/* Upload Section Styles */
.upload-section { margin-bottom: 32px; }
.upload-header { 
  text-align: center; 
  margin-bottom: 24px; 
}
.upload-icon { 
  font-size: 48px; 
  margin-bottom: 12px; 
  opacity: 0.8;
}
.upload-section h3 { 
  margin: 0 0 8px; 
  color: #e6edf3; 
  font-size: 24px; 
  font-weight: 600;
}
.upload-description { 
  color: #9bb1c9; 
  font-size: 14px; 
  margin: 0;
}

.upload-card { 
  background: linear-gradient(135deg, #1c2128 0%, #21262d 100%);
  border: 2px dashed #2a313c; 
  border-radius: 16px; 
  padding: 24px; 
  display: flex; 
  align-items: center; 
  gap: 20px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.upload-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 0.1), transparent);
  transition: left 0.5s;
}
.upload-card:hover::before {
  left: 100%;
}
.upload-card:hover { 
  border-color: #2563eb; 
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(37, 99, 235, 0.15);
}
.upload-card.has-file { 
  border-color: #10b981; 
  border-style: solid;
}
.upload-card.uploading { 
  border-color: #f59e0b; 
  border-style: solid;
}

.file-input-area { flex: 1; }
#fileInput { display: none; }
.file-label { 
  display: flex; 
  align-items: center; 
  gap: 16px; 
  cursor: pointer; 
  padding: 16px; 
  border-radius: 12px;
  background: rgba(15, 20, 26, 0.5);
  border: 1px solid #2a313c;
  transition: all 0.2s ease;
  position: relative;
}
.file-label:hover { 
  background: rgba(15, 20, 26, 0.8); 
  border-color: #444c56;
  transform: scale(1.02);
}
.file-icon { 
  font-size: 32px; 
  opacity: 0.8;
  transition: transform 0.2s ease;
}
.file-label:hover .file-icon {
  transform: scale(1.1);
}
.file-text { flex: 1; }
.file-title { 
  font-weight: 600; 
  color: #e6edf3; 
  margin-bottom: 4px;
  font-size: 16px;
  word-break: break-all;
}
.file-subtitle { 
  font-size: 13px; 
  color: #9bb1c9; 
}
.file-size {
  background: #2563eb;
  color: white;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.upload-btn { 
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: white; 
  border: none; 
  padding: 12px 24px; 
  border-radius: 12px; 
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
  overflow: hidden;
  min-width: 140px;
  white-space: nowrap;
}
.upload-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.3s;
}
.upload-btn:hover::before {
  left: 100%;
}
.upload-btn:hover:not(:disabled) { 
  background: linear-gradient(135deg, #1d4ed8 0%, #1e40af 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}
.upload-btn:disabled { 
  background: #374151; 
  color: #9ca3af; 
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
  opacity: 0.6;
}
.upload-btn:disabled::before {
  display: none;
}
.btn-icon {
  font-size: 16px;
}
.btn-text {
  font-size: 14px;
  font-weight: 600;
}
.spinning {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.progress-section { 
  margin-top: 20px; 
  padding: 20px; 
  background: rgba(15, 20, 26, 0.5);
  border-radius: 12px;
  border: 1px solid #2a313c;
}
.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.progress-label {
  color: #e6edf3;
  font-weight: 500;
  font-size: 14px;
}
.progress-percentage {
  color: #2563eb;
  font-weight: 600;
  font-size: 14px;
}
.progress-bar { 
  width: 100%; 
  height: 8px; 
  background: #2a313c; 
  border-radius: 4px; 
  overflow: hidden; 
  position: relative;
}
.progress-bar::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 0.3), transparent);
  animation: shimmer 2s infinite;
}
@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
.progress-fill { 
  height: 100%; 
  background: linear-gradient(90deg, #2563eb, #3b82f6);
  transition: width 0.3s ease; 
  position: relative;
  z-index: 1;
}

.message-section { 
  margin-top: 16px; 
  padding: 16px 20px; 
  border-radius: 12px; 
  font-size: 14px; 
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 4px solid;
}
.message-section.success { 
  background: rgba(16, 185, 129, 0.1); 
  color: #10b981; 
  border-left-color: #10b981;
}
.message-section.error { 
  background: rgba(239, 68, 68, 0.1); 
  color: #f87171; 
  border-left-color: #f87171;
}
.message-section:not(.success):not(.error) {
  background: rgba(59, 130, 246, 0.1);
  color: #60a5fa;
  border-left-color: #60a5fa;
}
.message-icon {
  font-size: 18px;
}
.message-text {
  flex: 1;
}

/* File List Styles */
.file-list {
  margin-top: 16px;
  background: rgba(15, 20, 26, 0.5);
  border-radius: 12px;
  border: 1px solid #2a313c;
  overflow: hidden;
}
.file-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(15, 20, 26, 0.8);
  border-bottom: 1px solid #2a313c;
  font-weight: 500;
  color: #e6edf3;
  font-size: 14px;
}
.clear-btn {
  background: #dc2626;
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.clear-btn:hover {
  background: #b91c1c;
}
.file-items {
  max-height: 200px;
  overflow-y: auto;
}
.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #2a313c;
  transition: background-color 0.2s;
}
.file-item:last-child {
  border-bottom: none;
}
.file-item:hover {
  background: rgba(15, 20, 26, 0.3);
}
.file-item.uploading {
  background: rgba(37, 99, 235, 0.1);
}
.file-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.file-name {
  color: #e6edf3;
  font-size: 14px;
  font-weight: 500;
  word-break: break-all;
}
.file-size {
  color: #9bb1c9;
  font-size: 12px;
}
.file-status {
  font-size: 12px;
}
.status-ready {
  color: #10b981;
}
.status-uploading {
  color: #f59e0b;
}
.images .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.images > h3 { margin-top: 24px; margin-bottom: 8px; }
.images > button { margin-bottom: 16px; }
.card { border: 1px solid #2a313c; background:#1c2128; color:#e6edf3; padding: 10px; border-radius: 10px; display: flex; flex-direction: column; gap: 8px; }
.header { display:flex; justify-content: space-between; align-items: center; }
.title { font-weight: 600; }
.status { color:#a6b3c5; font-size: 12px; }
.media { border-radius: 6px; border:1px solid #2f3b4a; background:#0f141a; display:flex; align-items:center; justify-content:center; min-height: 200px; }
.media img { width: 100%; height: auto; border-radius: 6px; }
.media.placeholder { opacity: .6; }
.media.disabled, .thumb-placeholder.disabled { pointer-events: none; filter: grayscale(0.3); }
.ph-text { padding: 24px; color:#9bb1c9; font-size: 14px; text-align:center; }
.actions { display:flex; justify-content:flex-end; }
.btn { display:inline-block; background:#2563eb; color:white; padding:8px 12px; border-radius:8px; text-decoration:none; }
.btn.small { padding:6px 10px; font-size:13px; }
.btn.danger { background:#dc2626; }
.btn.xsmall { padding:4px 8px; font-size:12px; }
.meta { list-style: none; padding: 0; margin: 0; display: grid; gap: 4px; font-size: 12px; color:#c9d4e1; }
.thumbs { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 10px; }
.thumb { background:#0f141a; border:1px solid #2a313c; border-radius:8px; padding:6px; display:flex; flex-direction:column; gap:6px; }
.thumb-image img { width: 100%; height: auto; border-radius: 4px; }
.thumb-placeholder { display:flex; align-items:center; justify-content:center; min-height: 120px; border-radius:4px; border:1px dashed #2f3b4a; background:#0c1117; }
.ph-text.small { padding: 8px; font-size: 12px; }
.thumb-brief { display:flex; gap:8px; align-items:center; flex-wrap:wrap; font-size:12px; color:#c9d4e1; }
.pill { padding:2px 6px; border-radius:999px; font-size:11px; border:1px solid #2f3b4a; background:#10161d; }
.thumb-details summary { cursor:pointer; color:#9bb1c9; }
.thumb-meta { margin-top: 4px; font-size: 12px; color:#a6b3c5; display:grid; gap:2px; }
.expandBtn { margin-top: 8px; padding: 6px 10px; border-radius: 6px; border:1px solid #2f3b4a; background:#0f141a; color:#e6edf3; cursor:pointer; }
.row-actions { display:flex; justify-content:flex-end; margin-top: 6px; }
</style>


