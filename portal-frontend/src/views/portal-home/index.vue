<script setup lang="ts">
import ProductCard from '../../components/portal/ProductCard.vue';
import { products } from './constants/products';
</script>

<template>
  <main class="portal-page">
    <header class="hero-section">
      <span class="hero-tag">AI 产品工作台</span>
      <h1>智能应用统一入口</h1>
      <p>聚合聊天、知识问答与场景化助手能力，让每个 AI 产品都能在同一体验下快速使用与持续扩展。</p>
    </header>

    <section class="products-section">
      <div class="products-section__header">
        <div>
          <span class="products-section__eyebrow">全部产品</span>
          <h2>产品矩阵</h2>
        </div>
        <p>当前可用能力优先展示，规划中模块将按统一标准持续接入。</p>
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
<style scoped src="./styles/portal-home-view.css"></style>
