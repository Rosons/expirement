import { nextTick, ref, type Ref } from 'vue';

type UseChatScrollOptions = {
  chatBodyRef: Ref<HTMLElement | null>;
  hasMoreOlder: Ref<boolean>;
  isLoadingOlder: Ref<boolean>;
  isLoadingHistory: Ref<boolean>;
  loadOlderMessages: () => void | Promise<void>;
};

export function useChatScroll(options: UseChatScrollOptions) {
  const shouldAutoScroll = ref(true);
  const hasUnseenNewMessages = ref(false);

  function isNearBottom(threshold = 72): boolean {
    const element = options.chatBodyRef.value;
    if (!element) {
      return true;
    }
    const { scrollTop, clientHeight, scrollHeight } = element;
    return scrollTop + clientHeight >= scrollHeight - threshold;
  }

  function isNearTop(threshold = 120): boolean {
    const element = options.chatBodyRef.value;
    if (!element) {
      return false;
    }
    return element.scrollTop <= threshold;
  }

  function handleChatScroll(): void {
    const nearBottom = isNearBottom();
    shouldAutoScroll.value = nearBottom;
    if (nearBottom) {
      hasUnseenNewMessages.value = false;
    }
    if (
      isNearTop() &&
      options.hasMoreOlder.value &&
      !options.isLoadingOlder.value &&
      !options.isLoadingHistory.value
    ) {
      void options.loadOlderMessages();
    }
  }

  function scrollToBottom(optionsArg?: { immediate?: boolean; retryCount?: number; force?: boolean }): void {
    const element = options.chatBodyRef.value;
    if (!element) {
      return;
    }

    const immediate = optionsArg?.immediate ?? false;
    const retryCount = optionsArg?.retryCount ?? 2;
    const force = optionsArg?.force ?? false;

    if (!force && !shouldAutoScroll.value) {
      return;
    }

    nextTick(() => {
      const target = options.chatBodyRef.value;
      if (!target) {
        return;
      }
      const behavior: ScrollBehavior = immediate ? 'auto' : 'smooth';
      target.scrollTo({ top: target.scrollHeight, behavior });

      requestAnimationFrame(() => {
        const rafTarget = options.chatBodyRef.value;
        rafTarget?.scrollTo({ top: rafTarget.scrollHeight, behavior: 'auto' });
      });

      if (retryCount > 0) {
        setTimeout(() => {
          scrollToBottom({ immediate: true, retryCount: retryCount - 1, force });
        }, 80);
      }
    });
  }

  function scrollToLatestMessage(): void {
    shouldAutoScroll.value = true;
    hasUnseenNewMessages.value = false;
    scrollToBottom({ immediate: false, retryCount: 4, force: true });
  }

  function markNewMessageIfNeeded(): boolean {
    if (!shouldAutoScroll.value) {
      hasUnseenNewMessages.value = true;
      return true;
    }
    return false;
  }

  function resetUnreadState(): void {
    shouldAutoScroll.value = true;
    hasUnseenNewMessages.value = false;
  }

  return {
    shouldAutoScroll,
    hasUnseenNewMessages,
    handleChatScroll,
    scrollToBottom,
    scrollToLatestMessage,
    markNewMessageIfNeeded,
    resetUnreadState,
  };
}
