<script setup lang="ts">
import { ref, nextTick, watch, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createSession, listSessions, getMessages, sendMessage,
  type ChatSession, type ChatMessage, type ChatReference,
} from '@/modules/chat/api'
import { readSSEStream, type SSEStream } from '@/shared/utils/sse'
import EmptyState from '@/shared/components/EmptyState.vue'
import StatusPulse from '@/shared/components/StatusPulse.vue'
import MarkdownRenderer from '@/shared/components/MarkdownRenderer.vue'
import ReferenceList from '@/modules/chat/components/ReferenceList.vue'
import { formatDateTime } from '@/shared/utils/format'

const route = useRoute()
const projectId = route.params.projectId as string

const sessions = ref<ChatSession[]>([])
const selectedSessionId = ref<string | null>(null)
const messages = ref<ChatMessage[]>([])
const loadingSessions = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)

const newSessionTitle = ref('')
const creatingSession = ref(false)
const inputMessage = ref('')
const streamingContent = ref('')
const streamingMessageId = ref<string | null>(null)
const streamReferences = ref<ChatReference[]>([])
const chatContainer = ref<HTMLElement | null>(null)
const activeStream = ref<SSEStream | null>(null)

async function loadSessions() {
  loadingSessions.value = true
  try {
    const res = await listSessions(projectId, 1, 20)
    sessions.value = res.data.data.records
  } catch { /* handled */ } finally { loadingSessions.value = false }
}

async function handleCreateSession() {
  if (!newSessionTitle.value.trim()) return
  creatingSession.value = true
  try {
    const res = await createSession(projectId, newSessionTitle.value.trim())
    sessions.value.unshift(res.data.data)
    newSessionTitle.value = ''
    selectSession(res.data.data.id)
  } catch { /* handled */ } finally { creatingSession.value = false }
}

async function selectSession(sessionId: string) {
  selectedSessionId.value = sessionId
  streamingContent.value = ''
  streamingMessageId.value = null
  loadingMessages.value = true
  try {
    const res = await getMessages(sessionId)
    messages.value = res.data.data
    await nextTick()
    scrollToBottom()
  } catch { /* handled */ } finally { loadingMessages.value = false }
}

async function handleSendMessage() {
  if (!inputMessage.value.trim() || !selectedSessionId.value) return
  sending.value = true
  try {
    const res = await sendMessage(selectedSessionId.value, {
      content: inputMessage.value,
      agentIds: ['300002'],
      stream: true,
      useRag: false,
      ragLimit: 5,
    })
    const data = res.data.data
    streamingMessageId.value = data.assistantMessageId
    streamingContent.value = ''
    streamReferences.value = data.references || []

    messages.value.push({
      id: data.userMessageId,
      sessionId: selectedSessionId.value,
      senderType: 'USER',
      senderName: null,
      content: inputMessage.value,
      status: 'COMPLETED',
      tokenUsage: 0,
      references: [],
      createTime: new Date().toISOString(),
    })

    inputMessage.value = ''
    await nextTick()
    scrollToBottom()

    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
    activeStream.value = readSSEStream(data.streamUrl, baseUrl, {
      onToken(content: string) {
        streamingContent.value += content
        nextTick(() => scrollToBottom())
      },
      onDone(d: Record<string, unknown>) {
        const finalContent = streamingContent.value
        messages.value.push({
          id: String(data.assistantMessageId),
          sessionId: selectedSessionId.value!,
          senderType: 'AGENT',
          senderName: null,
          content: finalContent,
          status: 'COMPLETED',
          tokenUsage: (d.tokenUsage as number) || 0,
          references: (d.references as ChatReference[]) || streamReferences.value,
          createTime: new Date().toISOString(),
        })
        streamingContent.value = ''
        streamingMessageId.value = null
        activeStream.value = null
        nextTick(() => scrollToBottom())
      },
      onError(code: string, message: string) {
        ElMessage.error(`SSE Error [${code}]: ${message}`)
        streamingMessageId.value = null
        activeStream.value = null
      },
    })
  } catch (e: unknown) {
    const err = e as { message?: string }
    ElMessage.error(err.message || 'Send failed')
  } finally {
    sending.value = false
  }
}

