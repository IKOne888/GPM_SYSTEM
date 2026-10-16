import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
// 全局主题样式（设计令牌 + Element Plus 主题变量覆盖）
import './styles/theme.css'

const app = createApp(App)

// 全量注册 Element Plus 图标，模板中可直接使用 <Reading /> 等
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册 Pinia（用户信息、任务统计缓存）
app.use(createPinia())
// 注册路由（含登录守卫）
app.use(router)
// 注册 Element Plus，使用中文语言包
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
