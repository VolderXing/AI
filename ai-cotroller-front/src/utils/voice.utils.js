class VoiceUtils {
    constructor() {
        this.recognition = null
        this.isSupported = 'webkitSpeechRecognition' in window || 'SpeechRecognition' in window
        this.isRecording = false
        this.onResultCallback = null
        this.onErrorCallback = null
    }

    init(language = 'zh-CN') {
        if (!this.isSupported) throw new Error('浏览器不支持语音识别')
        const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition
        this.recognition = new Recognition()
        this.recognition.continuous = false
        this.recognition.interimResults = false
        this.recognition.lang = language

        this.recognition.onstart = () => {
            this.isRecording = true
            console.log('🎤 开始录音（单次）')
        }
        this.recognition.onresult = (event) => {
            const transcript = event.results[0][0].transcript.trim()
            if (transcript && this.onResultCallback) {
                this.onResultCallback(transcript)
            }
        }
        this.recognition.onerror = (event) => {
            this.isRecording = false
            if (this.onErrorCallback) this.onErrorCallback(event.error)
        }
        this.recognition.onend = () => {
            this.isRecording = false
            console.log('🎤 录音结束（单次完成）')
        }
    }

    startRecording() {
        if (!this.recognition) this.init()
        if (this.isRecording) return
        try {
            this.recognition.start()
        } catch (error) {
            console.error('启动录音失败:', error)
            if (error.message.includes('already started')) {
                this.stopRecording()
                setTimeout(() => this.startRecording(), 200)
            }
        }
    }

    stopRecording() {
        if (this.recognition && this.isRecording) {
            try { this.recognition.stop() } catch(e) {}
        }
    }

    onResult(callback) { this.onResultCallback = callback }
    onError(callback) { this.onErrorCallback = callback }
    destroy() {
        if (this.recognition) {
            this.stopRecording()
            this.recognition = null
        }
    }
}
export default new VoiceUtils()