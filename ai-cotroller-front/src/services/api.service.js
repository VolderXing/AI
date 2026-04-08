import axios from 'axios'

const API_BASE_URL = '/api'

class ApiService {
    constructor() {
        this.client = axios.create({
            baseURL: API_BASE_URL,
            timeout: 10000,
            headers: {
                'Content-Type': 'application/json'
            }
        })
    }

    async getStatus() {
        try {
            const response = await this.client.get('/status')
            return response.data
        } catch (error) {
            console.error('获取状态失败:', error)
            throw error
        }
    }

    async testCommand(command) {
        try {
            const response = await this.client.post('/test/command', { command })
            return response.data
        } catch (error) {
            console.error('测试指令失败:', error)
            throw error
        }
    }

    async getAppInfo() {
        try {
            const response = await this.client.get('/info')
            return response.data
        } catch (error) {
            console.error('获取应用信息失败:', error)
            throw error
        }
    }

    async getDemo() {
        try {
            const response = await this.client.get('/test/demo')
            return response.data
        } catch (error) {
            console.error('获取演示数据失败:', error)
            throw error
        }
    }
}

export default new ApiService()