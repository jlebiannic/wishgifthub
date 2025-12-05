// Liste des avatars disponibles
export const AVATARS = [
  // Animaux mignons
  { id: 'avatar-1', name: '🦊 Renard', emoji: '🦊', color: '#FF6B35' },
  { id: 'avatar-2', name: '🐼 Panda', emoji: '🐼', color: '#2D3047' },
  { id: 'avatar-3', name: '🐨 Koala', emoji: '🐨', color: '#95B8D1' },
  { id: 'avatar-4', name: '🦁 Lion', emoji: '🦁', color: '#F4A261' },
  { id: 'avatar-5', name: '🐯 Tigre', emoji: '🐯', color: '#E76F51' },
  { id: 'avatar-6', name: '🐸 Grenouille', emoji: '🐸', color: '#52B788' },
  { id: 'avatar-7', name: '🐷 Cochon', emoji: '🐷', color: '#F4A7B9' },
  { id: 'avatar-8', name: '🐮 Vache', emoji: '🐮', color: '#8D99AE' },
  { id: 'avatar-9', name: '🐵 Singe', emoji: '🐵', color: '#D4A373' },
  { id: 'avatar-10', name: '🐱 Chat', emoji: '🐱', color: '#FFB4A2' },
  { id: 'avatar-11', name: '🐶 Chien', emoji: '🐶', color: '#B08968' },
  { id: 'avatar-12', name: '🦄 Licorne', emoji: '🦄', color: '#E0B0FF' },

  // Créatures fantastiques et nature
  { id: 'avatar-13', name: '🦉 Hibou', emoji: '🦉', color: '#8B7355' },
  { id: 'avatar-14', name: '🦋 Papillon', emoji: '🦋', color: '#A7C7E7' },
  { id: 'avatar-15', name: '🐝 Abeille', emoji: '🐝', color: '#FFD700' },
  { id: 'avatar-16', name: '🐙 Poulpe', emoji: '🐙', color: '#9B59B6' },
  { id: 'avatar-17', name: '🦜 Perroquet', emoji: '🦜', color: '#48C9B0' },
  { id: 'avatar-18', name: '🐧 Pingouin', emoji: '🐧', color: '#5DADE2' },

  // Objets et symboles
  { id: 'avatar-19', name: '⭐ Étoile', emoji: '⭐', color: '#F39C12' },
  { id: 'avatar-20', name: '🌙 Lune', emoji: '🌙', color: '#5499C7' },
  { id: 'avatar-21', name: '☀️ Soleil', emoji: '☀️', color: '#F1C40F' },
  { id: 'avatar-22', name: '🌈 Arc-en-ciel', emoji: '🌈', color: '#EC7063' },
  { id: 'avatar-23', name: '🎨 Palette', emoji: '🎨', color: '#AF7AC5' },
  { id: 'avatar-24', name: '🎭 Masques', emoji: '🎭', color: '#85929E' },
  { id: 'avatar-25', name: '🎸 Guitare', emoji: '🎸', color: '#E74C3C' },
  { id: 'avatar-26', name: '🎮 Jeux vidéo', emoji: '🎮', color: '#3498DB' },
  { id: 'avatar-27', name: '📚 Livres', emoji: '📚', color: '#8E44AD' },
  { id: 'avatar-28', name: '⚽ Football', emoji: '⚽', color: '#27AE60' },
  { id: 'avatar-29', name: '🏀 Basket', emoji: '🏀', color: '#E67E22' },
  { id: 'avatar-30', name: '🎯 Cible', emoji: '🎯', color: '#C0392B' },

  // Nourriture et boissons
  { id: 'avatar-31', name: '🍕 Pizza', emoji: '🍕', color: '#E67E22' },
  { id: 'avatar-32', name: '🍔 Burger', emoji: '🍔', color: '#D35400' },
  { id: 'avatar-33', name: '🍦 Glace', emoji: '🍦', color: '#AED6F1' },
  { id: 'avatar-34', name: '🍩 Donut', emoji: '🍩', color: '#F8B4D0' },
  { id: 'avatar-35', name: '🍰 Gâteau', emoji: '🍰', color: '#FADBD8' },
  { id: 'avatar-36', name: '☕ Café', emoji: '☕', color: '#A0785A' },

  // Nature
  { id: 'avatar-37', name: '🌸 Fleur rose', emoji: '🌸', color: '#FFB7C5' },
  { id: 'avatar-38', name: '🌺 Hibiscus', emoji: '🌺', color: '#FF6F91' },
  { id: 'avatar-39', name: '🌻 Tournesol', emoji: '🌻', color: '#F4D03F' },
  { id: 'avatar-40', name: '🌹 Rose', emoji: '🌹', color: '#EC7063' },
  { id: 'avatar-41', name: '🍀 Trèfle', emoji: '🍀', color: '#58D68D' },
  { id: 'avatar-42', name: '🌵 Cactus', emoji: '🌵', color: '#82E0AA' },

  // Émotions et symboles
  { id: 'avatar-43', name: '💎 Diamant', emoji: '💎', color: '#85C1E9' },
  { id: 'avatar-44', name: '👑 Couronne', emoji: '👑', color: '#F4D03F' },
  { id: 'avatar-45', name: '🎁 Cadeau', emoji: '🎁', color: '#E74C3C' },
  { id: 'avatar-46', name: '🎈 Ballon', emoji: '🎈', color: '#3498DB' },
  { id: 'avatar-47', name: '🎊 Confettis', emoji: '🎊', color: '#F39C12' },
  { id: 'avatar-48', name: '🔥 Flamme', emoji: '🔥', color: '#E74C3C' }
] as const

export type AvatarId = typeof AVATARS[number]['id']

export function getAvatarById(id: string | null | undefined) {
  if (!id) return null
  return AVATARS.find(avatar => avatar.id === id) || null
}

export function getDefaultAvatar() {
  return AVATARS[0]
}

