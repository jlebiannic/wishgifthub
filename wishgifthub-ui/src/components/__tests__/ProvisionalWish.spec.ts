import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount, shallowMount} from '@vue/test-utils'
import {createPinia, setActivePinia} from 'pinia'
import {createVuetify} from 'vuetify'
import * as components from 'vuetify/components'
import AddWishDialog from '../AddWishDialog.vue'
import EditWishDialog from '../EditWishDialog.vue'
import ProvisionalWishField from '../ProvisionalWishField.vue'
import MemberCard from '../MemberCard.vue'
import DashboardView from '@/views/DashboardView.vue'
import {useWishStore} from '@/stores/wish'
import {useAuthStore} from '@/stores/auth'
import {useGroupStore} from '@/stores/group'
import type {WishResponse} from '@/generated/api/wish/data-contracts'

vi.mock('vue-router', () => ({
  useRoute: () => ({params: {groupId: 'group'}}),
  useRouter: () => ({push: vi.fn()})
}))
vi.mock('@/api/client', () => ({getApiClient: vi.fn(), updateApiToken: vi.fn()}))

const wish: WishResponse = {
  id: 'wish', userId: 'child', groupId: 'group', giftName: 'Surprise',
  createdAt: '2026-10-19T10:00:00Z', provisional: true, collective: false
}
const child = {id: 'child', parentId: 'parent', pseudo: 'Camille', createdAt: wish.createdAt}
const parent = {id: 'parent', email: 'parent@example.com', createdAt: wish.createdAt}
const stubs = {
  VDialog: true, VCard: true, VCardText: true, VCardTitle: true, VCardActions: true,
  VForm: {template: '<form><slot /></form>', methods: {validate: async () => ({valid: true}), resetValidation() {}}},
  VTextField: true, VTextarea: true, VDivider: true, VIcon: true, VSpacer: true,
  VImg: true, VAlert: true, VRow: true, VCol: true, VContainer: true,
  VExpandTransition: true, VProgressCircular: true, VChip: true, VTooltip: true,
  VProgressLinear: true, VTab: true, VTabs: true, VAvatar: true, VList: true,
  VListItem: true, VListItemTitle: true, VListItemSubtitle: true, VWindowItem: true, VWindow: true,
  UserAvatar: true, VBtn: {template: '<button><slot /></button>'}
}
const global = {renderStubDefaultSlot: true, stubs}

beforeEach(() => {
  setActivePinia(createPinia())
  useAuthStore().user = {id: 'parent', username: 'parent', email: 'parent@example.com', roles: ['USER']}
})

