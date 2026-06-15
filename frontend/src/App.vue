<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useSetupStore } from './stores/setup'

const auth = useAuthStore()
const setup = useSetupStore()
const router = useRouter()

const navItems = [
  { title: 'Dashboard', icon: 'mdi-view-dashboard', route: '/' },
  { title: 'Users', icon: 'mdi-account-cog', route: '/users', admin: true },
  { title: 'Audit Logs', icon: 'mdi-shield-account', route: '/audit-logs', admin: true },
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
</script>

<template>
  <v-app>
    <template v-if="auth.user && !setup.needsSetup">
      <v-app-bar flat elevation="0" class="border-b" color="white" height="56">
        <template #prepend>
          <div class="d-flex align-center ml-2">
            <v-avatar color="primary" size="32" variant="tonal" class="mr-2">
              <v-icon size="18" color="primary">mdi-file-document-check</v-icon>
            </v-avatar>
            <v-toolbar-title class="font-weight-semibold text-body-1 text-primary mr-3">
              KYC Docs
            </v-toolbar-title>
          </div>
          <div class="d-flex align-center ga-1">
            <v-btn v-for="item in visibleNavItems" :key="item.route" :to="item.route"
              variant="text" class="text-none px-3"
              :class="router.currentRoute.value.path === item.route ? 'text-primary font-weight-semibold' : 'text-grey-darken-1'"
              :active="router.currentRoute.value.path === item.route" flat>
              <v-icon start size="16">{{ item.icon }}</v-icon>
              {{ item.title }}
            </v-btn>
          </div>
        </template>

        <v-spacer />

        <div class="d-flex align-center ga-2 mr-2">
          <v-btn v-if="auth.user?.mustChangePassword" color="warning" variant="tonal" size="small"
            prepend-icon="mdi-key" @click="router.push('/change-password')">
            Change Password
          </v-btn>
          <v-menu>
            <template #activator="{ props }">
              <v-btn v-bind="props" variant="text" class="text-none px-2" flat>
                <v-avatar color="primary" size="28" class="mr-2" variant="tonal">
                  <span class="text-caption font-weight-medium text-primary">{{ initials }}</span>
                </v-avatar>
                <span class="text-body-2 text-grey-darken-1 font-weight-medium">{{ auth.user?.fullName }}</span>
                <v-icon size="16" class="ml-1 text-grey">mdi-chevron-down</v-icon>
              </v-btn>
            </template>
            <v-list density="compact" class="rounded-xl border" elevation="4">
              <v-list-item title="Recovery Codes" prepend-icon="mdi-shield-key"
                @click="router.push('/recovery-codes')" />
              <v-divider class="my-1" />
              <v-list-item title="Change Password" prepend-icon="mdi-key"
                @click="router.push('/change-password')" />
              <v-divider class="my-1" />
              <v-list-item title="Logout" prepend-icon="mdi-logout"
                @click="handleLogout" color="error" />
            </v-list>
          </v-menu>
        </div>
      </v-app-bar>

      <v-main>
        <router-view />
      </v-main>
    </template>

    <template v-else>
      <v-main>
        <router-view />
      </v-main>
    </template>
  </v-app>
</template>
