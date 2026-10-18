import {beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount, shallowMount} from '@vue/test-utils'
import {createPinia, setActivePinia} from 'pinia'
import {createVuetify} from 'vuetify'
import * as components from 'vuetify/components'
import CollectiveGiftFields from '../CollectiveGiftFields.vue'
import AddWishDialog from '../AddWishDialog.vue'
import EditWishDialog from '../EditWishDialog.vue'
import MemberCard from '../MemberCard.vue'
import {useAuthStore} from '@/stores/auth'
import {useWishStore} from '@/stores/wish'
import {useNotificationStore} from '@/stores/notification'
import type {WishResponse} from '@/generated/api/wish/data-contracts'

const wish: WishResponse = {
  id: 'wish', userId: 'owner', groupId: 'group', giftName: 'Shared gift',
  createdAt: '2026-10-18T10:00:00Z', collective: true, maxParticipants: 2,
  participantCount: 0, participating: false, full: false
}
const member = {id: 'owner', email: 'owner@example.com', createdAt: wish.createdAt}
const stubs = {
  VDialog: true, VCard: true, VCardText: true, VCardTitle: true, VCardActions: true,
  VForm: {template: '<form><slot /></form>', methods: {validate: async () => ({valid: true}), resetValidation() {}}},
  VTextField: true, VTextarea: true, VDivider: true, VIcon: true, VSpacer: true,
  VImg: true, VAlert: true, VRow: true, VCol: true, VExpandTransition: true, VProgressCircular: true,
  VChip: true, VTooltip: true, UserAvatar: true,
  VBtn: {template: '<button><slot /></button>'}
}

function mountMember(overrides: Partial<WishResponse> = {}, own = false) {
  return shallowMount(MemberCard, {
    props: {member, wishes: [{...wish, ...overrides}], groupId: 'group', isCurrentUser: own, allMembers: [member]},
    global: {renderStubDefaultSlot: true, stubs}
  })
}

beforeEach(() => {
  setActivePinia(createPinia())
  const auth = useAuthStore()
  auth.user = {id: 'viewer', username: 'viewer', email: 'viewer@example.com', roles: []}
})

describe('collective gift controls', () => {
  it('defaults off, reveals an accessible minimum-2 stepper, and rejects invalid input', async () => {
    const wrapper = mount(CollectiveGiftFields, {
      global: {plugins: [createVuetify({components})]}
    })
    expect(wrapper.find('input[type="number"]').exists()).toBe(false)
    await wrapper.get('input[type="checkbox"]').setValue(true)
    const input = wrapper.get('input[type="number"]')
    expect((input.element as HTMLInputElement).value).toBe('2')
    expect(input.attributes('min')).toBe('2')
    const minus = wrapper.get('[aria-label="Diminuer le nombre de participants"]')
    expect(minus.attributes('disabled')).toBeDefined()
    await wrapper.get('[aria-label="Augmenter le nombre de participants"]').trigger('click')
    expect((input.element as HTMLInputElement).value).toBe('3')
    await minus.trigger('click')
    expect((input.element as HTMLInputElement).value).toBe('2')
    await input.setValue('1.5')
    await input.trigger('blur')
    await flushPromises()
    expect(wrapper.text()).toContain('Saisissez un nombre entier')
    expect(wrapper.text()).toContain('Permettre à plusieurs personnes de se regrouper pour offrir ce cadeau.')
    wrapper.unmount()
  })

  it('creation submits collective configuration and resets it after success', async () => {
    const add = vi.spyOn(useWishStore(), 'addWish').mockResolvedValue(wish)
    const wrapper = shallowMount(AddWishDialog, {
      props: {modelValue: true, groupId: 'group'},
      global: {renderStubDefaultSlot: true, stubs}
    })
    const fields = wrapper.getComponent(CollectiveGiftFields)
    fields.vm.$emit('update:collective', true)
    fields.vm.$emit('update:maxParticipants', 5)
    wrapper.findAllComponents({name: 'VTextField'}).find(field => field.attributes('label') === 'Titre *')!
      .vm.$emit('update:modelValue', 'Shared gift')
    await flushPromises()
    await wrapper.findAll('button').find(button => button.text().includes('Ajouter'))!.trigger('click')
    await flushPromises()
    expect(add).toHaveBeenCalledWith('group', expect.objectContaining({collective: true, maxParticipants: 5}))
    expect(fields.props('collective')).toBe(false)
    expect(fields.props('maxParticipants')).toBe(2)
  })

  it('editing loads saved settings and submits updated capacity', async () => {
    const update = vi.spyOn(useWishStore(), 'updateWish').mockResolvedValue(wish)
    const wrapper = shallowMount(EditWishDialog, {
      props: {modelValue: true, groupId: 'group', wish: {...wish, maxParticipants: 4}},
      global: {renderStubDefaultSlot: true, stubs}
    })
    const fields = wrapper.getComponent(CollectiveGiftFields)
    expect(fields.props('collective')).toBe(true)
    expect(fields.props('maxParticipants')).toBe(4)
    fields.vm.$emit('update:maxParticipants', 6)
    await wrapper.findAll('button').find(button => button.text() === 'Enregistrer')!.trigger('click')
    await flushPromises()
    expect(update).toHaveBeenCalledWith('group', 'wish', expect.objectContaining({collective: true, maxParticipants: 6}))
  })

  it('reopening the editor discards cancelled collective settings', async () => {
    const wrapper = shallowMount(EditWishDialog, {
      props: {modelValue: true, groupId: 'group', wish},
      global: {renderStubDefaultSlot: true, stubs}
    })
    const fields = wrapper.getComponent(CollectiveGiftFields)
    fields.vm.$emit('update:maxParticipants', 9)
    await wrapper.setProps({modelValue: false})
    await wrapper.setProps({modelValue: true})
    expect(fields.props('maxParticipants')).toBe(2)
  })
})

