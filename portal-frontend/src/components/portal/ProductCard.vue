<script setup lang="ts">
import { computed } from 'vue';

interface ProductCardProps {
  title: string;
  description: string;
  route?: string;
  statusText: string;
  active: boolean;
}

const props = defineProps<ProductCardProps>();

const cardTag = computed(() => (props.route ? 'RouterLink' : 'div'));
const cardInitials = computed(() => props.title.slice(0, 2));
const actionText = computed(() => (props.active ? '进入产品' : '敬请期待'));
const isOnlineStatus = computed(() => props.statusText === '已上线');
const isDevelopingStatus = computed(() => props.statusText === '开发中');
</script>

<template>
  <component
    :is="cardTag"
    :to="route"
    class="product-card"
    :class="{
      'product-card--disabled': !active,
      'product-card--interactive': active,
    }"
  >
    <div class="product-card__glow" aria-hidden="true"></div>
    <div class="product-card__header">
      <div class="product-card__identity">
        <div class="product-card__icon" aria-hidden="true">{{ cardInitials }}</div>
        <div class="product-card__title-wrap">
          <span class="product-card__eyebrow">AI Product</span>
          <h3>{{ title }}</h3>
        </div>
      </div>
      <span
        class="badge"
        :class="{
          'badge--online': isOnlineStatus,
          'badge--developing': isDevelopingStatus,
        }"
      >
        {{ statusText }}
      </span>
    </div>

    <p class="product-card__description">{{ description }}</p>

    <div class="product-card__footer">
      <span class="product-card__state">
        <span
          class="product-card__state-dot"
          :class="{
            'product-card__state-dot--online': isOnlineStatus,
            'product-card__state-dot--developing': isDevelopingStatus,
          }"
        ></span>
        <span>{{ active ? '当前可进入体验' : '暂未开放' }}</span>
      </span>
      <span class="product-card__action">
        {{ actionText }}
        <span class="product-card__arrow" aria-hidden="true"></span>
      </span>
    </div>
  </component>
</template>

<style scoped>
.product-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 220px;
  padding: 22px;
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 248, 255, 0.92)),
    rgba(255, 255, 255, 0.96);
  box-shadow:
    0 20px 42px rgba(16, 24, 40, 0.07),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  text-decoration: none;
  color: inherit;
  position: relative;
  overflow: hidden;
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease;
}

.product-card__glow {
  position: absolute;
  top: -56px;
  right: -40px;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.18), rgba(59, 130, 246, 0));
  pointer-events: none;
}

.product-card:hover {
  transform: translateY(-3px);
  border-color: rgba(96, 165, 250, 0.28);
  box-shadow: 0 26px 54px rgba(44, 62, 106, 0.13);
}

.product-card--disabled {
  cursor: not-allowed;
  opacity: 0.82;
}

.product-card--disabled:hover {
  transform: none;
  border-color: rgba(148, 163, 184, 0.16);
  box-shadow:
    0 20px 42px rgba(16, 24, 40, 0.07),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.product-card--interactive {
  cursor: pointer;
}

.product-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.product-card__identity {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.product-card__icon {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background:
    linear-gradient(145deg, rgba(219, 234, 254, 0.96), rgba(191, 219, 254, 0.82)),
    rgba(219, 234, 254, 0.92);
  color: #1d4ed8;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.04em;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.product-card__title-wrap {
  min-width: 0;
}

.product-card__eyebrow {
  display: inline-flex;
  margin-bottom: 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #64748b;
}

h3 {
  margin: 0;
  font-size: 20px;
  line-height: 1.25;
  color: #0f172a;
}

.product-card__description {
  margin: 0;
  color: #475569;
  line-height: 1.75;
  flex: 1;
}

.badge {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.82);
  color: #475569;
  font-size: 12px;
  white-space: nowrap;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.badge--online {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
  border-color: rgba(34, 197, 94, 0.22);
}

.badge--developing {
  background: rgba(125, 211, 252, 0.24);
  color: #0369a1;
  border-color: rgba(125, 211, 252, 0.42);
}

.product-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.8);
}

.product-card__state {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  font-size: 12px;
  color: #64748b;
}

.product-card__state-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  flex-shrink: 0;
}

.product-card__state-dot--online {
  background: #16a34a;
  box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.16);
}

.product-card__state-dot--developing {
  background: #38bdf8;
  box-shadow: 0 0 0 6px rgba(125, 211, 252, 0.24);
}

.product-card__action {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.product-card__arrow {
  width: 8px;
  height: 8px;
  border-top: 2px solid currentColor;
  border-right: 2px solid currentColor;
  transform: rotate(45deg);
}

.product-card--disabled .product-card__icon {
  background:
    linear-gradient(145deg, rgba(241, 245, 249, 0.96), rgba(226, 232, 240, 0.82)),
    rgba(241, 245, 249, 0.92);
  color: #64748b;
}

.product-card--disabled .product-card__action {
  color: #64748b;
}

@media (max-width: 768px) {
  .product-card {
    min-height: 200px;
    padding: 18px;
    border-radius: 20px;
  }

  .product-card__header,
  .product-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
