<script setup lang="ts">
import {computed} from 'vue'
import {getAvatarById, getDefaultAvatar} from '@/utils/avatars'

const props = defineProps<{
  avatarId?: string | null
  size?: number
}>()

const avatar = computed(() => {
  return getAvatarById(props.avatarId) || getDefaultAvatar()
})

const avatarSize = computed(() => props.size || 40)
</script>

<template>
  <div
    class="user-avatar"
    :style="{
      backgroundColor: avatar.color,
      width: `${avatarSize}px`,
      height: `${avatarSize}px`,
      fontSize: `${avatarSize * 0.6}px`
    }"
  >
    {{ avatar.emoji }}
  </div>
</template>

<style scoped>
.user-avatar {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border: 2px solid rgba(255, 255, 255, 0.3);
}
</style>

