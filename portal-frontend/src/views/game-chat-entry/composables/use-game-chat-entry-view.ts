import { useRouter } from 'vue-router';
import { createChatId } from '../../../components/chat/workspace';

export function useGameChatEntryView() {
  const router = useRouter();

  async function handleStartGame(reason: string): Promise<void> {
    const entryMessage = reason.trim() || '开始游戏';
    const sessionId = createChatId();
    await router.push({
      name: 'product-game-chat-play',
      query: {
        sessionId,
        gameStart: entryMessage,
      },
    });
  }

  function goHome(): void {
    void router.push('/');
  }

  return {
    handleStartGame,
    goHome,
  };
}
