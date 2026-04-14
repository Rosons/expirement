import DOMPurify from 'dompurify';
import hljs from 'highlight.js/lib/core';
import bash from 'highlight.js/lib/languages/bash';
import java from 'highlight.js/lib/languages/java';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import markdown from 'highlight.js/lib/languages/markdown';
import python from 'highlight.js/lib/languages/python';
import sql from 'highlight.js/lib/languages/sql';
import typescript from 'highlight.js/lib/languages/typescript';
import xml from 'highlight.js/lib/languages/xml';
import MarkdownIt from 'markdown-it';
import multimdTable from 'markdown-it-multimd-table';

hljs.registerLanguage('bash', bash);
hljs.registerLanguage('shell', bash);
hljs.registerLanguage('java', java);
hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('js', javascript);
hljs.registerLanguage('json', json);
hljs.registerLanguage('markdown', markdown);
hljs.registerLanguage('md', markdown);
hljs.registerLanguage('python', python);
hljs.registerLanguage('py', python);
hljs.registerLanguage('sql', sql);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('ts', typescript);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('html', xml);

function escapeHtml(raw: string): string {
  return raw
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function normalizeLanguage(rawLanguage: string): string {
  return rawLanguage.toLowerCase().replace(/[^\w-]/g, '');
}

const markdownParser = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: true,
  highlight(code, language) {
    const normalizedLanguage = normalizeLanguage(language || '');
    const validLanguage = normalizedLanguage && hljs.getLanguage(normalizedLanguage);

    try {
      const highlighted = validLanguage
        ? hljs.highlight(code, { language: normalizedLanguage, ignoreIllegals: true }).value
        : hljs.highlightAuto(code).value;

      const displayLanguage = validLanguage ? normalizedLanguage : 'text';
      const languageClass = validLanguage ? ` language-${normalizedLanguage}` : '';
      return `<div class="md-code-block"><div class="md-code-block__header"><span class="md-code-block__lang">${escapeHtml(displayLanguage)}</span><button class="md-code-block__copy" type="button">复制</button></div><pre class="hljs${languageClass}"><code>${highlighted}</code></pre></div>`;
    } catch {
      return `<div class="md-code-block"><div class="md-code-block__header"><span class="md-code-block__lang">text</span><button class="md-code-block__copy" type="button">复制</button></div><pre class="hljs"><code>${escapeHtml(code)}</code></pre></div>`;
    }
  },
}).use(multimdTable);

const defaultMarkdown = '...';
const markdownDirectivePrefix = /^(include|define|ifdef|ifndef|endif|pragma|region|endregion)\b/i;

function normalizeMarkdownSyntax(rawMarkdown: string): string {
  const lines = rawMarkdown.split(/\r?\n/);
  let isInFenceBlock = false;

  const normalizedLines = lines.map((line) => {
    const trimmedLine = line.trimStart();
    if (trimmedLine.startsWith('```') || trimmedLine.startsWith('~~~')) {
      isInFenceBlock = !isInFenceBlock;
      return line;
    }

    if (isInFenceBlock) {
      return line;
    }

    const match = line.match(/^(\s{0,3})(#{2,6})(?!#)(\S.*)$/);
    if (!match) {
      return line;
    }

    const [, indent, hashMarks, headingText] = match;
    // 防止把疑似指令/宏（如 ##define）误改成Markdown标题
    if (markdownDirectivePrefix.test(headingText)) {
      return line;
    }

    // 兼容“###标题”这类缺少空格的ATX标题写法，避免被当作普通文本
    return `${indent}${hashMarks} ${headingText}`;
  });

  return normalizedLines.join('\n');
}

export function renderSafeMarkdown(content: string): string {
  const sourceText = content && content.trim().length > 0 ? content : defaultMarkdown;
  const normalizedMarkdown = normalizeMarkdownSyntax(sourceText);
  const renderedHtml = markdownParser.render(normalizedMarkdown);
  return DOMPurify.sanitize(renderedHtml, {
    ADD_TAGS: ['button'],
    ADD_ATTR: ['type', 'align', 'colspan', 'rowspan', 'scope'],
  });
}
