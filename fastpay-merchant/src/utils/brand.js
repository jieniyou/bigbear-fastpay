/**
 * 品牌配置工具
 * 统一缓存和应用网站名称、署名等运行时配置。
 */

const STORAGE_KEY = 'fastpay_brand_config'
const DEFAULT_SITE_AUTHOR = '大熊Bigbear'

export const DEFAULT_BRAND_CONFIG = {
  siteName: 'FAST 易支付',
  siteAuthor: DEFAULT_SITE_AUTHOR,
  authorText: `by ${DEFAULT_SITE_AUTHOR}`
}

/**
 * 标准化品牌配置。
 */
export function normalizeBrandConfig(config = {}) {
  const siteName = config.siteName || DEFAULT_BRAND_CONFIG.siteName
  const siteAuthor = config.siteAuthor || DEFAULT_BRAND_CONFIG.siteAuthor
  return {
    siteName,
    siteAuthor,
    authorText: config.authorText || `by ${siteAuthor}`
  }
}

/**
 * 读取缓存品牌配置。
 */
export function getCachedBrandConfig() {
  try {
    const cached = localStorage.getItem(STORAGE_KEY)
    return cached ? normalizeBrandConfig(JSON.parse(cached)) : DEFAULT_BRAND_CONFIG
  } catch (error) {
    return DEFAULT_BRAND_CONFIG
  }
}

/**
 * 保存品牌配置缓存。
 */
export function saveBrandConfig(config) {
  const normalized = normalizeBrandConfig(config)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized))
  return normalized
}

/**
 * 应用浏览器标题。
 */
export function applyBrandTitle(pageTitle) {
  const brand = getCachedBrandConfig()
  document.title = pageTitle ? `${pageTitle} - ${brand.siteName}` : brand.siteName
}
