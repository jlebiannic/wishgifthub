import {describe, expect, it, vi} from 'vitest'
import {shallowMount} from '@vue/test-utils'
import {createPinia} from 'pinia'
import AddWishDialog from '../AddWishDialog.vue'
import EditWishDialog from '../EditWishDialog.vue'

vi.mock('@/api/client', () => ({getApiClient: vi.fn()}))

const imageUrl = 'https://assets.katogroup.eu/i/katogroup/KZ1114A1_01_kizer'
const global = {
  plugins: [createPinia()],
  renderStubDefaultSlot: true,
  stubs: {
    VDialog: true, VCard: true, VCardTitle: true, VCardText: true, VCardActions: true,
    VForm: true, VTextField: {name: 'VTextField', props: ['label', 'rules'], template: '<input />'},
    VTextarea: true, VBtn: true, VIcon: true, VDivider: true, VSpacer: true,
    VImg: true, VAlert: true, VProgressCircular: true, VRow: true
  }
}

describe.each([
  {name: 'AddWishDialog', dialog: AddWishDialog},
  {name: 'EditWishDialog', dialog: EditWishDialog}
])('image URL rules in $name', ({dialog}) => {
  it('accepts extensionless CDN URLs while retaining protocol and length validation', () => {
    const wrapper = shallowMount(dialog, {
      props: {groupId: 'group', modelValue: true, wish: null},
      global
    })
    const field = wrapper.findAllComponents({name: 'VTextField'})
      .find(component => String(component.props('label')).includes('image'))!
    const rules = field.props('rules') as Array<(value: string) => boolean | string>
    for (const value of ['', imageUrl, `${imageUrl}?width=800&format=webp`, 'https://cdn.example.com/image.jpg']) {
      expect(rules.every(rule => rule(value) === true)).toBe(true)
    }
    for (const value of ['ftp://cdn.example.com/image', 'javascript:alert(1)', 'not-a-url', `${imageUrl}${'a'.repeat(2048)}`]) {
      expect(rules.some(rule => rule(value) !== true)).toBe(true)
    }
    wrapper.unmount()
  })
})
