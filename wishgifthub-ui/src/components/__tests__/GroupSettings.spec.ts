import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, shallowMount} from '@vue/test-utils'
import {createPinia, setActivePinia} from 'pinia'
import GroupSettingsDialog from '../GroupSettingsDialog.vue'
import CreateGroupButton from '../CreateGroupButton.vue'
import GroupMembersView from '@/views/GroupMembersView.vue'
import {useGroupStore} from '@/stores/group'
import {useNotificationStore} from '@/stores/notification'
import {useAuthStore} from '@/stores/auth'
import {useWishStore} from '@/stores/wish'
import {useChildrenStore} from '@/stores/children'

const api = vi.hoisted(() => ({updateGroup: vi.fn(), createGroup: vi.fn()}))
vi.mock('@/api/client', () => ({getApiClient: () => api, updateApiToken: vi.fn()}))
vi.mock('vue-router', () => ({
  useRoute: () => ({params: {groupId: 'group'}, hash: ''}),
  useRouter: () => ({push: vi.fn()})
}))

const group = {id: 'group', name: 'Famille', type: 'noël', adminId: 'owner',
  createdAt: '2026-10-19T10:00:00Z', collectiveGiftThreshold: 50}
const global = {
  renderStubDefaultSlot: true,
  stubs: {
    VDialog: true, VCard: true, VCardTitle: true, VCardText: true, VCardActions: true,
    VForm: {template: '<form><slot /></form>'}, VTextField: true, VSpacer: true,
    VDivider: true, VAlert: true, VIcon: true, VRow: true, VCol: true, VContainer: true,
    VProgressCircular: true, VBtn: {template: '<button><slot /></button>'}
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  useGroupStore().groups = [group]
  api.updateGroup.mockResolvedValue({data: {...group, collectiveGiftThreshold: 75.5}})
})

describe('group threshold settings', () => {
  it.each(['USER', 'ADMIN'])('loads the joined group setting for a %s without exposing another owner’s settings', async role => {
    useAuthStore().user = {id: 'invited', username: 'invited', email: 'invited@test', roles: [role]}
    useGroupStore().groups = []
    vi.spyOn(useGroupStore(), 'fetchMyGroups').mockImplementation(async () => {
      useGroupStore().groups = [{...group, collectiveGiftThreshold: 75}]
      return useGroupStore().groups
    })
    const ownedGroups = vi.spyOn(useGroupStore(), 'fetchGroups').mockResolvedValue([])
    vi.spyOn(useGroupStore(), 'fetchGroupMembers').mockResolvedValue([])
    vi.spyOn(useWishStore(), 'fetchGroupWishes').mockResolvedValue([])
    vi.spyOn(useChildrenStore(), 'fetchChildren').mockResolvedValue([])
    const wrapper = shallowMount(GroupMembersView, {global})
    await flushPromises()
    expect(useGroupStore().groups[0]?.collectiveGiftThreshold).toBe(75)
    expect(ownedGroups).not.toHaveBeenCalled()
    expect(wrapper.findComponent(GroupSettingsDialog).exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Paramètres du groupe')
    wrapper.unmount()
  })

  it('persists the setting and updates the group store immediately', async () => {
    const wrapper = shallowMount(GroupSettingsDialog, {props: {group, modelValue: true}, global})
    wrapper.getComponent({name: 'VTextField'}).vm.$emit('update:modelValue', '75,50')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(api.updateGroup).toHaveBeenCalledWith('group', {
      name: 'Famille', type: 'noël', collectiveGiftThreshold: 75.5
    })
    expect(useGroupStore().groups[0]?.collectiveGiftThreshold).toBe(75.5)
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })

  it.each(['', '-1', '1.001', '100000000'])('rejects invalid input %s without saving', async value => {
    const notify = vi.spyOn(useNotificationStore(), 'showError')
    const wrapper = shallowMount(GroupSettingsDialog, {props: {group, modelValue: true}, global})
    wrapper.getComponent({name: 'VTextField'}).vm.$emit('update:modelValue', value)
    await wrapper.get('form').trigger('submit')
    expect(api.updateGroup).not.toHaveBeenCalled()
    expect(notify).toHaveBeenCalled()
  })

  it('reports save failure, preserves settings, and discards cancelled changes on reopening', async () => {
    const notify = vi.spyOn(useNotificationStore(), 'showError')
    api.updateGroup.mockRejectedValueOnce(new Error('offline'))
    const wrapper = shallowMount(GroupSettingsDialog, {props: {group, modelValue: true}, global})
    wrapper.getComponent({name: 'VTextField'}).vm.$emit('update:modelValue', '80')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(notify).toHaveBeenCalled()
    expect(useGroupStore().groups[0]?.collectiveGiftThreshold).toBe(50)
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
    await wrapper.setProps({modelValue: false})
    await wrapper.setProps({modelValue: true})
    expect(wrapper.getComponent({name: 'VTextField'}).attributes('modelvalue')).toBe('50')
  })

  it('creates a group with 50 euro by default or a custom threshold', async () => {
    const create = vi.spyOn(useGroupStore(), 'createGroup').mockResolvedValue(true)
    const wrapper = shallowMount(CreateGroupButton, {global})
    const inputs = wrapper.findAllComponents({name: 'VTextField'})
    inputs[0]!.vm.$emit('update:modelValue', 'Famille')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(create).toHaveBeenLastCalledWith('Famille', 'noël', 50)
    inputs[0]!.vm.$emit('update:modelValue', 'Autre')
    inputs[1]!.vm.$emit('update:modelValue', '70,01')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(create).toHaveBeenLastCalledWith('Autre', 'noël', 70.01)
  })
})
