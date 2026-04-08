class WebSocketService {
    constructor() {
        this.socket = null
        this.messageHandlers = new Set()
        this.isManuallyClosed = false
        this.connectPromise = null
    }

    async connect() {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) return
        if (this.connectPromise) return this.connectPromise

        this.connectPromise = new Promise((resolve, reject) => {
            const wsUrl = `ws://localhost:8080/engine-websocket`
            this.socket = new WebSocket(wsUrl)
            this.isManuallyClosed = false

            this.socket.onopen = () => {
                console.log('✅ WebSocket连接成功')
                this.connectPromise = null
                resolve()
            }
            this.socket.onmessage = (e) => {
                try {
                    const data = JSON.parse(e.data)
                    this.messageHandlers.forEach(handler => handler(data))
                } catch (err) { console.error(err) }
            }
            this.socket.onclose = () => {
                console.log('🔌 WebSocket连接关闭')
                this.connectPromise = null
                if (!this.isManuallyClosed) {
                    // 自动重连（可选）
                    setTimeout(() => this.connect(), 3000)
                }
            }
            this.socket.onerror = (err) => {
                this.connectPromise = null
                reject(err)
            }
        })
        return this.connectPromise
    }

    send(message) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(message)
        } else {
            console.error('WebSocket未连接，无法发送消息')
            throw new Error('WebSocket未连接')
        }
    }

    addMessageHandler(handler) { this.messageHandlers.add(handler) }
    removeMessageHandler(handler) { this.messageHandlers.delete(handler) }
    disconnect() {
        this.isManuallyClosed = true
        if (this.socket) this.socket.close()
        this.messageHandlers.clear()
    }
}
export default new WebSocketService()