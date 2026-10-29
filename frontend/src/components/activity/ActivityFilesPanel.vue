<script setup lang="ts">
import { ref } from 'vue'
import { ElButton, ElMessage, ElMessageBox } from 'element-plus'

import AppIcon from '@/components/common/AppIcon.vue'
import { ApiError } from '@/api/http'
import { adminAttachmentUrl, deleteAttachment, uploadAttachment, uploadCover } from '@/api/admin/files'
import type { AttachmentSummary } from '@/types/activity'
import { formatFileSize } from '@/utils/format'

const props = defineProps<{
  activityId: string
  coverUrl: string | null
  attachments: AttachmentSummary[]
}>()

const emit = defineEmits<{ changed: [] }>()

const coverInput = ref<HTMLInputElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const busy = ref(false)

async function onCover(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('封面不能超过 5MB')
    input.value = ''
    return
  }
  busy.value = true
  try {
    await uploadCover(props.activityId, file)
    ElMessage.success('封面已更新')
    emit('changed')
  } catch (caught) {
    ElMessage.error(caught instanceof ApiError ? caught.message : '封面上传失败')
  } finally {
    busy.value = false
    input.value = ''
  }
}

async function onAttachment(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const extension = file.name.split('.').pop()?.toLowerCase() ?? ''
  if (!['pdf', 'docx', 'pptx'].includes(extension)) {
    ElMessage.error('附件仅支持 PDF、DOCX、PPTX')
    input.value = ''
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.error('单个附件不能超过 20MB')
    input.value = ''
    return
  }
  busy.value = true
  try {
    await uploadAttachment(props.activityId, file)
    ElMessage.success('附件已上传')
    emit('changed')
  } catch (caught) {
    ElMessage.error(caught instanceof ApiError ? caught.message : '附件上传失败')
  } finally {
    busy.value = false
    input.value = ''
  }
}

async function remove(item: AttachmentSummary): Promise<void> {
  try {
    await ElMessageBox.confirm(`删除附件「${item.originalName}」？删除后不能恢复。`, '删除附件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  busy.value = true
  try {
    await deleteAttachment(item.id)
    ElMessage.success('附件已删除')
    emit('changed')
  } catch (caught) {
    ElMessage.error(caught instanceof ApiError ? caught.message : '删除失败')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="files">
    <div class="files__block">
      <h3 class="files__title">活动封面</h3>
      <div class="files__cover">
        <img v-if="coverUrl" :src="coverUrl" alt="活动封面" />
        <span v-else>尚未上传封面</span>
      </div>
      <div class="files__actions">
        <ElButton :disabled="busy" @click="coverInput?.click()">上传封面</ElButton>
        <span class="files__hint">PNG、JPEG、GIF 或 WEBP，不超过 5MB</span>
      </div>
      <input
        ref="coverInput"
        class="files__input"
        type="file"
        accept="image/png,image/jpeg,image/gif,image/webp"
        @change="onCover"
      />
    </div>

    <div class="files__block">
      <h3 class="files__title">活动附件</h3>
      <ul v-if="attachments.length > 0" class="files__list">
        <li v-for="item in attachments" :key="item.id">
          <AppIcon name="inbox" :size="16" />
          <span class="files__name">{{ item.originalName }}</span>
          <span class="files__size mono">{{ formatFileSize(item.sizeBytes) }}</span>
          <a class="files__link" :href="adminAttachmentUrl(item.id)">下载</a>
          <button class="files__link files__link--danger" type="button" :disabled="busy" @click="remove(item)">
            删除
          </button>
        </li>
      </ul>
      <p v-else class="files__hint">还没有附件。上传失败时活动草稿会保留，可以重试。</p>
      <div class="files__actions">
        <ElButton :disabled="busy" @click="fileInput?.click()">上传附件</ElButton>
        <span class="files__hint">PDF、DOCX 或 PPTX，单个不超过 20MB</span>
      </div>
      <input
        ref="fileInput"
        class="files__input"
        type="file"
        accept=".pdf,.docx,.pptx,application/pdf"
        @change="onAttachment"
      />
    </div>
  </section>
</template>

<style scoped>
.files {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.files__title {
  margin-bottom: var(--space-2);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.files__cover {
  display: grid;
  place-items: center;
  width: min(100%, 360px);
  height: 180px;
  overflow: hidden;
  border: 1px dashed var(--border);
  border-radius: var(--radius-card);
  background: var(--bg-subtle);
  color: var(--text-subtle);
}

.files__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.files__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.files__hint {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.files__input {
  display: none;
}

.files__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.files__list li {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-control);
}

.files__name {
  flex: 1;
  min-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.files__size,
.files__link {
  color: var(--text-subtle);
  font-size: var(--font-size-xs);
}

.files__link {
  border: 0;
  background: transparent;
  cursor: pointer;
}

.files__link--danger {
  color: var(--danger);
}
</style>
