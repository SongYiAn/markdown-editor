import { createApp } from 'vue'
import App from './App.vue'
import './style.css'
import router from './router'

// 应用入口
createApp(App).use(router).mount('#app')
