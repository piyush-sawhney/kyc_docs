<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useSetupStore } from './stores/setup'
import KnapsLogo from './components/KnapsLogo.vue'

const auth = useAuthStore()
const setup = useSetupStore()
const router = useRouter()

const routeLoading = ref(false)
const userMenuOpen = ref(false)

router.beforeEach(() => {
  routeLoading.value = true
})

router.afterEach(() => {
  setTimeout(() => { routeLoading.value = false }, 200)
})

const navItems = [
  { title: 'Dashboard', icon: 'bi-view-dashboard', route: '/' },
  { title: 'Users', icon: 'bi-person-gear', route: '/users', admin: true },
  { title: 'Audit Logs', icon: 'bi-shield-account', route: '/audit-logs', admin: true },
]

const visibleNavItems = computed(() => {
  if (!auth.user) return []
  return navItems.filter((item) => !item.admin || auth.user?.role === 'admin')
})

const initials = computed(() => {
  if (!auth.user?.fullName) return '?'
  return auth.user.fullName
    .split(' ')
    .map((n: string) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
})

onMounted(async () => {
  if (!auth.initialized) await auth.fetchUser()
  if (!setup.initialized) await setup.checkStatus()
})

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}

function closeMenu() {
  userMenuOpen.value = false
}
</script>

<template>
  <div>
    <div v-if="routeLoading" class="route-loader"></div>

    <nav v-if="auth.user && !setup.needsSetup" class="navbar navbar-expand navbar-light bg-white border-bottom fixed-top" style="height: 56px; padding: 0 16px;">
      <div class="d-flex align-items-center w-100">
        <div class="d-flex align-items-center gap-2">
          <KnapsLogo size="sm" />
        </div>

        <div class="d-flex align-items-center gap-1 ms-3">
          <router-link v-for="item in visibleNavItems" :key="item.route" :to="item.route"
            class="nav-btn text-decoration-none px-3 py-2 small fw-medium"
            :class="router.currentRoute.value.path === item.route ? 'text-primary' : 'text-muted'">
            <i :class="item.icon + ' me-1'" style="font-size: 14px;"></i>
            {{ item.title }}
          </router-link>
        </div>

        <div class="ms-auto d-flex align-items-center gap-2">
          <div class="dropdown">
            <button class="btn btn-link text-decoration-none text-dark px-2 dropdown-toggle d-flex align-items-center gap-1"
              @click="userMenuOpen = !userMenuOpen" data-bs-toggle="dropdown" aria-expanded="false">
              <div class="avatar-initials" style="background: rgba(30,58,95,0.08); color: #1E3A5F; width: 28px; height: 28px; font-size: 11px;">
                {{ initials }}
              </div>
              <small class="fw-medium text-muted">{{ auth.user?.fullName }}</small>
            </button>
            <ul class="dropdown-menu dropdown-menu-end shadow-sm border rounded-3 py-1" style="min-width: 200px;"
              :class="{ show: userMenuOpen }">
              <li><hr class="dropdown-divider my-1"></li>
              <li>
                <a class="dropdown-item small py-2 text-danger" href="#" @click.prevent="handleLogout(); closeMenu()">
                  <i class="bi bi-box-arrow-right me-2"></i> Logout
                </a>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </nav>

    <main :style="auth.user && !setup.needsSetup ? 'padding-top: 56px;' : ''">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<style>
.route-loader {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #1E3A5F 30%, #3B82F6 60%, #1E3A5F);
  background-size: 200% 100%;
  animation: loader-slide 1s ease infinite;
  z-index: 9999;
}
@keyframes loader-slide {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}
.page-enter-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.page-leave-active {
  transition: opacity 0.1s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
.page-leave-to {
  opacity: 0;
}
.nav-btn {
  position: relative;
  transition: color 0.15s ease;
  border-radius: 0;
}
.nav-btn::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: rgb(30, 58, 95);
  border-radius: 1px;
  transition: width 0.2s ease;
}
.nav-btn.text-primary::after {
  width: 60%;
}
</style>