function scrollToBottom() {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

loadSessions()

onBeforeUnmount(() => {
  if (activeStream.value) {
    activeStream.value.abort()
    activeStream.value = null
  }
})
</script>

<template>
  <div class="chat-workspace">
    <!-- Session Rail -->
    <aside class="chat-rail">
      <div class="chat-rail-header">
        <span class="chat-rail-title">Sessions</span>
        <StatusPulse status="AI Ready" tone="primary" />
      </div>
      <div class="session-create">
        <el-input v-model="newSessionTitle" placeholder="New session..." size="small" @keyup.enter="handleCreateSession" />
        <el-button type="primary" size="small" :loading="creatingSession" @click="handleCreateSession">+</el-button>
      </div>
      <div class="session-list" v-loading="loadingSessions">
        <div
          v-for="s in sessions" :key="s.id"
          class="session-item"
          :class="{ active: s.id === selectedSessionId }"
          @click="selectSession(s.id)"
        >
          <div class="session-item-bar" />
          <div class="session-title">{{ s.title }}</div>
          <div class="session-time">{{ formatDateTime(s.lastMessageTime || s.createTime) }}</div>
        </div>
        <EmptyState v-if="!loadingSessions && sessions.length === 0" description="No sessions" />
      </div>
    </aside>

    <!-- Chat Main -->
    <section class="chat-main">
      <div v-if="!selectedSessionId" class="chat-empty">
        <div class="chat-empty-icon">◈</div>
        <div class="chat-empty-text">Select a session to start AI conversation</div>
      </div>
      <template v-else>
        <div class="chat-messages" ref="chatContainer" v-loading="loadingMessages">
          <div
            v-for="msg in messages" :key="msg.id"
            class="chat-msg"
            :class="{ 'chat-msg--user': msg.senderType === 'USER', 'chat-msg--agent': msg.senderType === 'AGENT' }"
          >
            <div class="chat-msg__sender">
              <span class="chat-msg__dot" :class="msg.senderType === 'USER' ? 'dot-user' : 'dot-agent'" />
              {{ msg.senderType === 'USER' ? 'You' : 'Assistant' }}
            </div>
            <div class="chat-msg__content">
              <MarkdownRenderer v-if="msg.content" :content="msg.content" />
              <span v-else class="chat-msg__empty">(empty)</span>
            </div>
            <ReferenceList v-if="msg.references && msg.references.length > 0" :references="msg.references" compact />
          </div>

          <div v-if="streamingMessageId" class="chat-msg chat-msg--agent chat-msg--streaming">
            <div class="chat-msg__sender">
              <span class="chat-msg__dot dot-agent streaming-pulse" />
              Assistant
              <StatusPulse status="Streaming" tone="primary" />
            </div>
            <div class="chat-msg__content">
              <MarkdownRenderer :content="streamingContent" />
              <span class="cursor-blink">|</span>
            </div>
          </div>
        </div>

        <div class="chat-composer">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            placeholder="Send instruction to AI..."
            @keyup.enter.exact.prevent="handleSendMessage"
            class="chat-composer-input"
          />
          <button class="chat-send-btn" :disabled="!inputMessage.trim() || sending" @click="handleSendMessage">
            <span v-if="sending" class="sending-dots">··</span>
            <span v-else>↑</span>
          </button>
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped>
.chat-workspace {
  display: flex;
  height: calc(100vh - 48px - 128px);
  min-height: 520px;
  overflow: hidden;
}

/* ---- Session Rail ---- */
.chat-rail {
  width: 260px;
  flex-shrink: 0;
  border-right: 1px solid var(--app-border);
  background: var(--app-surface);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  display: flex;
  flex-direction: column;
}
.chat-rail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--app-border);
}
.chat-rail-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--app-text);
  text-transform: uppercase;
  letter-spacing: 0.8px;
}
.session-create {
  display: flex;
  gap: 6px;
  padding: 12px;
  border-bottom: 1px solid var(--app-border);
}
.session-create .el-input { flex: 1; }
.session-list { flex: 1; overflow-y: auto; padding: 8px; }
.session-item {
  padding: 10px 12px;
  border-radius: var(--app-radius-sm);
  cursor: pointer;
  margin-bottom: 2px;
  border: 1px solid transparent;
  transition: all 0.2s var(--app-ease-out);
  position: relative;
  overflow: hidden;
}
.session-item-bar {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background: var(--app-primary);
  border-radius: 0 2px 2px 0;
  transition: height 0.2s var(--app-ease-out);
}
.session-item:hover { background: var(--app-panel-hover); }
.session-item.active {
  background: var(--app-primary-soft);
  border-color: rgba(56, 189, 248, 0.2);
}
.session-item.active .session-item-bar {
  height: 60%;
}
.session-title { font-size: 13px; font-weight: 500; color: var(--app-text-soft); }
.session-time { font-size: 11px; color: var(--app-text-muted); margin-top: 2px; }

