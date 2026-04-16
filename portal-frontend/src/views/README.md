# Views 目录约定

`views` 目录统一采用按路由分组的结构，目标是让页面入口、页面私有逻辑和样式保持就近维护。

## 目录规范

- 页面统一使用 `views/<route-name>/index.vue` 作为路由入口文件
- 页面私有逻辑放在 `views/<route-name>/composables/`
- 页面私有样式放在 `views/<route-name>/styles/`
- 页面私有常量放在 `views/<route-name>/constants/`

## 推荐示例

```text
views/
  portal-home/
    index.vue
    constants/
    styles/
  game-chat-entry/
    index.vue
    composables/
    styles/
```

## 约束

- 路由文件统一引用目录入口，不再引用 `*View.vue`
- 避免“同层 `FooView.vue` + `foo/` 目录并存”
- 公共逻辑优先放到 `components` 或 `services`，避免复制到多个页面目录
