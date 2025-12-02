import {ref} from 'vue'
import {defineStore} from 'pinia'

export interface ConfirmOptions {
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  confirmColor?: string
  icon?: string
  iconColor?: string
}

/**
 * Store gérant les dialogues de confirmation
 */
export const useConfirmStore = defineStore('confirm', () => {
  const isOpen = ref(false)
  const options = ref<ConfirmOptions>({
    title: 'Confirmation',
    message: '',
    confirmText: 'Confirmer',
    cancelText: 'Annuler',
    confirmColor: 'primary',
    icon: 'mdi-help-circle',
    iconColor: 'primary'
  })

  let resolveCallback: ((value: boolean) => void) | null = null

  /**
   * Affiche un dialogue de confirmation
   * @returns Promise qui résout à true si confirmé, false si annulé
   */
  function confirm(opts: ConfirmOptions): Promise<boolean> {
    options.value = {
      title: opts.title || 'Confirmation',
      message: opts.message,
      confirmText: opts.confirmText || 'Confirmer',
      cancelText: opts.cancelText || 'Annuler',
      confirmColor: opts.confirmColor || 'primary',
      icon: opts.icon || 'mdi-help-circle',
      iconColor: opts.iconColor || 'primary'
    }

    isOpen.value = true

    return new Promise<boolean>((resolve) => {
      resolveCallback = resolve
    })
  }

  /**
   * Dialogue de confirmation pour une suppression
   */
  function confirmDelete(itemName: string): Promise<boolean> {
    return confirm({
      title: 'Confirmer la suppression',
      message: `Êtes-vous sûr de vouloir supprimer ${itemName} ? Cette action est irréversible.`,
      confirmText: 'Supprimer',
      cancelText: 'Annuler',
      confirmColor: 'error',
      icon: 'mdi-delete-alert',
      iconColor: 'error'
    })
  }

  /**
   * Dialogue de confirmation pour une annulation
   */
  function confirmCancel(message?: string): Promise<boolean> {
    return confirm({
      title: 'Confirmer l\'annulation',
      message: message || 'Êtes-vous sûr de vouloir annuler ? Les modifications non enregistrées seront perdues.',
      confirmText: 'Oui, annuler',
      cancelText: 'Non, continuer',
      confirmColor: 'warning',
      icon: 'mdi-alert',
      iconColor: 'warning'
    })
  }

  /**
   * Gère la confirmation
   */
  function handleConfirm() {
    if (resolveCallback) {
      resolveCallback(true)
      resolveCallback = null
    }
    isOpen.value = false
  }

  /**
   * Gère l'annulation
   */
  function handleCancel() {
    if (resolveCallback) {
      resolveCallback(false)
      resolveCallback = null
    }
    isOpen.value = false
  }

  return {
    isOpen,
    options,
    confirm,
    confirmDelete,
    confirmCancel,
    handleConfirm,
    handleCancel
  }
})

