package com.df.knowledge.service.doc;

/** 索引实际命中类型及相关性，混合得分采用 RRF，模糊得分采用向量相似度。 */
public record DocIndexHit(Long chunkId, double score, String matchType) { }
