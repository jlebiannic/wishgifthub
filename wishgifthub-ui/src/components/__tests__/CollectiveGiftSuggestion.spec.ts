import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, mount, shallowMount} from '@vue/test-utils'
import {createPinia, setActivePinia} from 'pinia'
import {createVuetify} from 'vuetify'
import * as components from 'vuetify/components'
import CollectiveGiftFields from '../CollectiveGiftFields.vue'
import AddWishDialog from '../AddWishDialog.vue'
import EditWishDialog from '../EditWishDialog.vue'
import {useGroupStore} from '@/stores/group'
import {useWishStore} from '@/stores/wish'

const api = vi.hoisted(() => ({extractMetadata: vi.fn()}))
vi.mock('@/api/client', () => ({getApiClient: () => api, updateApiToken: vi.fn()}))

const group = {id: 'group', name: 'Noël', type: 'noël', adminId: 'owner',
  createdAt: '2026-10-19T10:00:00Z', collectiveGiftThreshold: 75}
const wish = {id: 'wish', userId: 'owner', groupId: 'group', giftName: 'Livre',
  createdAt: group.createdAt, price: '76 €', collective: false}

beforeEach(() => setActivePinia(createPinia()))
afterEach(() => vi.useRealTimers())

function mountFields(props: {price?: string, threshold?: number, collective?: boolean, active?: boolean} = {}) {
  return mount(CollectiveGiftFields, {
    props,
    global: {plugins: [createVuetify({components})]}
  })
}

describe('collective suggestion', () => {
  it('suggests only strictly above the default 50 euro threshold', async () => {
    const wrapper = mountFields({price: '50 €'})
    expect(wrapper.find('.v-alert').exists()).toBe(false)
    await wrapper.setProps({price: '50,01 €'})
    expect(wrapper.text()).toContain('Souhaitez-vous en faire un cadeau commun')
    await wrapper.setProps({price: '49.99'})
    expect(wrapper.find('.v-alert').exists()).toBe(false)
    await wrapper.setProps({price: 'prix inconnu'})
    expect(wrapper.find('.v-alert').exists()).toBe(false)
    wrapper.unmount()
  })

  it('uses the group threshold, allows refusal, and reconsiders when the amount changes', async () => {
    const wrapper = mountFields({price: '75', threshold: 75})
    expect(wrapper.find('.v-alert').exists()).toBe(false)
    await wrapper.setProps({price: '75,01'})
    await wrapper.findAll('button').find(button => button.text() === 'Non, garder individuel')!.trigger('click')
    expect(wrapper.find('.v-alert').exists()).toBe(false)
    expect(wrapper.emitted('update:collective')).toBeUndefined()
    await wrapper.setProps({price: '80'})
    expect(wrapper.find('.v-alert').exists()).toBe(true)
    await wrapper.setProps({threshold: 100})
    expect(wrapper.find('.v-alert').exists()).toBe(false)
    wrapper.unmount()
  })

  it('accepts with the existing capacity controls and does not prompt an already collective wish', async () => {
    const wrapper = mountFields({price: '100', collective: true})
    expect(wrapper.find('.v-alert').exists()).toBe(false)
    await wrapper.setProps({collective: false})
    await wrapper.findAll('button').find(button => button.text() === 'Oui, cadeau commun')!.trigger('click')
    expect(wrapper.emitted('update:collective')).toEqual([[true]])
    await wrapper.setProps({collective: true})
    expect(wrapper.find('input[type="number"]').exists()).toBe(true)
    expect(wrapper.find('.v-alert').exists()).toBe(false)
    wrapper.unmount()
  })

  it('does not prompt closed dialogs and restores the question on reopening', async () => {
    const wrapper = mountFields({price: '100', active: false})
    expect(wrapper.find('.v-alert').exists()).toBe(false)
    await wrapper.setProps({active: true})
    await wrapper.findAll('button').find(button => button.text() === 'Non, garder individuel')!.trigger('click')
    await wrapper.setProps({active: false})
    await wrapper.setProps({active: true})
    expect(wrapper.find('.v-alert').exists()).toBe(true)
    wrapper.unmount()
  })

  it('allows a zero threshold and never suggests for missing or zero prices', async () => {
    const wrapper = mountFields({threshold: 0})
    expect(wrapper.find('.v-alert').exists()).toBe(false)
    await wrapper.setProps({price: '0 €'})
    expect(wrapper.find('.v-alert').exists()).toBe(false)
    await wrapper.setProps({price: '0,01 €'})
    expect(wrapper.find('.v-alert').exists()).toBe(true)
    wrapper.unmount()
  })
})

const stubs = {
  VDialog: true, VCard: true, VCardText: true, VCardTitle: true, VCardActions: true,
  VForm: {template: '<form><slot /></form>', methods: {validate: async () => ({valid: true}), resetValidation() {}}},
  VTextField: true, VTextarea: true, VDivider: true, VIcon: true, VSpacer: true, VImg: true,
  VAlert: true, VRow: true, VProgressCircular: true, VBtn: {template: '<button><slot /></button>'}
}

describe.each([
  {name: 'add', dialog: AddWishDialog}, {name: 'edit', dialog: EditWishDialog}
])('$name wish suggestion wiring', ({dialog, name}) => {
  it('uses the current group setting and price, and keeps individual wishes allowed', async () => {
    useGroupStore().groups = [group]
    const add = vi.spyOn(useWishStore(), 'addWish').mockResolvedValue(wish)
    const update = vi.spyOn(useWishStore(), 'updateWish').mockResolvedValue(wish)
    const wrapper = shallowMount(dialog, {
      props: {groupId: 'group', modelValue: true, wish},
      global: {renderStubDefaultSlot: true, stubs}
    })
    const fields = wrapper.getComponent(CollectiveGiftFields)
    expect(fields.props('threshold')).toBe(75)
    const inputs = wrapper.findAllComponents({name: 'VTextField'})
    inputs.find(field => field.attributes('label') === 'Prix estimé (optionnel)')!.vm.$emit('update:modelValue', '80,01 €')
    inputs.find(field => field.attributes('label') === 'Titre *')!.vm.$emit('update:modelValue', 'Livre')
    await flushPromises()
    expect(fields.props('price')).toBe('80,01 €')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    if (name === 'add') expect(add).toHaveBeenCalledWith('group', expect.objectContaining({collective: false, price: '80,01 €'}))
    else expect(update).toHaveBeenCalledWith('group', 'wish', expect.objectContaining({collective: false, price: '80,01 €'}))
    wrapper.unmount()
  })

  it('uses the price supplied automatically by URL metadata', async () => {
    vi.useFakeTimers()
    useGroupStore().groups = [group]
    api.extractMetadata.mockResolvedValueOnce({data: {title: 'Livre', price: '80,01 €'}})
    const wrapper = shallowMount(dialog, {
      props: {groupId: 'group', modelValue: true, wish: {...wish, price: ''}},
      global: {renderStubDefaultSlot: true, stubs}
    })
    wrapper.findAllComponents({name: 'VTextField'})
      .find(field => field.attributes('label') === 'URL du produit (optionnel)')!
      .vm.$emit('update:modelValue', 'https://example.com/product')
    await flushPromises()
    await vi.advanceTimersByTimeAsync(1000)
    await flushPromises()
    expect(api.extractMetadata).toHaveBeenCalledWith({url: 'https://example.com/product'})
    const fields = wrapper.getComponent(CollectiveGiftFields)
    expect(fields.props('price')).toBe('80,01 €')
    expect(fields.props('threshold')).toBe(75)
    wrapper.unmount()
  })
})
