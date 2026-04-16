<script setup lang="ts">
import { ArrowRightBold, SuccessFilled, Tools } from '@element-plus/icons-vue';
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
const statusBadgeClass = computed(() => {
  if (isOnlineStatus.value) {
    return 'badge--online';
  }
  if (isDevelopingStatus.value) {
    return 'badge--developing';
  }
  return 'badge--planned';
});
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
      <span class="badge" :class="statusBadgeClass">
        {{ statusText }}
      </span>
    </div>

    <p class="product-card__description">{{ description }}</p>

    <div class="product-card__footer">
      <span class="product-card__state">
        <el-icon
          class="product-card__state-icon"
          :class="{ 'product-card__state-icon--developing': !isOnlineStatus }"
        >
          <SuccessFilled v-if="isOnlineStatus" />
          <Tools v-else />
        </el-icon>
        <span>{{ active ? '当前可进入体验' : '暂未开放' }}</span>
      </span>
      <span class="product-card__action">
        {{ actionText }}
        <el-icon class="product-card__arrow" aria-hidden="true">
          <ArrowRightBold />
        </el-icon>
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 22px;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11px;
  line-height: 1.2;
  font-weight: 600;
  letter-spacing: 0.01em;
  white-space: nowrap;
  border: 1px solid transparent;
}

.badge--online {
  background: #bbf7d0;
  color: #166534;
  border-color: #22c55e;
}

.badge--developing {
  background: #67e8f9;
  color: #164e63;
  border-color: #22d3ee;
}

.badge--planned {
  background: #fde047;
  color: #854d0e;
  border-color: #facc15;
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

.product-card__state-icon {
  font-size: 14px;
  color: #22c55e;
  flex-shrink: 0;
}

.product-card__state-icon--developing {
  color: #38bdf8;
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
  font-size: 12px;
}

.product-card--disabled .product-card__state-icon {
  color: #94a3b8;
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
