import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {flushPromises, shallowMount} from '@vue/test-utils'
import {createPinia, setActivePinia} from 'pinia'
import ChildrenDialog from '../ChildrenDialog.vue'
import AvatarSelector from '../AvatarSelector.vue'
import AddWishDialog from '../AddWishDialog.vue'
import MemberCard from '../MemberCard.vue'
import GroupMembersView from '@/views/GroupMembersView.vue'
import {useChildrenStore} from '@/stores/children'
import {useAuthStore} from '@/stores/auth'
import {useConfirmStore} from '@/stores/confirm'
import {useNotificationStore} from '@/stores/notification'
import {useGroupStore} from '@/stores/group'
import {useWishStore} from '@/stores/wish'
import type {UserResponse, WishResponse} from '@/generated/api/wish/data-contracts'

const api = vi.hoisted(() => ({
  getChildren: vi.fn(), createChild: vi.fn(), updateChild: vi.fn(), deleteChild: vi.fn()
}))
vi.mock('@/api/client', () => ({getApiClient: () => api, updateApiToken: vi.fn()}))
vi.mock('vue-router', () => ({
  useRoute: () => ({params: {groupId: 'group'}, hash: ''}),
  useRouter: () => ({push: vi.fn()})
}))

const child: UserResponse = {
  id: 'child', parentId: 'parent', email: null, pseudo: 'Camille', avatarId: 'avatar-1',
  createdAt: '2026-10-19T10:00:00Z'
}
const otherChild: UserResponse = {...child, id: 'other-child', parentId: 'other-parent', pseudo: 'Alex'}
const parent: UserResponse = {...child, id: 'parent', parentId: null, email: 'parent@example.com', pseudo: 'Parent'}
const wish: WishResponse = {
  id: 'wish', userId: 'child', groupId: 'group', giftName: 'Livre', createdAt: child.createdAt,
  collective: false
}
const stubs = {
  VDialog: true, VCard: true, VCardText: true, VCardTitle: true, VCardActions: true,
  VForm: {template: '<form><slot /></form>', methods: {validate: async () => ({valid: true}), resetValidation() {}}},
  VTextField: true, VTextarea: true, VDivider: true, VIcon: true, VSpacer: true,
  VImg: true, VAlert: true, VRow: true, VCol: true, VContainer: true, VSelect: true,
  VExpandTransition: true, VProgressCircular: true, VChip: true, VTooltip: true,
  UserAvatar: true, AvatarSelector: true,
  VBtn: {props: ['disabled', 'ariaLabel'], template: '<button :disabled="disabled" :aria-label="ariaLabel"><slot /></button>'}
}
const global = {renderStubDefaultSlot: true, stubs}

function dialog() {
  return shallowMount(ChildrenDialog, {props: {modelValue: true, groupId: 'group'}, global})
}

async function submit(wrapper: ReturnType<typeof dialog>, pseudo: string) {
  wrapper.getComponent({name: 'VTextField'}).vm.$emit('update:modelValue', pseudo)
  await wrapper.get('form').trigger('submit')
  await flushPromises()
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.stubGlobal('localStorage', {removeItem: vi.fn(), setItem: vi.fn(), getItem: vi.fn()})
  setActivePinia(createPinia())
  useAuthStore().user = {id: 'parent', username: 'parent', email: 'parent@example.com', roles: ['USER']}
  const store = useChildrenStore()
  store.currentGroupId = 'group'
  store.children = [child]
  api.getChildren.mockResolvedValue({data: [child]})
  api.createChild.mockResolvedValue({data: {...child, id: 'new-child'}})
  api.updateChild.mockResolvedValue({data: {...child, pseudo: 'Nouveau pseudo'}})
  api.deleteChild.mockResolvedValue({})
})

afterEach(() => vi.unstubAllGlobals())

