import {createApp} from 'vue'
import {createPinia} from 'pinia'
import vuetify from './plugins/vuetify'

import App from './App.vue'
import router from './router'

// Import CSS custom APRÈS Vuetify pour permettre les overrides
import './assets/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(vuetify)

app.mount('#app')
