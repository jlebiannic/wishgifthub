import {ref} from 'vue'
import {defineStore} from 'pinia'
import {getApiClient} from '@/api/client'
import type {ChildRequest, UserResponse} from '@/generated/api/wish/data-contracts'

export const useChildrenStore = defineStore('children', () => {
  const children = ref<UserResponse[]>([])
  const currentGroupId = ref<string | null>(null)
  const isLoading = ref(false)

  function reset() {
    children.value = []
    currentGroupId.value = null
  }

  async function fetchChildren(groupId: string) {
    if (currentGroupId.value !== groupId) children.value = []
    currentGroupId.value = groupId
    isLoading.value = true
    try {
      const response = await getApiClient().getChildren(groupId)
      if (currentGroupId.value === groupId) children.value = response.data
      return response.data
    } finally {
      isLoading.value = false
    }
  }

  async function saveChild(groupId: string, childId: string | null, request: ChildRequest) {
    const api = getApiClient()
    const response = childId
      ? await api.updateChild(groupId, childId, request)
      : await api.createChild(groupId, request)
    if (currentGroupId.value === groupId) {
      children.value = [...children.value.filter(child => child.id !== response.data.id), response.data]
    }
    return response.data
  }

  async function deleteChild(groupId: string, childId: string) {
    await getApiClient().deleteChild(groupId, childId)
    if (currentGroupId.value === groupId) {
      children.value = children.value.filter(child => child.id !== childId)
    }
  }

  return {children, currentGroupId, isLoading, fetchChildren, saveChild, deleteChild, reset}
})
