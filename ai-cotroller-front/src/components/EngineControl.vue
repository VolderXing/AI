<template>
  <div class="engine-control">
    <!-- 顶部标题栏 -->
    <header class="app-header">
      <div class="header-content">
        <h1 class="app-title">
          <span class="title-icon">🚀</span>
          智能电机转速控制系统 - DeepSeek
        </h1>
        <div class="header-status">
          <div class="status-item">
            <span class="status-label">AI服务:</span>
            <span class="status-value" :class="aiStatusClass">{{ aiServiceName }}</span>
          </div>
          <div class="status-item">
            <span class="status-label">连接状态:</span>
            <span class="status-value" :class="connectionClass">{{ connectionText }}</span>
          </div>
        </div>
      </div>
    </header>

    <!-- 主要内容区域 -->
    <main class="main-content">
      <div class="content-grid">
        <!-- 左侧：转速仪表盘 -->
        <div class="grid-left">
          <RPMGauge
              :currentRPM="currentRPM"
              :maxRPM="8000"
              class="rpm-gauge-card"
          />

          <!-- AI分析结果 -->
          <div class="ai-analysis card">
            <h3 class="card-title">🤖 AI分析结果</h3>
            <div v-if="lastAction" class="analysis-content">
              <div class="analysis-item">
                <span class="item-label">执行动作:</span>
                <span class="item-value">{{ lastAction }}</span>
              </div>
              <div class="analysis-item">
                <span class="item-label">决策原因:</span>
                <span class="item-value">{{ lastReason }}</span>
              </div>
              <div class="analysis-item">
                <span class="item-label">置信度:</span>
                <span class="item-value">
                  {{ (lastConfidence * 100).toFixed(1) }}%
                  <div class="confidence-bar">
                    <div
                        class="confidence-fill"
                        :style="{ width: (lastConfidence * 100) + '%' }"
                    ></div>
                  </div>
                </span>
              </div>
              <div v-if="aiResponse" class="ai-response">
                <span class="response-label">AI响应:</span>
                <span class="response-text">{{ aiResponse }}</span>
              </div>
            </div>
            <div v-else class="analysis-placeholder">
              <div class="placeholder-icon">💡</div>
              <div class="placeholder-text">等待语音指令...</div>
            </div>
          </div>
        </div>

        <!-- 右侧：控制面板 -->
        <div class="grid-right">
          <!-- 语音控制 -->
          <VoiceRecorder
              @command-sent="handleCommandSent"
              class="voice-recorder-card"
          />

          <!-- 指令历史 -->
          <CommandHistory
              :history="commandHistory"
              @clear-history="clearHistory"
              class="history-card"
          />
        </div>
      </div>

      <!-- 系统信息栏 -->
      <footer class="app-footer">
        <div class="footer-content">
          <div class="system-info">
            <span class="info-item">最后更新: {{ lastUpdate }}</span>
            <span class="info-item">指令总数: {{ commandHistory.length }}</span>
            <span class="info-item">平均置信度: {{ averageConfidence }}%</span>
          </div>
          <div class="footer-actions">
            <button @click="testConnection" class="footer-btn">
              🔄 测试连接
            </button>
            <button @click="showHelp" class="footer-btn">
              ❓ 使用帮助
            </button>
          </div>
        </div>
      </footer>
    </main>

    <!-- 连接状态提示 -->
    <div v-if="showConnectionAlert" class="connection-alert" :class="connectionAlertClass">
      <div class="alert-content">
        <span class="alert-icon">{{ connectionAlertIcon }}</span>
        <span class="alert-text">{{ connectionAlertText }}</span>
        <button @click="showConnectionAlert = false" class="alert-close">×</button>
      </div>
    </div>
  </div>
</template>

<script>
import RPMGauge from './RPMGauge.vue'
import VoiceRecorder from './VoiceRecorder.vue'
import CommandHistory from './CommandHistory.vue'
import WebSocketService from '../services/websocket.service.js'
import ApiService from '../services/api.service.js'

