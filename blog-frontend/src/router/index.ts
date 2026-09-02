import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/Home.vue')
      },
      {
        path: 'posts/:slug',
        name: 'post-detail',
        component: () => import('@/views/PostDetail.vue')
      },
      {
        path: 'tags/:name',
        name: 'tag-archive',
        component: () => import('@/views/TagArchive.vue')
      },
      {
        path: 'search',
        name: 'search',
        component: () => import('@/views/Search.vue')
      }
    ]
  },
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('@/views/admin/Login.vue')
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/admin/posts'
      },
      {
        path: 'posts',
        name: 'admin-posts',
        component: () => import('@/views/admin/PostList.vue')
      },
      {
        path: 'posts/new',
        name: 'admin-post-new',
        component: () => import('@/views/admin/PostEditor.vue')
      },
      {
        path: 'posts/:id/edit',
        name: 'admin-post-edit',
        component: () => import('@/views/admin/PostEditor.vue')
      },
      {
        path: 'env-check',
        name: 'admin-env-check',
        component: () => import('@/views/admin/EnvCheck.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (to.meta.requiresAuth) {
    const { useAuthStore } = await import('@/stores/useAuth')
    const auth = useAuthStore()
    if (!auth.ready) await auth.fetchMe()
    if (!auth.username) {
      return { name: 'admin-login', query: { redirect: to.fullPath } }
    }
  }
})

export default router