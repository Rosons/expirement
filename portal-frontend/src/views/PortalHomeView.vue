<script setup lang="ts">
import ProductCard from '../components/portal/ProductCard.vue';

interface ProductInfo {
  title: string;
  description: string;
  statusText: string;
  route?: string;
  active: boolean;
}

const products: ProductInfo[] = [
  {
    title: '智能聊天',
    description: '基于 Spring AI 的多会话记忆聊天，支持流式 Markdown 展示与会话历史回溯。',
    route: '/products/chat',
    statusText: '已上线',
    active: true,
  },
  {
    title: '哄哄模拟器',
    description: '先输入女友生气原因再开局，进入聊天后基于通用对话能力进行实时哄人模拟。',
    route: '/products/game-chat',
    statusText: '已上线',
    active: true,
  },
  {
    title: '知识问答',
    description: '基于知识库检索增强的对话能力，支持文档索引、语义召回与可追溯答案生成。',
    statusText: '开发中',
    active: false,
  },
  {
    title: '智能客服',
    description: '通过聊天对话结合 Function Calling，完成问题分流、工单处理与业务动作自动执行。',
    statusText: '规划中',
    active: false,
  },
  {
    title: '流程智能体',
    description: '用于多步骤任务协同与业务自动化编排，作为统一入口中的高级产品模块。',
    statusText: '规划中',
    active: false,
  },
];
</script>

<template>
  <main class="portal-page">
    <header class="hero-section">
      <div class="hero-tag">Expirement Unified Portal</div>
      <h1>实验产品统一入口</h1>
      <p>沉浸式、简约且可扩展的 AI 工作台。从智能聊天开始，后续产品模块可以继续在这里自然接入。</p>
    </header>

    <section class="products-section">
      <div class="products-section__header">
        <div>
          <span class="products-section__eyebrow">Products</span>
          <h2>产品矩阵</h2>
        </div>
        <p>优先展示当前可体验产品，同时为后续模块预留统一视觉入口。</p>
      </div>

      <div class="product-grid">
        <ProductCard
          v-for="product in products"
          :key="product.title"
          :title="product.title"
          :description="product.description"
          :status-text="product.statusText"
          :route="product.route"
          :active="product.active"
        />
      </div>
    </section>
  </main>
</template>

<style scoped>
.portal-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 58px 28px 72px;
}

.hero-section {
  margin-bottom: 26px;
  padding: 8px 4px 0;
}

.hero-tag {
  display: inline-flex;
  padding: 6px 14px;
  border-radius: 999px;
  margin-bottom: 16px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #1e40af;
  background: rgba(59, 130, 246, 0.14);
}

h1 {
  margin: 0 0 14px;
  font-size: clamp(32px, 5vw, 44px);
  letter-spacing: 0.01em;
  color: #0b1220;
}

p {
  margin: 0;
  max-width: 780px;
  line-height: 1.8;
  color: #475569;
}

.products-section {
  padding: 24px;
  border-radius: 28px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.64);
  box-shadow: 0 22px 46px rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(16px);
}

.products-section__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;
}

.products-section__eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #64748b;
}

.products-section__header h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
  color: #0f172a;
}

.products-section__header p {
  max-width: 420px;
  font-size: 14px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

@media (max-width: 768px) {
  .portal-page {
    padding: 34px 16px 44px;
  }

  .products-section {
    padding: 18px;
    border-radius: 22px;
  }

  .products-section__header {
    flex-direction: column;
    align-items: flex-start;
    margin-bottom: 18px;
  }
}
</style>
