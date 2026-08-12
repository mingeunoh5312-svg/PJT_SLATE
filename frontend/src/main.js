import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/slate.css'
import './styles/theme.css'

createApp(App).use(router).mount('#app')
