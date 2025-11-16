<script setup lang="ts">
import {ref} from 'vue'

const message = ref('Hello Wish !')
</script>

<template>
  <div class="hello-wish">
    <div class="gift-icon">🎁</div>
    <h1 class="title">{{ message }}</h1>
    <p class="subtitle">Bienvenue sur votre plateforme de gestion de cadeaux</p>
  </div>
</template>

<style scoped>
.hello-wish {
  padding: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  max-width: 600px;
  margin: 0 auto;
  color: white;
}

.gift-icon {
  font-size: 80px;
  margin-bottom: 20px;
  animation: bounce 2s infinite;
}

.title {
  font-size: 48px;
  font-weight: bold;
  margin: 20px 0;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.subtitle {
  font-size: 18px;
  margin-top: 10px;
  opacity: 0.9;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}
</style>

