import {createRouter, createWebHistory} from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/group/:groupId',
      name: 'group-members',
      component: () => import('../views/GroupMembersView.vue'),
    },
    {
      path: '/group/:groupId/dashboard',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
    },
    {
      path: '/invite/:token',
      name: 'accept-invite',
      component: () => import('../views/AcceptInviteView.vue'),
    },
    {
      path: '/token-expired',
      name: 'token-expired',
      component: () => import('../views/TokenExpiredView.vue'),
      meta: { public: true } // Marquer cette route comme publique
    },
    {
      path: '/test-input',
      name: 'test-input',
      component: () => import('../views/TestInputView.vue'),
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

// Garde de navigation pour empêcher toute redirection depuis /token-expired
router.beforeEach((to, from, next) => {
  console.log('Navigation:', from.path, '->', to.path)

  // Si on essaie d'aller vers /token-expired, toujours autoriser
  if (to.path === '/token-expired') {
    console.log('Accès autorisé à /token-expired')
    next()
    return
  }

  next()
})

export default router
