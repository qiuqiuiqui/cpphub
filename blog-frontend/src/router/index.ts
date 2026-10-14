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
      },
      {
        path: 'categories/:slug',
        name: 'category-detail',
        component: () => import('@/views/CategoryPage.vue')
      },
      {
        path: 'tools',
        name: 'tools-home',
        component: () => import('@/views/tools/EnvCheck.vue')
      },
      {
        path: 'tools/env',
        name: 'tools-env',
        component: () => import('@/views/tools/EnvCheck.vue')
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
        redirect: { name: 'admin-posts-published' }
      },
      {
        path: 'posts',
        redirect: { name: 'admin-posts-published' }
      },
      {
        path: 'posts/published',
        name: 'admin-posts-published',
        meta: { title: '发布文章管理' },
        component: () => import('@/views/admin/PostList.vue')
      },
      {
        path: 'posts/drafts',
        name: 'admin-posts-drafts',
        meta: { title: '草稿箱管理' },
        component: () => import('@/views/admin/PostList.vue')
      },
      {
        path: 'posts/new',
        name: 'admin-post-new',
        meta: { title: '新建文章' },
        component: () => import('@/views/admin/PostEditor.vue')
      },
      {
        path: 'categories',
        name: 'admin-categories',
        meta: { title: '栏目管理' },
        component: () => import('@/views/admin/CategoryManager.vue')
      },
      {
        path: 'tags',
        name: 'admin-tags',
        meta: { title: '标签管理' },
        component: () => import('@/views/admin/TagManager.vue')
      },
      {
        path: 'posts/:id/edit',
        name: 'admin-post-edit',
        meta: { title: '编辑文章' },
        component: () => import('@/views/admin/PostEditor.vue')
      },
      {
        path: 'env-check',
        name: 'admin-env-check',
        meta: { title: '环境检查' },
        component: () => import('@/views/tools/EnvCheck.vue')
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