describe('child profiles', () => {
  it('handles nullable emails in display names', () => {
    const auth = useAuthStore()
    expect(auth.getMemberDisplayName(child)).toBe('Camille')
    expect(auth.getMemberDisplayName({...child, pseudo: null})).toBe('Membre')
    expect(auth.getMemberDisplayName({...child, email: undefined})).toBe('Camille')
    expect(auth.getMemberDisplayName({...child, pseudo: null, email: undefined})).toBe('Membre')
    expect(auth.getMemberDisplayName({...child, pseudo: null, email: 'name@example.com'})).toBe('name')
  })

  it('scopes children to the active group and clears stale data on group changes', async () => {
    const store = useChildrenStore()
    await store.fetchChildren('group')
    expect(api.getChildren).toHaveBeenCalledWith('group')
    api.getChildren.mockRejectedValueOnce(new Error('Forbidden'))
    await expect(store.fetchChildren('other-group')).rejects.toThrow('Forbidden')
    expect(store.children).toEqual([])
    expect(store.isLoading).toBe(false)
  })

  it('creates more children without changing authentication, validates and trims the pseudo', async () => {
    const wrapper = dialog()
    const originalUser = {...useAuthStore().user}
    await submit(wrapper, '   ')
    await submit(wrapper, 'a'.repeat(101))
    expect(api.createChild).not.toHaveBeenCalled()
    await submit(wrapper, '  Jules  ')
    expect(api.createChild).toHaveBeenCalledWith('group', {pseudo: 'Jules', avatarId: null})
    expect(useChildrenStore().children).toHaveLength(2)
    expect(useAuthStore().user).toEqual(originalUser)
    expect(wrapper.emitted('changed')).toHaveLength(1)
  })

  it('edits the child profile with its avatar and supports clearing it', async () => {
    const wrapper = dialog()
    await wrapper.get('[aria-label="Modifier Camille"]').trigger('click')
    expect(wrapper.getComponent(AvatarSelector).props('modelValue')).toBe('avatar-1')
    wrapper.getComponent(AvatarSelector).vm.$emit('update:modelValue', null)
    await submit(wrapper, 'Nouveau pseudo')
    expect(api.updateChild).toHaveBeenCalledWith('group', 'child', {pseudo: 'Nouveau pseudo', avatarId: null})
    expect(api.createChild).not.toHaveBeenCalled()
    expect(wrapper.emitted('changed')).toHaveLength(1)
  })

  it('requires explicit global deletion confirmation and resets a deleted editor', async () => {
    const confirm = vi.spyOn(useConfirmStore(), 'confirm').mockResolvedValueOnce(false).mockResolvedValueOnce(true)
    const wrapper = dialog()
    await wrapper.get('[aria-label="Modifier Camille"]').trigger('click')
    await wrapper.get('[aria-label="Supprimer Camille"]').trigger('click')
    await flushPromises()
    expect(api.deleteChild).not.toHaveBeenCalled()
    expect(confirm).toHaveBeenCalledWith(expect.objectContaining({
      message: expect.stringMatching(/tous ses souhaits.*tous les groupes.*irréversible/)
    }))
    await wrapper.get('[aria-label="Supprimer Camille"]').trigger('click')
    await flushPromises()
    expect(api.deleteChild).toHaveBeenCalledWith('group', 'child')
    expect(useChildrenStore().children).toEqual([])
    expect(wrapper.text()).toContain('Ajouter un enfant')
    expect(wrapper.emitted('changed')).toHaveLength(1)
  })

  it('surfaces API failures, preserves the form and disables actions during saving', async () => {
    let rejectSave!: (reason: unknown) => void
    api.createChild.mockReturnValueOnce(new Promise((_, reject) => { rejectSave = reject }))
    const wrapper = dialog()
    await submit(wrapper, 'Jules')
    expect(wrapper.findAll('button').every(button => button.attributes('disabled') !== undefined)).toBe(true)
    rejectSave({response: {data: {message: 'Accès refusé'}}})
    await flushPromises()
    expect(useNotificationStore().notifications.at(-1)?.message).toBe('Accès refusé')
    expect(wrapper.getComponent({name: 'VTextField'}).attributes('modelvalue')).toBe('Jules')
    expect(wrapper.emitted('changed')).toBeUndefined()
  })

  it('clears child data when the parent logs out', () => {
    useAuthStore().logout()
    expect(useChildrenStore().children).toEqual([])
  })
})

