export interface ProductInfo {
  title: string;
  description: string;
  statusText: string;
  route?: string;
  active: boolean;
}

export const products: ProductInfo[] = [
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
    route: '/products/knowledge-chat',
    statusText: '已上线',
    active: true,
  },
  {
    title: '智能客服',
    description: '通过聊天对话结合 Function Calling，完成问题分流、工单处理与业务动作自动执行。',
    route: '/products/customer-chat',
    statusText: '已上线',
    active: true,
  },
  {
    title: '流程智能体',
    description: '用于多步骤任务协同与业务自动化编排，作为统一入口中的高级产品模块。',
    statusText: '规划中',
    active: false,
  },
];
