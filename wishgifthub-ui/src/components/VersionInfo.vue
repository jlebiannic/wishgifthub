<script setup lang="ts">
import {onMounted, ref} from 'vue'

interface VersionInfo {
  buildTimestamp: string
  buildDate: string
  version: string
}

const versionInfo = ref<VersionInfo | null>(null)
const showVersion = ref(false)

onMounted(async () => {
  try {
    // Charger le fichier version.json depuis public/
    const response = await fetch('/version.json')
    if (response.ok) {
      versionInfo.value = await response.json()
    }
  } catch (error) {
    console.error('Erreur lors du chargement de la version:', error)
  }
})

function toggleVersion() {
  showVersion.value = !showVersion.value
}
</script>

<template>
  <div class="version-info">
    <!-- Bouton pour afficher/masquer les infos de version -->
    <v-btn
      icon
      size="small"
      variant="text"
      @click="toggleVersion"
      class="version-btn"
      title="Informations de version"
    >
      <v-icon>mdi-information-outline</v-icon>
    </v-btn>

    <!-- Dialog avec les informations de version -->
    <v-dialog v-model="showVersion" max-width="500">
      <v-card>
        <v-card-title class="text-h5 bg-primary text-white pa-4">
          <v-icon class="mr-2">mdi-information</v-icon>
          Informations de version
        </v-card-title>

        <v-card-text v-if="versionInfo" class="pa-6">
          <v-list>
            <v-list-item>
              <v-list-item-title class="text-subtitle-2 text-medium-emphasis">
                Version
              </v-list-item-title>
              <v-list-item-subtitle class="text-h6 mt-1">
                {{ versionInfo.version }}
              </v-list-item-subtitle>
            </v-list-item>

            <v-divider class="my-2" />

            <v-list-item>
              <v-list-item-title class="text-subtitle-2 text-medium-emphasis">
                Date de déploiement
              </v-list-item-title>
              <v-list-item-subtitle class="text-body-1 mt-1">
                {{ versionInfo.buildDate }}
              </v-list-item-subtitle>
            </v-list-item>

            <v-divider class="my-2" />

            <v-list-item>
              <v-list-item-title class="text-subtitle-2 text-medium-emphasis">
                Timestamp
              </v-list-item-title>
              <v-list-item-subtitle class="text-caption mt-1 font-monospace">
                {{ versionInfo.buildTimestamp }}
              </v-list-item-subtitle>
            </v-list-item>
          </v-list>
        </v-card-text>

        <v-card-text v-else class="pa-6">
          <v-alert type="info" variant="tonal">
            Informations de version non disponibles
          </v-alert>
        </v-card-text>

        <v-card-actions class="px-6 pb-4">
          <v-spacer />
          <v-btn color="primary" variant="flat" @click="showVersion = false">
            Fermer
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.version-info {
  position: fixed;
  bottom: 16px;
  right: 16px;
  z-index: 1000;
}

.version-btn {
  opacity: 0.6;
}

.version-btn:hover {
  opacity: 1;
}

.font-monospace {
  font-family: monospace;
}
</style>