export default {
  name: 'EngineControl',
  components: {
    RPMGauge,
    VoiceRecorder,
    CommandHistory
  },
  data() {
    return {
      currentRPM: 0,
      lastAction: '',
      lastReason: '',
      lastConfidence: 0,
      aiResponse: '',
      commandHistory: [],
      aiServiceName: '未知',
      connectionState: 'DISCONNECTED',
      lastUpdate: '--',
      showConnectionAlert: false,
      connectionAlertText: '',
      connectionAlertClass: '',
      connectionAlertIcon: ''
    }
  },
  computed: {
    aiStatusClass() {
      return {
        'active': this.aiServiceName.includes('DeepSeek'),
        'fallback': this.aiServiceName.includes('Fallback')
      }
    },
    connectionClass() {
      return {
        'connected': this.connectionState === 'CONNECTED',
        'connecting': this.connectionState === 'CONNECTING',
        'disconnected': this.connectionState === 'DISCONNECTED'
      }
    },
    connectionText() {
      const stateMap = {
        'CONNECTED': '已连接',
        'CONNECTING': '连接中',
        'DISCONNECTED': '未连接'
      }
      return stateMap[this.connectionState] || this.connectionState
    },
    averageConfidence() {
      if (this.commandHistory.length === 0) return 0
      const total = this.commandHistory.reduce((sum, item) => sum + item.confidence, 0)
      return ((total / this.commandHistory.length) * 100).toFixed(1)
    }
  },
  mounted() {
    this.initWebSocket()
    this.loadSystemInfo()
  },
  beforeUnmount() {
    WebSocketService.removeMessageHandler(this.handleWebSocketMessage)
    WebSocketService.disconnect()
  },
  methods: {
    async initWebSocket() {
      try {
        this.connectionState = 'CONNECTING'
        await WebSocketService.connect()
        this.connectionState = 'CONNECTED'
        this.showAlert('success', '✅ WebSocket连接成功')

        // 设置消息处理器
        WebSocketService.addMessageHandler(this.handleWebSocketMessage)

      } catch (error) {
        this.connectionState = 'DISCONNECTED'
        this.showAlert('error', `❌ WebSocket连接失败: ${error.message}`)
        console.error('WebSocket连接失败:', error)
      }
    },

    handleWebSocketMessage(data) {
      console.log('收到WebSocket消息:', data)

      switch (data.type) {
        case 'success':
          this.handleSuccessMessage(data.data)
          break
        case 'safety_warning':
          this.handleSafetyWarning(data.data)
          break
        case 'error':
          this.handleErrorMessage(data.data)
          break
        case 'processing':
          this.handleProcessingMessage(data.data)
          break
        case 'system':
          console.log('系统消息:', data.data.message)
          break
      }

      this.lastUpdate = new Date().toLocaleTimeString()
    },

    handleSuccessMessage(data) {
      this.currentRPM = data.rpm
      this.lastAction = data.action
      this.lastReason = data.reason
      this.lastConfidence = data.confidence
      this.aiResponse = data.aiResponse
      this.aiServiceName = data.provider || 'DeepSeek'

      // 添加到历史记录
      this.commandHistory.unshift({
        command: data.command,
        rpm: data.rpm,
        action: data.action,
        reason: data.reason,
        confidence: data.confidence,
        isSafe: true,
        timestamp: data.timestamp || Date.now()
      })

      // 限制历史记录数量
      if (this.commandHistory.length > 50) {
        this.commandHistory = this.commandHistory.slice(0, 50)
      }

      this.showAlert('success', `✅ 指令执行成功: ${data.rpm} RPM`)
    },

    handleSafetyWarning(data) {
      this.aiResponse = data.message
      this.showAlert('warning', `⚠️ ${data.message}`)
    },

    handleErrorMessage(data) {
      this.aiResponse = data.message
      this.showAlert('error', `❌ ${data.message}`)
    },

    handleProcessingMessage(data) {
      this.aiResponse = '🤖 AI正在分析指令...'
    },

    handleCommandSent(command) {
      console.log('发送指令:', command)
      this.lastAction = ''
      this.lastReason = ''
      this.lastConfidence = 0
      this.aiResponse = '处理中...'
    },

    clearHistory() {
      this.commandHistory = []
      this.showAlert('info', '🗑️ 指令历史已清空')
    },

    async loadSystemInfo() {
      try {
        const info = await ApiService.getAppInfo()
        this.aiServiceName = info.ai?.service || 'DeepSeek'
      } catch (error) {
        console.error('加载系统信息失败:', error)
        this.aiServiceName = 'Fallback'
      }
    },

    async testConnection() {
      try {
        const status = await ApiService.getStatus()
        this.showAlert('success', `✅ 后端服务正常 (${status.service})`)
      } catch (error) {
        this.showAlert('error', '❌ 后端服务连接失败')
      }
    },

    showHelp() {
      const helpText = `
使用说明:

🎤 语音控制:
• 点击"开始语音控制"按钮
• 说出指令，如"设置发动机到中速"
• 系统会自动解析并执行

💡 常用指令:
• "启动发动机" - 800 RPM
• "停止发动机" - 0 RPM
• "设置怠速" - 800 RPM
• "设置中速" - 3000 RPM
• "设置高速" - 5000 RPM
• "最大功率" - 8000 RPM
• 或直接说数字如"2500"

🔧 其他功能:
• 点击指令示例快速发送
• 查看指令历史记录
• 实时显示AI分析结果
      `.trim()

      alert(helpText)
    },

    showAlert(type, message) {
      this.connectionAlertText = message
      this.connectionAlertClass = type

      switch (type) {
        case 'success':
          this.connectionAlertIcon = '✅'
          break
        case 'error':
          this.connectionAlertIcon = '❌'
          break
        case 'warning':
          this.connectionAlertIcon = '⚠️'
          break
        default:
          this.connectionAlertIcon = '💡'
      }

      this.showConnectionAlert = true

      // 3秒后自动隐藏
      setTimeout(() => {
        this.showConnectionAlert = false
      }, 3000)
    }
  }
}
</script>

