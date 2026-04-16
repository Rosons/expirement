import { ElMessage } from 'element-plus';
import { computed } from 'vue';
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
  const renderedHtml = computed(() => renderSafeMarkdown(props.content));
  const canCopyMessage = computed(() => props.enableCopyAction && props.content.trim().length > 0);
  const canResendMessage = computed(
    () => props.enableResendAction && props.role === 'user' && !props.streaming && props.content.trim().length > 0,
  );

  const roleLabel = computed(() => {
    if (props.role === 'user') {
      return '用户';
    }
    if (props.role === 'assistant') {
      return 'AI 助手';
    }
    return '系统提示';
  });

  const avatarLabel = computed(() => {
    if (props.role === 'user') {
      return '你';
    }
    if (props.role === 'assistant') {
      return 'AI';
    }
    return '系';
  });

  const formattedTime = computed(() => {
    if (!Number.isFinite(props.createdAt)) {
      return '';
    }
    return new Intl.DateTimeFormat('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(props.createdAt);
  });

  const showAssistantStreamingPlaceholder = computed(
    () => props.role === 'assistant' && Boolean(props.streaming) && props.content.trim().length === 0,
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
      await copyTextToClipboard(props.content);
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
    resend(props.content);
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
