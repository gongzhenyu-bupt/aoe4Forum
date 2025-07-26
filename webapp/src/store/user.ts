import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    isLogin: false,
    userInfo: {
      avatar: '',
      name: ''
    }
  }),
  actions: {
    setUser(user: any) {
      this.isLogin = true
      this.userInfo = user
    },
    logout() {
      this.isLogin = false
      this.userInfo = { avatar: '', name: '' }
    }
  }
}) 