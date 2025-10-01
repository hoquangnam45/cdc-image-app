<template>
  <div class="container">
    <h2>Login</h2>
    <form @submit.prevent="onSubmit">
      <label>Username or Email or Phone</label>
      <input v-model="form.username" placeholder="username" />
      <input v-model="form.email" placeholder="email" />
      <input v-model="form.phoneNumber" placeholder="phone" />
      <label>Password</label>
      <input v-model="form.password" type="password" />
      <button type="submit">Login</button>
    </form>
    <p>
      No account? <router-link to="/register">Register</router-link>
    </p>
  </div>
  <div v-if="error" class="error">{{ error }}</div>
  <div v-if="loading">Loading...</div>
  <div v-if="auth.isAuthenticated">
    <router-link to="/dashboard">Go to dashboard</router-link>
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
    await auth.login(form)
    router.push('/dashboard')
  } catch (e) {
    error.value = e?.response?.data?.message || e.message || 'Login failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.container { max-width: 420px; margin: 40px auto; display: flex; flex-direction: column; gap: 8px; }
input { padding: 8px; }
button { padding: 8px; }
.error { color: red; }
</style>


