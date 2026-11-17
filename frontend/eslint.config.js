import js from '@eslint/js'
import vue from 'eslint-plugin-vue'
import globals from 'globals'
export default [
 {ignores:['dist/**','node_modules/**','playwright-report/**','test-results/**']},
 js.configs.recommended,
 ...vue.configs['flat/essential'],
 {files:['**/*.{js,vue}'],languageOptions:{globals:{...globals.browser,...globals.node},parserOptions:{ecmaVersion:'latest',sourceType:'module'}},rules:{'no-unused-vars':'off','no-empty':['error',{allowEmptyCatch:true}],'vue/multi-word-component-names':'off'}},
 {files:['**/*.vue'],languageOptions:{globals:{defineProps:'readonly',defineEmits:'readonly',defineExpose:'readonly'}}}
]
