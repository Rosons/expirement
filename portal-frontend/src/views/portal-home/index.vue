<script setup lang="ts">
import ProductCard from '../../components/portal/ProductCard.vue';
import { products } from './constants/products';
</script>

<template>
  <main class="portal-page">
    <header class="hero-section">
      <span class="hero-tag">Expirement Unified Portal</span>
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
<style scoped src="./styles/portal-home-view.css"></style>
