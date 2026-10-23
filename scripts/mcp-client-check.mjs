// 官方 MCP SDK v1 的真实 Streamable HTTP 验收，凭证仅从环境读取。
import assert from 'node:assert/strict';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';
const client = new Client({ name: 'dfkb-p0-acceptance', version: '1.0.0' });
const transport = new StreamableHTTPClientTransport(new URL(process.env.MCP_BASE_URL), {
  requestInit: { headers: { Authorization: `Bearer ${process.env.MCP_TOKEN}` } }
});
try {
  await client.connect(transport);
  const listed = await client.listTools();
  assert.deepEqual(listed.tools.map(tool => tool.name).sort(), ['ask','list_knowledge_bases','search']);
  for (const tool of listed.tools) assert.equal(tool.annotations.readOnlyHint, true);
  const bases = await client.callTool({ name: 'list_knowledge_bases', arguments: {} });
  assert.equal(bases.isError, false);
  const searched = await client.callTool({ name: 'search', arguments: { query: '沸点', mode: 'HYBRID', pageNo: 1 } });
  assert.equal(searched.isError, false);
  assert.equal(searched.structuredContent.total, 1);
  const asked = await client.callTool({ name: 'ask', arguments: { question: '沸点？', selectedChunkIds: [100] } });
  assert.equal(asked.isError, false);
  assert.equal(asked.structuredContent.answered, true);
  assert.equal(asked.structuredContent.citations[0].chunkId, 100);
  console.log('MCP_OFFICIAL_CLIENT_OK initialize tools/list search ask list_knowledge_bases');
} finally {
  await client.close();
}
