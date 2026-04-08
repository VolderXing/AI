<template>
  <div class="rpm-gauge">
    <div class="gauge-header">
      <h2>发动机转速</h2>
      <div class="rpm-value">{{ currentRPM }} <span class="rpm-unit">RPM</span></div>
    </div>

    <div class="gauge-container">
      <div class="gauge-background">
        <div
            class="gauge-fill"
            :style="gaugeStyle"
            :class="gaugeClass"
        ></div>
      </div>

      <div class="gauge-markings">
        <div class="marking" v-for="mark in markings" :key="mark.value">
          <span class="mark-value">{{ mark.value }}</span>
          <div class="mark-line" :class="{ 'major': mark.major }"></div>
        </div>
      </div>

      <div class="gauge-pointer" :style="pointerStyle"></div>
    </div>

    <div class="gauge-status">
      <div class="status-indicator" :class="statusClass">
        {{ statusText }}
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RPMGauge',
  props: {
    currentRPM: {
      type: Number,
      default: 0
    },
    maxRPM: {
      type: Number,
      default: 8000
    }
  },
  data() {
    return {
      markings: [
        { value: 0, major: true },
        { value: 1000, major: false },
        { value: 2000, major: true },
        { value: 3000, major: false },
        { value: 4000, major: true },
        { value: 5000, major: false },
        { value: 6000, major: true },
        { value: 7000, major: false },
        { value: 8000, major: true }
      ]
    }
  },
  computed: {
    percentage() {
      return (this.currentRPM / this.maxRPM) * 100
    },
    gaugeStyle() {
      return {
        width: `${this.percentage}%`
      }
    },
    pointerStyle() {
      return {
        left: `${this.percentage}%`
      }
    },
    gaugeClass() {
      if (this.percentage < 30) return 'safe'
      if (this.percentage < 60) return 'warning'
      if (this.percentage < 80) return 'caution'
      return 'danger'
    },
    statusClass() {
      if (this.currentRPM === 0) return 'stopped'
      if (this.currentRPM < 1000) return 'idle'
      if (this.currentRPM < 3000) return 'normal'
      if (this.currentRPM < 5000) return 'high'
      return 'max'
    },
    statusText() {
      const statusMap = {
        stopped: '已停止',
        idle: '怠速',
        normal: '正常',
        high: '高速',
        max: '极限'
      }
      return statusMap[this.statusClass] || '未知'
    }
  }
}
</script>

<style scoped>
.rpm-gauge {
  background: white;
  border-radius: 15px;
  padding: 25px;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.gauge-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.gauge-header h2 {
  color: #2c3e50;
  font-size: 1.5rem;
  margin: 0;
}

.rpm-value {
  font-size: 2.5rem;
  font-weight: bold;
  color: #3498db;
}

.rpm-unit {
  font-size: 1rem;
  color: #7f8c8d;
}

.gauge-container {
  position: relative;
  height: 120px;
  margin: 30px 0;
}

.gauge-background {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 20px;
  background: #ecf0f1;
  border-radius: 10px;
  overflow: hidden;
  transform: translateY(-50%);
}

.gauge-fill {
  height: 100%;
  border-radius: 10px;
  transition: all 0.5s ease;
}

.gauge-fill.safe {
  background: linear-gradient(90deg, #2ecc71, #27ae60);
}

.gauge-fill.warning {
  background: linear-gradient(90deg, #f39c12, #e67e22);
}

.gauge-fill.caution {
  background: linear-gradient(90deg, #e74c3c, #c0392b);
}

.gauge-fill.danger {
  background: linear-gradient(90deg, #c0392b, #a93226);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.7; }
  100% { opacity: 1; }
}

.gauge-markings {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  transform: translateY(-50%);
}

.marking {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.mark-value {
  font-size: 0.8rem;
  color: #7f8c8d;
  margin-bottom: 5px;
}

.mark-line {
  width: 2px;
  height: 10px;
  background: #bdc3c7;
}

.mark-line.major {
  height: 15px;
  background: #7f8c8d;
}

.gauge-pointer {
  position: absolute;
  top: 50%;
  width: 3px;
  height: 30px;
  background: #2c3e50;
  transform: translate(-50%, -50%);
  transition: left 0.5s ease;
  z-index: 10;
}

.gauge-pointer::after {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 8px solid #2c3e50;
  transform: translateX(-50%);
}

.gauge-status {
  text-align: center;
  margin-top: 20px;
}

.status-indicator {
  display: inline-block;
  padding: 8px 20px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 0.9rem;
}

.status-indicator.stopped {
  background: #95a5a6;
  color: white;
}

.status-indicator.idle {
  background: #3498db;
  color: white;
}

.status-indicator.normal {
  background: #2ecc71;
  color: white;
}

.status-indicator.high {
  background: #f39c12;
  color: white;
}

.status-indicator.max {
  background: #e74c3c;
  color: white;
  animation: pulse 1.5s infinite;
}
</style>