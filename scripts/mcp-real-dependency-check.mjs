// 官方SDK连接真实业务链路；所有凭据仅由父进程环境提供，不输出令牌或文档正文。
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';

const version = JSON.parse(await readFile(new URL('./node_modules/@modelcontextprotocol/sdk/package.json', import.meta.url), 'utf8')).version;
assert.equal(version, '1.32.1');
const knowledgeBaseId = Number(process.env.MCP_KNOWLEDGE_BASE_ID);
const chunkId = Number(process.env.MCP_CHUNK_ID);
const modelId = Number(process.env.MCP_MODEL_ID);
const memberId = Number(process.env.MCP_MEMBER_ID);
const credentialId = Number(process.env.MCP_CREDENTIAL_ID);
for (const id of [knowledgeBaseId, chunkId, modelId, memberId, credentialId]) assert.ok(id > 0);

async function web(path, token, method = 'GET') {
  const response = await fetch(new URL(path, process.env.MCP_WEB_BASE_URL), {
    method,
    headers: { Authorization: `Bearer ${token}` }
  });
  const value = await response.json();
  assert.equal(value.code, 0, `网页HTTP入口失败：${path}`);
  return value.data;
}

const client = new Client({ name: 'dfkb-real-dependencies', version: '1.0.0' });
const transport = new StreamableHTTPClientTransport(new URL(process.env.MCP_BASE_URL), {
  requestInit: { headers: { Authorization: `Bearer ${process.env.MCP_TOKEN}` } }
});
try {
  await client.connect(transport);
  const tools = await client.listTools();
  assert.deepEqual(tools.tools.map(tool => tool.name).sort(), ['ask', 'list_knowledge_bases', 'search']);
  for (const tool of tools.tools) assert.equal(tool.annotations.readOnlyHint, true);
  const before = await web('knowledge/conversation/list', process.env.MCP_WEB_VIEWER_TOKEN);
  const bases = await client.callTool({ name: 'list_knowledge_bases', arguments: {} });
  assert.equal(bases.isError, false);
  assert.ok(bases.structuredContent.list.some(item => item.id === knowledgeBaseId));
  const searched = await client.callTool({ name: 'search', arguments: {
    query: '知识管理', knowledgeBaseIds: [knowledgeBaseId], formats: [5], mode: 'HYBRID', pageNo: 1, pageSize: 20
  } });
  assert.equal(searched.isError, false);
  assert.ok(searched.structuredContent.total > 20);
  assert.equal(searched.structuredContent.list.length, 20);
  assert.ok(searched.structuredContent.list[0].score > 0);
  const asked = await client.callTool({ name: 'ask', arguments: {
    question: '知识管理的用途是什么？', knowledgeBaseIds: [knowledgeBaseId], selectedChunkIds: [chunkId], modelId
  } });
  assert.equal(asked.isError, false);
  assert.equal(asked.structuredContent.answered, true);
  assert.ok(asked.structuredContent.answer.includes('本地模拟'));
  assert.equal(asked.structuredContent.citations[0].chunkId, chunkId);
  assert.deepEqual(await web('knowledge/conversation/list', process.env.MCP_WEB_VIEWER_TOKEN), before);

  // 保持同一客户端和传输连接，在共享库所有者撤权后继续调用。
  await web(`knowledge/base/member/delete?knowledgeBaseId=${knowledgeBaseId}&id=${memberId}`, process.env.MCP_WEB_OWNER_TOKEN, 'DELETE');
  const removed = await client.callTool({ name: 'search', arguments: { query: '知识管理', knowledgeBaseIds: [knowledgeBaseId] } });
  assert.equal(removed.isError, false);
  assert.equal(removed.structuredContent.total, 0);
  const refused = await client.callTool({ name: 'ask', arguments: {
    question: '撤权后继续问', knowledgeBaseIds: [knowledgeBaseId], selectedChunkIds: [chunkId], modelId
  } });
  assert.equal(refused.structuredContent.answered, false);
  assert.equal(refused.structuredContent.citations.length, 0);
  const records = await web('mcp/call-log/page', process.env.MCP_WEB_VIEWER_TOKEN);
  assert.ok(records.total >= 5);
  assert.equal(JSON.stringify(records).includes('保存和检索授权文档'), false);
  assert.equal(JSON.stringify(records).includes(process.env.MCP_TOKEN), false);
  const credentials = await web('mcp/credential/list', process.env.MCP_WEB_VIEWER_TOKEN);
  assert.equal(JSON.stringify(credentials).includes(process.env.MCP_TOKEN), false);
  await web(`mcp/credential/revoke?id=${credentialId}`, process.env.MCP_WEB_VIEWER_TOKEN, 'POST');
  await assert.rejects(() => client.listTools());
  console.log('P0_REAL_MCP_SDK_OK version=1.32.1 initialize tools/list search ask list_knowledge_bases member-revoked credential-revoked read-only audit-private');
} finally {
  await client.close();
}
