import { watch } from 'vue'
import { useI18n } from 'vue-i18n'

const defaultDescription = '校园安全门户：学校安全体系介绍、访客预约、失物招领、安全资讯与预警公告。'

/**
 * 门户 SEO：标题、描述、canonical 与中英文 hreflang，SPA 路由切换实时更新。
 */
export function useSeo(options: {
  title: () => string
  description?: () => string
  path?: () => string
}) {
  const { locale } = useI18n()

  function apply() {
    const title = options.title()
    document.title = title ? `${title} · 校园安全门户` : '校园安全门户'
    const desc = options.description?.() || defaultDescription
    setMeta('description', desc)
    setMeta('og:title', title, true)
    setMeta('og:description', desc, true)
    const path = options.path?.() || location.pathname
    const origin = location.origin
    setLink('canonical', `${origin}${path}`)
    setHreflang('zh-CN', `${origin}${path}?lang=zh-CN`)
    setHreflang('en-US', `${origin}${path}?lang=en-US`)
    setHreflang('x-default', `${origin}${path}`)
    document.documentElement.lang = locale.value
  }

  watch([locale, options.title, options.description], apply, { immediate: true })
  return { apply }
}

function setMeta(name: string, content: string, property = false) {
  const attr = property ? 'property' : 'name'
  let el = document.head.querySelector<HTMLMetaElement>(`meta[${attr}="${name}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, name)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

function setLink(rel: string, href: string) {
  let el = document.head.querySelector<HTMLLinkElement>(`link[rel="${rel}"]`)
  if (!el) {
    el = document.createElement('link')
    el.setAttribute('rel', rel)
    document.head.appendChild(el)
  }
  el.setAttribute('href', href)
}

function setHreflang(lang: string, href: string) {
  let el = document.head.querySelector<HTMLLinkElement>(`link[rel="alternate"][hreflang="${lang}"]`)
  if (!el) {
    el = document.createElement('link')
    el.setAttribute('rel', 'alternate')
    el.setAttribute('hreflang', lang)
    document.head.appendChild(el)
  }
  el.setAttribute('href', href)
}
