import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import DashboardView from '../views/DashboardView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginView },
    { path: '/register', component: RegisterView },
    { path: '/dashboard', component: DashboardView },
  ],
})

// Global route guard to check authentication
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // Define public routes that don't require authentication
  const publicRoutes = ['/login', '/register']
  
  // Check if the route requires authentication
  const requiresAuth = !publicRoutes.includes(to.path)
  
  if (requiresAuth && !authStore.isAuthenticated) {
    // Redirect to login if trying to access protected route without auth
    next('/login')
  } else {
    // Allow access to public routes or authenticated users
    next()
  }
})

export default router