/* ---- Chat Main ---- */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--app-bg-soft);
}
.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.chat-empty-icon {
  font-size: 48px;
  color: var(--app-text-muted);
  opacity: 0.2;
}
.chat-empty-text {
  color: var(--app-text-muted);
  font-size: 14px;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}
.chat-msg {
  margin-bottom: 20px;
  max-width: 78%;
}
.chat-msg--user { margin-left: auto; }
.chat-msg--agent { margin-right: auto; }
.chat-msg--streaming .chat-msg__content {
  border-color: rgba(56, 189, 248, 0.25);
  box-shadow: 0 0 24px rgba(56, 189, 248, 0.08);
  animation: streamGlow 2s ease-in-out infinite;
}
@keyframes streamGlow {
  0%, 100% { box-shadow: 0 0 16px rgba(56, 189, 248, 0.06); }
  50% { box-shadow: 0 0 28px rgba(56, 189, 248, 0.14); }
}
.chat-msg__sender {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--app-text-muted);
  margin-bottom: 6px;
  padding-left: 4px;
}
.chat-msg__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.dot-user { background: var(--app-accent); box-shadow: 0 0 6px var(--app-accent-glow); }
.dot-agent { background: var(--app-primary); box-shadow: 0 0 6px var(--app-primary-glow); }
.streaming-pulse { animation: statusPulse 1.5s ease-in-out infinite; }
@keyframes statusPulse {
  0%, 100% { box-shadow: 0 0 6px var(--app-primary-glow); }
  50% { box-shadow: 0 0 14px var(--app-primary-glow); }
}
.chat-msg__content {
  padding: 14px 18px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.65;
}
.chat-msg--user .chat-msg__content {
  background: rgba(139, 92, 246, 0.18);
  border: 1px solid rgba(139, 92, 246, 0.25);
  color: var(--app-text);
}
.chat-msg--agent .chat-msg__content {
  background: var(--app-panel);
  border: 1px solid var(--app-border);
  color: var(--app-text-soft);
}
.chat-msg__empty { color: var(--app-text-muted); }

.cursor-blink {
  animation: blink 1s step-end infinite;
  font-weight: bold;
  color: var(--app-primary);
}
@keyframes blink { 50% { opacity: 0; } }

/* ---- Composer ---- */
.chat-composer {
  display: flex;
  gap: 10px;
  padding: 16px 24px;
  background: var(--app-panel);
  border-top: 1px solid var(--app-border);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  align-items: flex-end;
}
.chat-composer-input { flex: 1; }
.chat-send-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid var(--app-border-strong);
  background: var(--app-surface);
  color: var(--app-text-soft);
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s var(--app-ease-out);
  flex-shrink: 0;
}
.chat-send-btn:hover:not(:disabled) {
  background: var(--app-primary-soft);
  border-color: var(--app-primary);
  color: var(--app-primary);
  box-shadow: 0 0 16px var(--app-primary-glow);
  transform: translateY(-1px);
}
.chat-send-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.sending-dots {
  animation: blink 0.6s step-end infinite;
}
</style>
