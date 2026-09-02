<template>
  <n-config-provider :theme="null">
    <n-message-provider>
      <router-view />
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
</script>

<style>
#app {
  min-height: 100vh;
}
</style>