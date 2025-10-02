<template>
  <div class="page">
    <div class="card">
      <h1 class="title">Create account</h1>
      <form class="form" @submit.prevent="onSubmit">
        <div class="field">
          <label>Username</label>
          <input v-model="form.username" placeholder="username" />
        </div>
        <div class="field">
          <label>Email</label>
          <input v-model="form.email" placeholder="email" />
        </div>
        <div class="field">
          <label>Phone</label>
          <input v-model="form.phoneNumber" placeholder="phone" />
        </div>
        <div class="field">
          <label>Password</label>
          <input v-model="form.password" type="password" placeholder="password" />
        </div>
        <button class="btn" type="submit" :disabled="loading">Create account</button>
      </form>
      <p class="subtext">Have an account? <router-link to="/login">Login</router-link></p>
      <p v-if="error" class="error">{{ error }}</p>
    </div>
  </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const error = ref('')
const form = reactive({ username: '', email: '', phoneNumber: '', password: '' })

const onSubmit = async () => {
  loading.value = true
  error.value = ''
  try {
    await auth.register(form)
    router.push('/dashboard')
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || 'Register failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { display: flex; justify-content: center; padding: 48px 0; background: transparent; }
.card { width: 560px; background: #1c2128; border: 1px solid #2a313c; border-radius: 12px; padding: 24px; }
.title { margin: 0 0 16px; text-align: center; color: #e6edf3; font-size: 24px; }
.form { display: flex; flex-direction: column; gap: 16px; }
.field { display: flex; flex-direction: column; gap: 6px; }
label { color: #a6b3c5; font-size: 14px; }
input { padding: 10px 12px; border-radius: 8px; border: 1px solid #2f3b4a; background: #0f141a; color: #e6edf3; }
.btn { margin-top: 8px; padding: 10px 12px; background: #16a34a; color: white; border: none; border-radius: 8px; cursor: pointer; }
.btn:disabled { opacity: .6; cursor: not-allowed; }
.subtext { margin-top: 10px; text-align: center; color: #a6b3c5; }
.error { margin-top: 8px; color: #ef4444; text-align: center; }
</style>


