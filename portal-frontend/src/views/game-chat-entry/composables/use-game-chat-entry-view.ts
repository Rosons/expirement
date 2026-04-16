import { useRouter } from 'vue-router';

export function useGameChatEntryView() {
  const router = useRouter();

  async function handleStartGame(reason: string): Promise<void> {
    const entryMessage = reason.trim() || '开始游戏';
    await router.push({
      name: 'product-game-chat-play',
      query: {
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