describe('collective reservation states', () => {
  it('shows availability and uses the existing reservation endpoint to participate', async () => {
    const reserve = vi.spyOn(useWishStore(), 'reserveWish').mockResolvedValue({...wish, participantCount: 1, participating: true})
    const wrapper = mountMember()
    expect(wrapper.text()).toContain('Cadeau commun')
    expect(wrapper.text()).toContain('0 / 2 participants')
    await wrapper.findAll('button').find(button => button.text() === 'Participer')!.trigger('click')
    await flushPromises()
    expect(reserve).toHaveBeenCalledWith('group', 'wish')
    expect(wrapper.emitted('wishUpdated')).toHaveLength(1)
  })

  it('shows own participation and allows cancelling even when full', async () => {
    const cancel = vi.spyOn(useWishStore(), 'unreserveWish').mockResolvedValue(wish)
    const wrapper = mountMember({participantCount: 2, full: true, participating: true})
    expect(wrapper.text()).toContain('Complet')
    expect(wrapper.text()).toContain('Vous participez')
    expect(wrapper.findAll('button').some(button => button.text() === 'Participer')).toBe(false)
    await wrapper.findAll('button').find(button => button.text() === 'Annuler ma participation')!.trigger('click')
    await flushPromises()
    expect(cancel).toHaveBeenCalledWith('group', 'wish')
    await wrapper.setProps({wishes: [wish]})
    expect(wrapper.findAll('button').some(button => button.text() === 'Participer')).toBe(true)
  })

  it('does not offer participation or cancellation to a non-participant when full', () => {
    const wrapper = mountMember({participantCount: 2, full: true})
    expect(wrapper.text()).toContain('Complet')
    expect(wrapper.findAll('button').some(button => /Participer|Annuler ma participation/.test(button.text()))).toBe(false)
  })

  it('does not reveal reservation activity to the recipient, even from stale data', () => {
    useAuthStore().user!.id = 'owner'
    const wrapper = mountMember({participantCount: 2, full: true, participating: true, reservedBy: 'viewer'}, true)
    expect(wrapper.text()).toContain('Cadeau commun')
    expect(wrapper.text()).not.toMatch(/participants|Complet|Vous participez|Réservé par/)
    expect(wrapper.findAll('button').some(button => /Participer|Annuler ma participation/.test(button.text()))).toBe(false)
  })

  it('keeps individual reservation actions unchanged', () => {
    const wrapper = mountMember({collective: false, reservedBy: null})
    expect(wrapper.findAll('button').some(button => button.text() === 'Réserver')).toBe(true)
    expect(wrapper.text()).not.toContain('Cadeau commun')
  })

  it('reports concurrent capacity conflicts and requests fresh state', async () => {
    vi.spyOn(useWishStore(), 'reserveWish').mockRejectedValue({
      response: {status: 409, data: {message: 'Ce cadeau commun est complet'}}
    })
    const wrapper = mountMember()
    await wrapper.findAll('button').find(button => button.text() === 'Participer')!.trigger('click')
    await flushPromises()
    expect(wrapper.emitted('wishUpdated')).toHaveLength(1)
    expect(useNotificationStore().notifications.at(-1)?.message).toBe('Ce cadeau commun est complet')
  })
})