describe('provisional wishes', () => {
  it('offers the exact visibility option and explains publication', async () => {
    const wrapper = mount(ProvisionalWishField, {
      global: {plugins: [createVuetify({components})]}
    })
    expect(wrapper.text()).toContain('Souhait provisoire (non visible par les autres membres)')
    expect(wrapper.text()).toContain('Décochez pour le publier')
    await wrapper.get('input[type="checkbox"]').setValue(true)
    expect(wrapper.emitted('update:modelValue')).toEqual([[true]])
    await wrapper.setProps({disabled: true})
    expect(wrapper.get('input[type="checkbox"]').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('ne peut pas être rendu provisoire')
    wrapper.unmount()
  })

  it('defaults to published, creates a provisional child wish, and resets after success', async () => {
    const add = vi.spyOn(useWishStore(), 'addWish').mockResolvedValue(wish)
    const wrapper = shallowMount(AddWishDialog, {
      props: {modelValue: true, groupId: 'group', targetUserId: 'child'},
      global
    })
    const field = wrapper.getComponent(ProvisionalWishField)
    expect(field.props('modelValue')).toBe(false)
    field.vm.$emit('update:modelValue', true)
    wrapper.findAllComponents({name: 'VTextField'}).find(input => input.attributes('label') === 'Titre *')!
      .vm.$emit('update:modelValue', 'Surprise')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(add).toHaveBeenCalledWith('group', expect.objectContaining({userId: 'child', provisional: true}))
    expect(field.props('modelValue')).toBe(false)
    expect(wrapper.emitted('wishAdded')).toHaveLength(1)
  })

  it('discards a provisional creation when closing', async () => {
    const wrapper = shallowMount(AddWishDialog, {props: {modelValue: true, groupId: 'group'}, global})
    wrapper.getComponent(ProvisionalWishField).vm.$emit('update:modelValue', true)
    await wrapper.setProps({modelValue: false})
    await wrapper.setProps({modelValue: true})
    expect(wrapper.getComponent(ProvisionalWishField).props('modelValue')).toBe(false)
  })

  it('loads a provisional wish, publishes it, and restores saved status on reopening', async () => {
    const update = vi.spyOn(useWishStore(), 'updateWish').mockResolvedValue({...wish, provisional: false})
    const wrapper = shallowMount(EditWishDialog, {
      props: {modelValue: true, groupId: 'group', wish}, global
    })
    const field = wrapper.getComponent(ProvisionalWishField)
    expect(field.props('modelValue')).toBe(true)
    field.vm.$emit('update:modelValue', false)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(update).toHaveBeenCalledWith('group', 'wish', expect.objectContaining({provisional: false}))
    await wrapper.setProps({modelValue: false})
    await wrapper.setProps({modelValue: true})
    expect(field.props('modelValue')).toBe(true)
  })

  it('can make an unreserved published wish provisional without closing on a save failure', async () => {
    const update = vi.spyOn(useWishStore(), 'updateWish')
      .mockRejectedValueOnce({response: {data: {message: 'Le souhait vient d’être réservé'}}})
      .mockResolvedValue(wish)
    const wrapper = shallowMount(EditWishDialog, {
      props: {modelValue: true, groupId: 'group', wish: {...wish, provisional: false}}, global
    })
    const field = wrapper.getComponent(ProvisionalWishField)
    expect(field.props('disabled')).toBe(false)
    field.vm.$emit('update:modelValue', true)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(update).toHaveBeenCalledWith('group', 'wish', expect.objectContaining({provisional: true}))
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
    expect(field.props('modelValue')).toBe(true)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.emitted('wishUpdated')).toHaveLength(1)
  })

  it.each([
    {reservedBy: 'member'}, {collective: true, participantCount: 1},
    {collective: true, participants: [parent]}
  ])('prevents hiding wishes with active reservations %j', reservations => {
    const wrapper = shallowMount(EditWishDialog, {
      props: {modelValue: true, groupId: 'group', wish: {...wish, provisional: false, ...reservations}}, global
    })
    expect(wrapper.getComponent(ProvisionalWishField).props('disabled')).toBe(true)
    expect(wrapper.getComponent(ProvisionalWishField).props('modelValue')).toBe(false)
  })

  it.each([false, true])('shows a badge but no reservation for a managed provisional wish (collective=%s)', async collective => {
    const wrapper = shallowMount(MemberCard, {
      props: {
        member: child, wishes: [{...wish, collective, full: false, participating: false}],
        groupId: 'group', isCurrentUser: false, isManagedChild: true, allMembers: [child, parent]
      },
      global
    })
    expect(wrapper.text()).toContain('Souhait provisoire')
    expect(wrapper.findAll('button').some(button => /^(Réserver|Participer|Annuler ma participation)$/.test(button.text()))).toBe(false)
    expect(wrapper.find('[aria-label="Modifier le souhait"]').exists()).toBe(true)
    await wrapper.setProps({wishes: [{...wish, provisional: false, collective, full: false, participating: false}]})
    expect(wrapper.text()).not.toContain('Souhait provisoire')
    expect(wrapper.findAll('button').some(button => /^(Réserver|Participer)$/.test(button.text()))).toBe(true)
  })

  it('labels provisional wishes on the owner dashboard', async () => {
    useGroupStore().members = [parent]
    vi.spyOn(useGroupStore(), 'fetchGroupMembers').mockResolvedValue([parent])
    vi.spyOn(useWishStore(), 'fetchMyWishes').mockResolvedValue([{...wish, userId: 'parent'}])
    vi.spyOn(useWishStore(), 'fetchGroupWishes').mockResolvedValue([{...wish, userId: 'parent'}])
    const wrapper = shallowMount(DashboardView, {global})
    await flushPromises()
    expect(wrapper.text()).toContain('Souhait provisoire')
  })
})
