import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  ElAside, ElButton, ElCard, ElCheckbox, ElCol, ElContainer, ElDatePicker,
  ElDescriptions, ElDescriptionsItem, ElDialog, ElForm, ElFormItem, ElHeader,
  ElInput, ElInputNumber, ElLoading, ElMain, ElMenu, ElMenuItem, ElOption,
  ElPagination, ElRadioButton, ElRadioGroup, ElRow, ElSelect, ElTable, ElTableColumn,
  ElTag, ElUpload
} from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/main.css'

const app = createApp(App)
  .use(createPinia())
  .use(router)
for (const component of [ElAside, ElButton, ElCard, ElCheckbox, ElCol, ElContainer,
  ElDatePicker, ElDescriptions, ElDescriptionsItem, ElDialog, ElForm, ElFormItem,
  ElHeader, ElInput, ElInputNumber, ElLoading, ElMain, ElMenu, ElMenuItem, ElOption,
  ElPagination, ElRadioButton, ElRadioGroup, ElRow, ElSelect, ElTable, ElTableColumn,
  ElTag, ElUpload]) { if (component === ElLoading) app.use(component); else app.component(component.name, component) }
app.mount('#app')