describe('child wishes without impersonation', () => {
  it('sends a target only when creating a child wish and omits it for self', async () => {
    const add = vi.spyOn(useWishStore(), 'addWish').mockResolvedValue(wish)
    const wrapper = shallowMount(AddWishDialog, {
      props: {groupId: 'group', modelValue: true, targetUserId: 'child', targetName: 'Camille'}, global
    })
    const title = () => wrapper.findAllComponents({name: 'VTextField'}).find(field => field.attributes('label') === 'Titre *')!
    title().vm.$emit('update:modelValue', 'Livre')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(add).toHaveBeenLastCalledWith('group', expect.objectContaining({userId: 'child'}))
    await wrapper.setProps({targetUserId: undefined})
    title().vm.$emit('update:modelValue', 'Mon livre')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(add.mock.calls[1]![1]).not.toHaveProperty('userId')
  })

  it('allows managing child wishes but keeps parent reservation identity and never labels the child Moi', async () => {
    const reserve = vi.spyOn(useWishStore(), 'reserveWish').mockResolvedValue(wish)
    const remove = vi.spyOn(useWishStore(), 'deleteWish').mockResolvedValue(true)
    vi.spyOn(useConfirmStore(), 'confirmDelete').mockResolvedValue(true)
    const wrapper = shallowMount(MemberCard, {
      props: {member: child, wishes: [wish], groupId: 'group', isCurrentUser: false, isManagedChild: true, allMembers: [parent, child]},
      global
    })
    expect(wrapper.text()).toContain('Camille')
    expect(wrapper.text()).not.toContain('Moi')
    await wrapper.findAll('button').find(button => button.text().includes('Ajouter un souhait'))!.trigger('click')
    expect(wrapper.emitted('addWish')).toEqual([['child']])
    await wrapper.findAll('button').find(button => button.text() === 'Réserver')!.trigger('click')
    await flushPromises()
    expect(reserve).toHaveBeenCalledWith('group', 'wish')
    expect(useAuthStore().user?.id).toBe('parent')
    await wrapper.get('[aria-label="Modifier le souhait"]').trigger('click')
    expect(wrapper.getComponent({name: 'EditWishDialog'}).props('modelValue')).toBe(true)
    await wrapper.get('[aria-label="Supprimer le souhait"]').trigger('click')
    await flushPromises()
    expect(remove).toHaveBeenCalledWith('group', 'wish')
  })

  it('places each parent before their indented children without adding a relationship label', async () => {
    const otherParent: UserResponse = {...parent, id: 'other-parent', pseudo: 'Autre parent'}
    const sibling: UserResponse = {...child, id: 'sibling', pseudo: 'Deuxieme enfant'}
    const groups = useGroupStore()
    groups.groups = [{id: 'group', name: 'Famille', type: 'noël', adminId: 'parent', createdAt: child.createdAt}]
    vi.spyOn(groups, 'fetchMyGroups').mockResolvedValue(groups.groups)
    vi.spyOn(groups, 'fetchGroupMembers').mockResolvedValue([otherChild, otherParent, child, parent, sibling])
    vi.spyOn(useWishStore(), 'fetchGroupWishes').mockResolvedValue([])
    const wrapper = shallowMount(GroupMembersView, {global})
    await flushPromises()
    const cards = wrapper.findAllComponents(MemberCard)
    expect(cards.map(card => card.props('member').id)).toEqual([
      'parent', 'child', 'sibling', 'other-parent', 'other-child'
    ])
    expect(cards.filter(card => card.classes().includes('child-member')).map(card => card.props('member').id))
      .toEqual(['child', 'sibling', 'other-child'])
    expect(wrapper.text()).not.toContain('Enfant de')
  })

  it.each([['USER'], ['ADMIN']])('adds wishes from member cards for %s users, refreshing after deletion', async role => {
    useAuthStore().user!.roles = [role]
    const groups = useGroupStore()
    groups.groups = [{id: 'group', name: 'Famille', type: 'noël', adminId: 'parent', createdAt: child.createdAt}]
    vi.spyOn(groups, 'fetchGroups').mockResolvedValue(groups.groups)
    vi.spyOn(groups, 'fetchMyGroups').mockResolvedValue(groups.groups)
    const fetchMembers = vi.spyOn(groups, 'fetchGroupMembers').mockResolvedValue([parent, child, otherChild])
    const fetchWishes = vi.spyOn(useWishStore(), 'fetchGroupWishes').mockResolvedValue([wish])
    const wrapper = shallowMount(GroupMembersView, {global})
    await flushPromises()
    expect(wrapper.text()).toContain('Gérer mes enfants')
    expect(wrapper.findComponent({name: 'VSelect'}).exists()).toBe(false)
    expect(wrapper.findAll('button').some(button => button.text() === 'Ajouter un souhait')).toBe(false)
    const cards = wrapper.findAllComponents(MemberCard)
    expect(cards.find(card => card.props('member').id === 'child')!.props('isManagedChild')).toBe(true)
    expect(cards.find(card => card.props('member').id === 'other-child')!.props('isManagedChild')).toBe(false)
    cards.find(card => card.props('member').id === 'parent')!.vm.$emit('addWish', 'parent')
    await flushPromises()
    expect(wrapper.getComponent(AddWishDialog).props('targetUserId')).toBeUndefined()
    expect(wrapper.getComponent(AddWishDialog).props('modelValue')).toBe(true)
    wrapper.getComponent(AddWishDialog).vm.$emit('update:modelValue', false)
    cards.find(card => card.props('member').id === 'child')!.vm.$emit('addWish', 'child')
    await flushPromises()
    expect(wrapper.getComponent(AddWishDialog).props('targetUserId')).toBe('child')
    const store = useChildrenStore()
    api.getChildren.mockResolvedValueOnce({data: []})
    await store.deleteChild('group', 'child')
    wrapper.getComponent(ChildrenDialog).vm.$emit('changed')
    await flushPromises()
    expect(wrapper.getComponent(AddWishDialog).props('targetUserId')).toBeUndefined()
    expect(wrapper.getComponent(AddWishDialog).props('modelValue')).toBe(false)
    expect(fetchMembers).toHaveBeenCalledTimes(2)
    expect(fetchWishes).toHaveBeenCalledTimes(2)
    expect(api.getChildren).toHaveBeenCalledTimes(2)
  })
})
