import { ElMessage } from 'element-plus';
import { computed, toRef } from 'vue';
import type { ChatRole } from '../../../../types/chat';
import { renderSafeMarkdown } from '../../../../utils/markdown';
import { copyTextToClipboard } from '../helpers/clipboard';

type MessageBubbleProps = {
  messageId: string;
  role: ChatRole;
  content: string;
  createdAt: number;
  streaming?: boolean;
  enableCopyAction?: boolean;
  enableResendAction?: boolean;
};

export function useMessageBubble(props: MessageBubbleProps, resend: (content: string) => void) {
  const content = toRef(props, 'content');
  const streaming = toRef(props, 'streaming');
  const role = toRef(props, 'role');
  const createdAt = toRef(props, 'createdAt');
  const enableCopyAction = toRef(props, 'enableCopyAction');
  const enableResendAction = toRef(props, 'enableResendAction');

  const renderedHtml = computed(() => {
    // 依赖 streaming：流式结束瞬间内容与最后一次分片相同，但仍需重新跑 Markdown 解析与 v-html 更新
    void streaming.value;
    return renderSafeMarkdown(content.value);
  });
  const canCopyMessage = computed(() => enableCopyAction.value && content.value.trim().length > 0);
  const canResendMessage = computed(
    () => enableResendAction.value && role.value === 'user' && !streaming.value && content.value.trim().length > 0,
  );

  const roleLabel = computed(() => {
    if (role.value === 'user') {
      return '用户';
    }
    if (role.value === 'assistant') {
      return 'AI 助手';
    }
    return '系统提示';
  });

  const avatarLabel = computed(() => {
    if (role.value === 'user') {
      return '你';
    }
    if (role.value === 'assistant') {
      return 'AI';
    }
    return '系';
  });

  const formattedTime = computed(() => {
    if (!Number.isFinite(createdAt.value)) {
      return '';
    }
    return new Intl.DateTimeFormat('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(createdAt.value);
  });

  const showAssistantStreamingPlaceholder = computed(
    () => role.value === 'assistant' && Boolean(streaming.value) && content.value.trim().length === 0,
  );

  async function handleMarkdownClick(event: MouseEvent): Promise<void> {
    const clickedElement = event.target as HTMLElement | null;
    const copyButton = clickedElement?.closest('.md-code-block__copy') as HTMLButtonElement | null;
    if (!copyButton) {
      return;
    }

    const codeElement = copyButton.closest('.md-code-block')?.querySelector('pre code') as HTMLElement | null;
    const codeText = codeElement?.innerText ?? '';
    if (!codeText.trim()) {
      ElMessage.warning('代码块无可复制内容');
      return;
    }

    try {
      await copyTextToClipboard(codeText);
      ElMessage.success('代码已复制');
    } catch (error) {
      console.error(error);
      ElMessage.error('复制失败');
    }
  }

  async function handleCopyMessage(): Promise<void> {
    if (!canCopyMessage.value) {
      ElMessage.warning('当前消息无可复制内容');
      return;
    }
    try {
      await copyTextToClipboard(content.value);
      ElMessage.success('消息已复制');
    } catch (error) {
      console.error(error);
      ElMessage.error('复制失败');
    }
  }

  function handleResendMessage(): void {
    if (!canResendMessage.value) {
      return;
    }
    resend(content.value);
  }

  return {
    renderedHtml,
    canCopyMessage,
    canResendMessage,
    roleLabel,
    avatarLabel,
    formattedTime,
    showAssistantStreamingPlaceholder,
    handleMarkdownClick,
    handleCopyMessage,
    handleResendMessage,
  };
}
