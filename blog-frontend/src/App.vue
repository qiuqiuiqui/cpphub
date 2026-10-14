<template>
  <n-config-provider :theme="null">
    <n-message-provider>
      <n-dialog-provider>
        <router-view />
      </n-dialog-provider>
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