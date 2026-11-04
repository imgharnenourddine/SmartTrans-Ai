import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { assistantService } from '../../services/assistantService'
import type { ChatMessage } from '../../types/assistant'
import { citationKindLabel, citationPath } from '../../types/assistant'
import { getApiErrorMessage } from '../../utils/apiError'

const EXAMPLES = [
  'Combien de missions et d’alertes d’expiration ?',
  'Quel chauffeur et véhicule puis-je affecter ?',
  'Quels risques d’expiration dans les 30 jours ?',
]

export function AssistantPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [draft, setDraft] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function send(text: string) {
    const message = text.trim()
    if (message.length < 2 || sending) {
      return
    }
    setError(null)
    setSending(true)
    setDraft('')
    const userMessage: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: message,
    }
    setMessages((current) => [...current, userMessage])
    try {
      const response = await assistantService.ask(message)
      setMessages((current) => [
        ...current,
        {
          id: crypto.randomUUID(),
          role: 'assistant',
          content: response.answer,
          mode: response.mode,
          citations: response.citations,
        },
      ])
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible d’interroger l’assistant'))
    } finally {
      setSending(false)
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void send(draft)
  }

  return (
    <section className="assistant-page">
      <header className="page-header">
        <h2>Assistant</h2>
        <p>
          Posez une question ou demandez une suggestion (chauffeur, véhicule, alertes).
        </p>
      </header>

      {messages.length === 0 && (
        <div className="assistant-examples">
          {EXAMPLES.map((example) => (
            <button
              key={example}
              type="button"
              className="btn btn-secondary btn-inline"
              onClick={() => void send(example)}
              disabled={sending}
            >
              {example}
            </button>
          ))}
        </div>
      )}

      <div className="chat-log" aria-live="polite">
        {messages.map((message) => (
          <article key={message.id} className={`chat-bubble chat-${message.role}`}>
            <p className="chat-role">{message.role === 'user' ? 'Vous' : 'Assistant'}</p>
            <p className="chat-content">{message.content}</p>
            {message.mode && (
              <p className="chat-mode">{message.mode === 'LLM' ? 'Réponse LLM' : 'Réponse à partir des données'}</p>
            )}
            {message.citations && message.citations.length > 0 && (
              <ul className="citation-list">
                {message.citations.map((citation) => (
                  <li key={`${citation.kind}-${citation.id}`}>
                    <Link to={citationPath(citation)}>
                      {citationKindLabel(citation.kind)} · {citation.title}
                    </Link>
                    {citation.snippet && <span>{citation.snippet}</span>}
                  </li>
                ))}
              </ul>
            )}
          </article>
        ))}
        {sending && <p className="page-loading">L’assistant cherche dans les données…</p>}
      </div>

      {error && <p className="form-error">{error}</p>}

      <form className="chat-composer" onSubmit={handleSubmit}>
        <label className="field">
          <span className="sr-only">Votre question</span>
          <textarea
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="Ex. Quel chauffeur puis-je affecter ?"
            rows={3}
            disabled={sending}
          />
        </label>
        <button type="submit" className="btn btn-primary btn-inline" disabled={sending || draft.trim().length < 2}>
          Envoyer
        </button>
      </form>
    </section>
  )
}
