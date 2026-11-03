import { useCallback, useState } from 'react'
import { isAxiosError } from 'axios'
import type { LlmPromptPredogled, LlmRezultat } from '../domain/llm.ts'
import type { LlmObrazecRequest } from '../dto/pantryDto.ts'
import * as llmService from '../services/llmService.ts'

export function useLlm() {
  const [predogled, setPredogled] = useState<LlmPromptPredogled | null>(null)
  const [rezultat, setRezultat] = useState<LlmRezultat | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const osveziPredogled = useCallback(async (request: LlmObrazecRequest) => {
    try {
      const data = await llmService.getPromptPredogled(request)
      setPredogled(data)
    } catch {
      // Predogled ne prekine obrazca
    }
  }, [])

  const generiraj = useCallback(async (request: LlmObrazecRequest) => {
    setLoading(true)
    setError(null)
    try {
      const data = await llmService.pridobiAiPredloge(request)
      setRezultat(data)
    } catch (err) {
      const msg =
        isAxiosError(err) && typeof err.response?.data?.napaka === 'string'
          ? err.response.data.napaka
          : 'Klic Google Gemini API ni uspel.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  return { predogled, rezultat, loading, error, osveziPredogled, generiraj }
}
