/**
 * 文件接口层：通用文件上传、下载与删除
 *
 * 1. URL 前缀 /system/file，返回统一 CommonResult（下载接口直接写字节流除外）
 * 2. 接口仅要求登录，下载/删除不做引用与归属校验（ADR-0009）
 */
package com.df.knowledge.controller.file;
