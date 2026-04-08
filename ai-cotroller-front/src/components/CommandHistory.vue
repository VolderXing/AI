<template>
  <div class="command-history">
    <div class="history-header">
      <h3>指令历史</h3>
      <button @click="clearHistory" class="clear-btn" :disabled="history.length === 0">
        🗑️ 清空
      </button>
    </div>

    <div class="history-list">
      <div
          v-for="(item, index) in history"
          :key="index"
          class="history-item"
          :class="{ 'current': index === 0 }"
      >
        <div class="item-main">
          <div class="command-text">"{{ item.command }}"</div>
          <div class="rpm-value">{{ item.rpm }} RPM</div>
        </div>

        <div class="item-details">
          <div class="confidence">
            置信度: {{ (item.confidence * 100).toFixed(1) }}%
          </div>
          <div class="timestamp">
            {{ formatTime(item.timestamp) }}
          </div>
        </div>

        <div class="item-status" :class="getStatusClass(item)">
          {{ getStatusText(item) }}
        </div>
      </div>

      <div v-if="history.length === 0" class="empty-state">
        <div class="empty-icon">📝</div>
        <div class="empty-text">暂无指令历史</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CommandHistory',
  props: {
    history: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    clearHistory() {
      this.$emit('clear-history')
    },
    formatTime(timestamp) {
      const date = new Date(timestamp)
      return date.toLocaleTimeString('zh-CN', {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    },
    getStatusClass(item) {
      if (!item.isSafe) return 'unsafe'
      if (item.confidence < 0.7) return 'low-confidence'
      return 'normal'
    },
    getStatusText(item) {
      if (!item.isSafe) return '不安全'
      if (item.confidence < 0.7) return '低置信度'
      return '正常'
    }
  }
}
</script>

<style>
.command-history {
  background: white;
  border-radius: 15px;
  padding: 25px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  height: 100%;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.history-header h3 {
  color: #2c3e50;
  margin: 0;
}

.clear-btn {
  background: #e74c3c;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s ease;
}

.clear-btn:hover:not(:disabled) {
  background: #c0392b;
  transform: translateY(-1px);
}

.clear-btn:disabled {
  background: #bdc3c7;
  cursor: not-allowed;
}

.history-list {
  max-height: 400px;
  overflow-y: auto;
}

.history-item {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 15px;
  margin-bottom: 12px;
  border-left: 4px solid #3498db;
  transition: all 0.3s ease;
}

.history-item.current {
  background: #e3f2fd;
  border-left-color: #2196F3;
  transform: scale(1.02);
}

.history-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.item-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.command-text {
  font-weight: bold;
  color: #2c3e50;
  font-size: 1.1rem;
}
</style>