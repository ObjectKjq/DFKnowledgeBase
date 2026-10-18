import { useState } from 'react'

import type { ModelCfg, ModelConfig } from '@/mock/data'
import { useKb } from '@/stores/knowledge'

import { Icon } from '@/components/Icon'

const FORMS: [keyof ModelConfig, string, string, string, string][] = [
  ['chat', '对话模型', '生成带引用的答案', 'Qwen3', 'http://localhost:11434/v1'],
  ['embedding', '向量模型', '建立文本与图片的语义索引', 'bge-m3', 'http://localhost:8001/v1'],
  ['rerank', '重排模型', '对召回结果重新排序', 'bge-reranker-v2-m3', 'http://localhost:8002/v1'],
]

function ModelForm({ id, title, desc, fallbackName, fallbackUrl }: {
  id: keyof ModelConfig
  title: string
  desc: string
  fallbackName: string
  fallbackUrl: string
}) {
  const modelConfig = useKb((s) => s.modelConfig)
  const saveModelConfig = useKb((s) => s.saveModelConfig)
  const openModal = useKb((s) => s.openModal)
  const toastMsg = useKb((s) => s.toastMsg)

  const cfg: ModelCfg = modelConfig[id]
  const [name, setName] = useState(cfg.name || fallbackName)
  const [url, setUrl] = useState(cfg.url || fallbackUrl)
  const [type, setType] = useState(cfg.type || '本地模型服务')
  const [key, setKey] = useState('')
  const [state, setState] = useState('示例配置')
  const [result, setResult] = useState('')
  const [testing, setTesting] = useState(false)

  const test = async () => {
    setTesting(true)
    await new Promise((r) => setTimeout(r, 600))
    setTesting(false)
    setState('演示检查通过')
    setResult('仅检查字段格式，未发起实际服务请求')
    toastMsg('演示检查通过，未调用模型服务')
  }

  const save = () => {
    saveModelConfig(id, { name: name.trim(), url: url.trim(), type })
    setKey('')
  }

  return (
    <form
      className="settings-card model-form"
      onSubmit={(e) => {
        e.preventDefault()
        if (id === 'embedding') openModal({ kind: 'vector-confirm' }, save)
        else save()
      }}
    >
      <h3>
        {title}
        <span className="tag" id={`model-state-${id}`}>
          {state}
        </span>
      </h3>
      <p>{desc}</p>
      <div className="form-grid">
        <label className="field">
          接入方式
          <select value={type} onChange={(e) => setType(e.target.value)}>
            <option>本地模型服务</option>
            <option>兼容标准接口</option>
          </select>
        </label>
        <label className="field">
          模型名称
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label className="field">
          服务地址
          <input value={url} onChange={(e) => setUrl(e.target.value)} type="url" required />
        </label>
        <label className="field">
          API 密钥
          <input
            type="password"
            value={key}
            autoComplete="off"
            placeholder="如服务需要，可填写（不持久化）"
            onChange={(e) => setKey(e.target.value)}
          />
        </label>
      </div>
      <div className="form-actions">
        <span className="field-hint" id={`model-result-${id}`} style={{ marginRight: 'auto' }}>
          {result}
        </span>
        <button type="button" className="btn ghost" onClick={() => void test()} disabled={testing}>
          {testing ? '检查中…' : '测试连接'}
        </button>
        <button type="submit" className="btn primary">
          保存演示配置
        </button>
      </div>
    </form>
  )
}

/** 实例设置 Tab（复刻原型 modelsHTML）。 */
export function ModelsTab() {
  return (
    <>
      <div className="info-banner">
        <Icon name="spark" />
        <span>
          聊天界面仅选择对话模型与档位。向量、重排模型由实例管理员统一配置；更换向量模型需要重建索引。下方连接测试与保存均为界面演示。
        </span>
      </div>
      {FORMS.map(([id, title, desc, name, url]) => (
        <ModelForm key={id} id={id} title={title} desc={desc} fallbackName={name} fallbackUrl={url} />
      ))}
    </>
  )
}
