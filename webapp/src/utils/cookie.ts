// Cookie 工具函数
export interface CookieOptions {
  expires?: Date | number // 过期时间，可以是Date对象或天数
  path?: string // cookie路径
  domain?: string // cookie域名
  secure?: boolean // 是否只在HTTPS下传输
  sameSite?: 'Strict' | 'Lax' | 'None' // SameSite属性
}

/**
 * 设置cookie
 * @param name cookie名称
 * @param value cookie值
 * @param options cookie选项
 */
export function setCookie(name: string, value: string, options: CookieOptions = {}): void {
  let cookieString = `${encodeURIComponent(name)}=${encodeURIComponent(value)}`
  
  if (options.expires) {
    let expires: Date
    if (typeof options.expires === 'number') {
      expires = new Date()
      expires.setTime(expires.getTime() + options.expires * 24 * 60 * 60 * 1000)
    } else {
      expires = options.expires
    }
    cookieString += `; expires=${expires.toUTCString()}`
  }
  
  if (options.path) {
    cookieString += `; path=${options.path}`
  }
  
  if (options.domain) {
    cookieString += `; domain=${options.domain}`
  }
  
  if (options.secure) {
    cookieString += '; secure'
  }
  
  if (options.sameSite) {
    cookieString += `; samesite=${options.sameSite}`
  }
  
  document.cookie = cookieString
}

/**
 * 获取cookie值
 * @param name cookie名称
 * @returns cookie值，如果不存在返回null
 */
export function getCookie(name: string): string | null {
  const nameEQ = encodeURIComponent(name) + '='
  const cookies = document.cookie.split(';')
  
  for (let cookie of cookies) {
    cookie = cookie.trim()
    if (cookie.indexOf(nameEQ) === 0) {
      return decodeURIComponent(cookie.substring(nameEQ.length))
    }
  }
  
  return null
}

/**
 * 删除cookie
 * @param name cookie名称
 * @param options cookie选项（需要与设置时一致）
 */
export function deleteCookie(name: string, options: CookieOptions = {}): void {
  setCookie(name, '', { ...options, expires: new Date(0) })
}

// /**
//  * 从response中提取并设置cookie
//  * @deprecated 后端已通过Set-Cookie自动设置，前端无需手动设置token cookie
//  */
// export function setCookiesFromResponse(response: any): void {
//   // 已废弃：token等cookie由后端Set-Cookie自动设置，前端无需手动设置
// } 