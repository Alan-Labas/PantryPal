import type { LlmPromptPredogled, LlmRezultat } from '../domain/llm.ts'
import type { LlmObrazecRequest } from '../dto/pantryDto.ts'
import { api } from './api.ts'

export const getPromptPredogled = async (data: LlmObrazecRequest): Promise<LlmPromptPredogled> => {
  const response = await api.post<LlmPromptPredogled>('/llm/prompt', data)
  return response.data
}

export const pridobiAiPredloge = async (data: LlmObrazecRequest): Promise<LlmRezultat> => {
  const response = await api.post<LlmRezultat>('/llm/predlogi', data)
  return response.data
}
