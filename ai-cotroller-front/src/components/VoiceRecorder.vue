<template>
  <div class="voice-recorder">
    <div class="recorder-header">
      <h3>语音控制</h3>
      <div class="connection-status" :class="{ connected: isConnected, disconnected: !isConnected }">
        {{ isConnected ? '已连接' : '未连接' }}
      </div>
    </div>

    <div class="recorder-controls">
      <button
          @click="toggleRecording"
          :class="['record-btn', { recording: isRecording }]"
          :disabled="!isSupported || !isConnected"
      >
        <span class="btn-icon">{{ isRecording ? '🛑' : '🎤' }}</span>
        <span class="btn-text">{{ isRecording ? '停止录音' : '开始语音控制' }}</span>
      </button>
      <div v-if="isRecording" class="recording-indicator">
        <div class="pulse-animation"></div>
        <span>正在聆听...请说话（说完自动结束）</span>
      </div>
    </div>

    <div v-if="!isSupported" class="browser-warning">❌ 浏览器不支持语音识别，请使用Chrome</div>
    <div v-if="!isConnected" class="connection-warning">❌ 未连接到服务器，语音控制不可用</div>

    <div class="voice-feedback">
      <div v-if="lastCommand" class="command-result"><strong>最新指令:</strong> "{{ lastCommand }}"</div>
      <div v-if="aiResponse" class="ai-response"><strong>AI响应:</strong> {{ aiResponse }}</div>
    </div>

    <div class="voice-suggestions">
      <h4>常用指令示例:</h4>
      <div class="suggestions-grid">
        <button v-for="s in suggestions" :key="s.command" @click="sendSuggestion(s.command)" class="suggestion-btn" :disabled="!isConnected">
          <span class="suggestion-icon">{{ s.icon }}</span>
          <span class="suggestion-text">{{ s.command }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import VoiceUtils from '../utils/voice.utils.js'
import WebSocketService from '../services/websocket.service.js'

export default {
  name: 'VoiceRecorder',
  data() {
    return {
      isRecording: false,
      isSupported: false,
      lastCommand: '',
      aiResponse: '',
      isConnected: false,
      suggestions: [
        { command: '启动发动机', icon: '🚀' }, { command: '停止发动机', icon: '🛑' },
        { command: '设置怠速', icon: '🐢' }, { command: '设置中速', icon: '🚗' },
        { command: '设置高速', icon: '🚀' }, { command: '最大功率', icon: '🔥' },
        { command: '经济模式', icon: '💰' }, { command: '运动模式', icon: '⚡' }
      ]
    }
  },
  mounted() {
    this.initVoiceRecognition()
    this.initWebSocket()
    WebSocketService.addMessageHandler(this.handleWebSocketMessage)
  },
  beforeUnmount() {
    VoiceUtils.destroy()
    WebSocketService.removeMessageHandler(this.handleWebSocketMessage)
    // 注意：不要主动断开 WebSocket，让页面关闭时自然断开
  },
  methods: {
    initVoiceRecognition() {
      this.isSupported = VoiceUtils.isSupported
      if (this.isSupported) {
        VoiceUtils.onResult(this.handleVoiceResult)
        VoiceUtils.onError(this.handleVoiceError)
      }
    },
    async initWebSocket() {
      try {
        await WebSocketService.connect()
        this.isConnected = true
        this.aiResponse = '✅ 连接成功，可以开始语音控制'
      } catch (err) {
        this.isConnected = false
        this.aiResponse = `❌ 连接失败: ${err.message}`
      }
    },
    handleWebSocketMessage(data) {
      switch (data.type) {
        case 'success': this.aiResponse = data.data.aiResponse; break
        case 'safety_warning': this.aiResponse = `⚠️ ${data.data.message}`; break
        case 'error': this.aiResponse = `❌ ${data.data.message}`; break
        case 'processing': this.aiResponse = '🤖 AI正在分析指令...'; break
      }
    },
    handleVoiceResult(transcript) {
      this.lastCommand = transcript
      this.sendVoiceCommand(transcript)
      // 识别完成后自动停止录音，按钮恢复“开始语音控制”
      this.isRecording = false  // 因为 onend 已经会重置，但这里主动重置也无妨
    },
    handleVoiceError(error) {
      let msg = '语音识别失败'
      if (error === 'not-allowed') msg = '请允许麦克风权限'
      else if (error === 'no-speech') msg = '没有检测到语音'
      else if (error === 'audio-capture') msg = '无法访问麦克风'
      this.aiResponse = `❌ ${msg}`
      this.isRecording = false
    },
    toggleRecording() {
      if (this.isRecording) {
        VoiceUtils.stopRecording()
        this.isRecording = false
      } else {
        this.startRecording()
      }
    },
    startRecording() {
      if (!this.isSupported) { this.aiResponse = '❌ 浏览器不支持语音识别'; return }
      if (!this.isConnected) { this.aiResponse = '❌ 未连接到服务器'; return }
      try {
        VoiceUtils.startRecording()
        this.isRecording = true
        this.aiResponse = '🎤 请开始说话...（单次识别）'
      } catch (err) {
        this.aiResponse = `❌ 启动录音失败: ${err.message}`
      }
    },
    sendVoiceCommand(command) {
      if (!this.isConnected) { this.aiResponse = '❌ 未连接到服务器'; return }
      try {
        WebSocketService.send(command)
        this.$emit('command-sent', command)
      } catch (err) {
        this.aiResponse = `❌ 发送失败: ${err.message}`
      }
    },
    sendSuggestion(command) {
      this.lastCommand = command
      this.sendVoiceCommand(command)
    }
  }
}
</script>

<style scoped>
/* 样式保持不变 */
/* 建议按钮网格容器 */
.suggestions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 20px;          /* 增大按钮之间的间距 */
  margin-top: 15px;
}

/* 每个建议按钮 */
.suggestion-btn {
  background: #e3f2fd;
  border: 2px solid #bbdefb;
  border-radius: 20px;   /* 加大圆角，更像圆角矩形 */
  padding: 16px 12px;    /* 增加上下内边距，按钮更高 */
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

/* 按钮悬停效果 */
.suggestion-btn:hover:not(:disabled) {
  background: #bbdefb;
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

/* 按钮禁用状态 */
.suggestion-btn:disabled {
  background: #f8f9fa;
  border-color: #dee2e6;
  cursor: not-allowed;
  opacity: 0.6;
}

/* 按钮内的图标 */
.suggestion-icon {
  font-size: 2rem;   /* 稍微增大图标 */
}

/* 按钮内的文字 */
.suggestion-text {
  font-size: 1rem;
  color: #1976d2;
  font-weight: 500;
}
</style>