<template>
  <div id="app">
    <EngineControl />
  </div>
</template>

<script>
import EngineControl from './components/EngineControl.vue'

export default {
  name: 'App',
  components: {
    EngineControl
  }
}
</script>

<style>
#app {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
</style>