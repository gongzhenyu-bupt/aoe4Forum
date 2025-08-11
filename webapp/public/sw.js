// Service Worker for avatar caching
const CACHE_NAME = 'avatar-cache-v1'
const AVATAR_CACHE_NAME = 'avatar-cache-v1'

// 安装时预缓存一些资源
self.addEventListener('install', (event) => {
  console.log('Service Worker installing...')
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        return cache.addAll([
          '/',
          '/index.html'
        ])
      })
  )
})

// 拦截网络请求
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url)
  
  // 只缓存头像请求
  if (url.pathname.includes('/avatarImg/') || url.pathname.includes('/defaultImg/')) {
    event.respondWith(
      caches.open(AVATAR_CACHE_NAME)
        .then((cache) => {
          return cache.match(event.request)
            .then((response) => {
              // 如果缓存中有，直接返回
              if (response) {
                console.log('Avatar served from cache:', url.pathname)
                return response
              }
              
              // 如果缓存中没有，从网络获取并缓存
              return fetch(event.request)
                .then((networkResponse) => {
                  // 只缓存成功的响应
                  if (networkResponse.status === 200) {
                    cache.put(event.request, networkResponse.clone())
                    console.log('Avatar cached:', url.pathname)
                  }
                  return networkResponse
                })
                .catch(() => {
                  // 网络请求失败时，返回默认头像
                  return new Response('', {
                    status: 200,
                    headers: {
                      'Content-Type': 'image/png'
                    }
                  })
                })
            })
        })
    )
  }
})

// 清理旧缓存
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME && cacheName !== AVATAR_CACHE_NAME) {
            return caches.delete(cacheName)
          }
        })
      )
    })
  )
}) 