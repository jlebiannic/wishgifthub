import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, shallowMount} from '@vue/test-utils'
import {createPinia, setActivePinia} from 'pinia'
import InvitationsDialog from '../InvitationsDialog.vue'
import {useNotificationStore} from '@/stores/notification'
import type {GroupMember, Invitation} from '@/stores/group'

const member = {
  id: 'member-1',
  email: 'member@example.com',
  createdAt: '2026-10-18T10:00:00Z'
} satisfies GroupMember

const invitation: Invitation = {
  id: 'invitation-1',
  groupId: 'group-1',
  email: member.email,
  token: 'invitation-token',
  accepted: true,
  createdAt: member.createdAt
}

function mountDialog(isAdmin = true, invitations: Invitation[] = [invitation]) {
  return shallowMount(InvitationsDialog, {
    props: {
      groupId: 'group-1',
      members: [member],
      invitations,
      isAdmin,
      isLoading: false
    },
    global: {
      renderStubDefaultSlot: true,
      stubs: {
        VCard: true,
        VCardTitle: true,
        VCardText: true,
        VCardActions: true,
        VIcon: true,
        VDivider: true,
        VDialog: true,
        VForm: true,
        VRow: true,
        VCol: true,
        VTextField: true,
        VList: true,
        VListItemTitle: true,
        VListItemSubtitle: true,
        VProgressLinear: true,
        VChip: true,
        VSpacer: true,
        VListItem: {
          template: '<div><slot name="prepend" /><slot /><slot name="append" /></div>'
        },
        VTooltip: {
          template: '<div><slot name="activator" :props="{}" /></div>'
        },
        VBtn: {
          template: '<button><slot /></button>'
        }
      }
    }
  })
}

const copyButton = '[aria-label="Copier le lien d\'invitation"]'
const resendButton = '[aria-label="Renvoyer l’invitation sur WhatsApp"]'
const publicInvitationLink = `https://wishgifthub.com/wishgifthub/invite/${invitation.token}`

describe('InvitationsDialog', () => {
  const writeText = vi.fn()

  beforeEach(() => {
    setActivePinia(createPinia())
    writeText.mockReset().mockResolvedValue(undefined)
    vi.stubGlobal('navigator', {clipboard: {writeText}})
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
    vi.clearAllTimers()
    vi.useRealTimers()
  })

  it('allows an admin to copy an accepted invitation for an active member', async () => {
    const wrapper = mountDialog()

    await wrapper.get(copyButton).trigger('click')
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith(publicInvitationLink)
    expect(useNotificationStore().notifications.at(-1)?.type).toBe('success')
  })

  it('does not expose the copy action to non-admins', () => {
    expect(mountDialog(false).find(copyButton).exists()).toBe(false)
  })

  it('opens the WhatsApp dialog for an accepted invitation', async () => {
    const wrapper = mountDialog()
    const dialog = wrapper.get('v-dialog-stub')

    expect(dialog.attributes('modelvalue')).toBe('false')
    await wrapper.get(resendButton).trigger('click')
    expect(dialog.attributes('modelvalue')).toBe('true')
  })

  it('only offers WhatsApp resending for admins with a matching accepted invitation', () => {
    expect(mountDialog(false).find(resendButton).exists()).toBe(false)
    expect(mountDialog(true, [{...invitation, token: ''}]).find(resendButton).exists()).toBe(false)
  })

  it('opens WhatsApp in a new tab with the original invitation link', async () => {
    const replace = vi.fn()
    const tab = {location: {replace}, opener: window}
    const open = vi.spyOn(window, 'open').mockReturnValue(tab as unknown as Window)
    vi.stubGlobal('fetch', vi.fn(async () => {
      expect(open).toHaveBeenCalledWith('about:blank', '_blank')
      return new Response('Salut ! 😊\n\n{{lien_invitation}}')
    }))
    const wrapper = mountDialog()
    const originalLocation = window.location.href

    await wrapper.get(resendButton).trigger('click')
    await wrapper.get('v-dialog-stub').get('v-form-stub').trigger('submit')
    await flushPromises()

    expect(tab.opener).toBeNull()
    expect(replace).toHaveBeenCalledWith(
      `https://api.whatsapp.com/send?text=${encodeURIComponent(`Salut ! 😊\n\n${publicInvitationLink}`)}`
    )
    expect(window.location.href).toBe(originalLocation)
  })

  it('still allows copying a pending invitation', async () => {
    const wrapper = mountDialog(true, [{...invitation, accepted: false}])
    const button = wrapper.findAll('button').find(button => button.text().includes('mdi-content-copy'))
    expect(button).toBeDefined()

    await button!.trigger('click')
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith(publicInvitationLink)
  })

  it.each([
    [],
    [{...invitation, email: 'another@example.com'}],
    [{...invitation, groupId: 'another-group'}],
    [{...invitation, token: ''}],
    [{...invitation, accepted: false}]
  ])('does not add an active-member copy action without a matching accepted invitation: %j', (...invitations) => {
    expect(mountDialog(true, invitations).find(copyButton).exists()).toBe(false)
  })

  it('reports clipboard failures instead of announcing success', async () => {
    writeText.mockRejectedValue(new Error('Clipboard unavailable'))
    const wrapper = mountDialog()

    await wrapper.get(copyButton).trigger('click')
    await flushPromises()

    expect(useNotificationStore().notifications.map(notification => notification.type)).toEqual(['error'])
  })

  it('copies on HTTP when Clipboard API is unavailable and removes the temporary field', async () => {
    vi.stubGlobal('navigator', {})
    const copy = vi.fn(() => true)
    Object.defineProperty(document, 'execCommand', {configurable: true, value: copy})
    const wrapper = mountDialog()
    try {
      await wrapper.get(copyButton).trigger('click')
      await flushPromises()
      expect(copy).toHaveBeenCalledWith('copy')
      expect(document.body.querySelector('textarea')).toBeNull()
      expect(useNotificationStore().notifications.at(-1)?.type).toBe('success')
    } finally {
      Reflect.deleteProperty(document, 'execCommand')
    }
  })
})
