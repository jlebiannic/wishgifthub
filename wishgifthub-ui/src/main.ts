import {createApp} from 'vue'
import {createPinia} from 'pinia'
import vuetify from './plugins/vuetify'

import App from './App.vue'
import router from './router'

// Import CSS custom APRÈS Vuetify pour permettre les overrides
import './assets/main.css'

// Afficher les informations de version dans la console
async function displayVersionInfo() {
  try {
    const response = await fetch('/version.json')
    if (response.ok) {
      const versionInfo = await response.json()
      console.log('%c🎁 WishGiftHub', 'font-size: 20px; font-weight: bold; color: #1976d2;')
      console.log(`%cVersion: ${versionInfo.version}`, 'font-size: 14px; color: #666;')
      console.log(`%cDéployé le: ${versionInfo.buildDate}`, 'font-size: 14px; color: #666;')
      console.log(`%cTimestamp: ${versionInfo.buildTimestamp}`, 'font-size: 12px; color: #999;')
    }
  } catch (error) {
    console.warn('Impossible de charger les informations de version')
  }
}

displayVersionInfo()

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(vuetify)

app.mount('#app')