<style scoped>
.engine-control {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.app-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 15px;
  padding: 20px 30px;
  margin-bottom: 20px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.app-title {
  color: #2c3e50;
  font-size: 2rem;
  font-weight: 700;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 15px;
}

.title-icon {
  font-size: 2.5rem;
}

.header-status {
  display: flex;
  gap: 30px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-label {
  color: #7f8c8d;
  font-weight: 500;
}

.status-value {
  padding: 6px 12px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 0.9rem;
}

.status-value.active {
  background: #d4edda;
  color: #155724;
}

.status-value.fallback {
  background: #fff3cd;
  color: #856404;
}

.status-value.connected {
  background: #d4edda;
  color: #155724;
}

.status-value.connecting {
  background: #fff3cd;
  color: #856404;
}

.status-value.disconnected {
  background: #f8d7da;
  color: #721c24;
}

.main-content {
  flex: 1;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.grid-left,
.grid-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.rpm-gauge-card,
.voice-recorder-card,
.history-card {
  height: fit-content;
}

.ai-analysis.card {
  background: white;
  border-radius: 15px;
  padding: 25px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.card-title {
  color: #2c3e50;
  margin-bottom: 20px;
  font-size: 1.3rem;
}

.analysis-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.analysis-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 15px;
}

.item-label {
  color: #7f8c8d;
  font-weight: 500;
  min-width: 80px;
}

.item-value {
  color: #2c3e50;
  font-weight: 500;
  flex: 1;
}

.confidence-bar {
  width: 100px;
  height: 6px;
  background: #ecf0f1;
  border-radius: 3px;
  margin-top: 5px;
  overflow: hidden;
}

.confidence-fill {
  height: 100%;
  background: linear-gradient(90deg, #2ecc71, #27ae60);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.ai-response {
  background: #e3f2fd;
  border-radius: 10px;
  padding: 15px;
  margin-top: 10px;
}

.response-label {
  color: #1976d2;
  font-weight: bold;
  display: block;
  margin-bottom: 5px;
}

.response-text {
  color: #2c3e50;
  font-style: italic;
}

.analysis-placeholder {
  text-align: center;
  padding: 40px 20px;
  color: #7f8c8d;
}

.placeholder-icon {
  font-size: 3rem;
  margin-bottom: 15px;
}

.placeholder-text {
  font-size: 1.1rem;
}

.app-footer {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 15px;
  padding: 15px 30px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.system-info {
  display: flex;
  gap: 30px;
}

.info-item {
  color: #7f8c8d;
  font-size: 0.9rem;
}

.footer-actions {
  display: flex;
  gap: 10px;
}

.footer-btn {
  background: #3498db;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s ease;
}

.footer-btn:hover {
  background: #2980b9;
  transform: translateY(-1px);
}

.connection-alert {
  position: fixed;
  top: 20px;
  right: 20px;
  background: white;
  border-radius: 10px;
  padding: 15px 20px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.connection-alert.success {
  border-left: 4px solid #2ecc71;
}

.connection-alert.error {
  border-left: 4px solid #e74c3c;
}

.connection-alert.warning {
  border-left: 4px solid #f39c12;
}

.connection-alert.info {
  border-left: 4px solid #3498db;
}

.alert-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.alert-icon {
  font-size: 1.2rem;
}

.alert-text {
  color: #2c3e50;
  font-weight: 500;
  flex: 1;
}

.alert-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #7f8c8d;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.alert-close:hover {
  color: #2c3e50;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .header-content {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }

  .header-status {
    justify-content: center;
  }

  .footer-content {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }

  .system-info {
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .engine-control {
    padding: 10px;
  }

  .app-title {
    font-size: 1.5rem;
  }

  .header-status {
    flex-direction: column;
    gap: 10px;
  }

  .system-info {
    flex-direction: column;
    gap: 10px;
  }

  .analysis-item {
    flex-direction: column;
    gap: 5px;
  }

  .item-label {
    min-width: auto;
  }
}
</style>