import axios from 'axios'
import { env } from '../config/env.ts'

export const api = axios.create({
  baseURL: `${env.apiBaseUrl}/api`,
})
