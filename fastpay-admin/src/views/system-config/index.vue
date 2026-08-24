<template>
  <div class="system-config-page dark-config-page">
    <div class="settings-nav">
      <button
        v-for="item in settingNavItems"
        :key="item.key"
        type="button"
        class="settings-nav-item"
        :class="{ active: activeTab === item.target, disabled: item.disabled }"
        @click="switchSettingTab(item)"
      >
        <span class="nav-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <span>{{ item.label }}</span>
      </button>
    </div>

    <div class="config-shell">
      <div class="mail-titlebar">
        <div class="title-left">
          <span class="step-badge">{{ activeTab === 'mail' ? '6' : '1' }}</span>
          <div>
            <h2>{{ activeTab === 'mail' ? '邮件配置' : '系统设置' }}</h2>
            <p>{{ activeTab === 'mail' ? '配置 SMTP 服务、测试邮件和订单邮件模板' : '配置后台与商户端展示的网站名称和署名' }}</p>
          </div>
        </div>
        <span class="permission-pill">管理员权限</span>
      </div>

      <el-form
        v-if="activeTab === 'site'"
        ref="brandFormRef"
        :model="brandData"
        :rules="brandRules"
        label-position="top"
        class="config-form site-form"
      >
        <div class="dark-card site-card">
          <div class="section-heading">
            <span class="card-icon"><el-icon><Setting /></el-icon></span>
            <div>
              <h3>网站信息</h3>
              <p>用于后台、商户端、浏览器标题和页脚展示。</p>
            </div>
          </div>

          <div class="site-grid">
            <el-form-item label="网站名称" prop="siteName">
              <el-input v-model="brandData.siteName" placeholder="请输入网站名称" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="网站署名" prop="siteAuthor">
              <el-input v-model="brandData.siteAuthor" placeholder="请输入署名，例如 xxx" maxlength="50" show-word-limit />
            </el-form-item>
          </div>

          <div class="brand-preview">
            <div class="preview-logo">易</div>
            <div>
              <div class="preview-name">{{ brandData.siteName || '网站名称' }}</div>
              <div class="preview-author">by {{ brandData.siteAuthor || 'xxx' }}</div>
            </div>
          </div>

          <div class="card-actions">
            <el-button @click="loadBrandData">重置</el-button>
            <el-button type="primary" :loading="brandSaving" @click="handleSaveBrand">
              保存系统设置
            </el-button>
          </div>
        </div>
      </el-form>

      <el-form
        v-else
        ref="mailFormRef"
        :model="mailData"
        :rules="mailRules"
        label-position="top"
        class="config-form mail-config-form"
      >
        <div class="mail-layout">
          <div class="mail-main">
            <section class="dark-card smtp-card">
              <div class="section-heading">
                <span class="card-icon"><el-icon><Message /></el-icon></span>
                <div>
                  <h3>SMTP 邮件服务</h3>
                  <p>用于注册验证、订单通知、订单确认和订单关闭结果通知。</p>
                </div>
                <el-switch v-model="mailData.mailEnabled" active-text="已启用" inactive-text="已停用" />
              </div>

              <div class="form-grid">
                <el-form-item label="SMTP 服务器" prop="smtpHost">
                  <el-input v-model="mailData.smtpHost" placeholder="smtp.qq.com" />
                </el-form-item>
                <el-form-item label="端口" prop="smtpPort">
                  <el-input-number v-model="mailData.smtpPort" :min="1" :max="65535" class="full-number" />
                </el-form-item>
                <el-form-item label="SMTP 登录账号" prop="smtpUsername">
                  <el-input v-model="mailData.smtpUsername" placeholder="xiaomin.summer@qq.com" />
                  <div class="form-tip">只用于 SMTP 服务身份认证。</div>
                </el-form-item>
                <el-form-item label="发件邮箱" prop="fromEmail">
                  <el-input v-model="mailData.fromEmail" placeholder="notice@example.com" />
                  <div class="form-tip">收件人实际看到的 From 地址。</div>
                </el-form-item>
                <el-form-item label="发件名称" prop="fromName">
                  <el-input v-model="mailData.fromName" placeholder="FAST 易支付" />
                  <div class="form-tip">用于替代邮箱账号展示在收件箱列表中。</div>
                </el-form-item>
                <el-form-item label="SMTP 密码" prop="smtpPassword">
                  <el-input
                    v-model="mailData.smtpPassword"
                    type="password"
                    show-password
                    :placeholder="passwordConfigured ? '已配置，留空则保持当前密码' : '请输入SMTP授权码或密码'"
                  />
                  <div class="form-tip">留空会保留当前已保存的密码或授权码。</div>
                </el-form-item>
              </div>

              <div class="ssl-row">
                <div>
                  <strong>使用 SSL</strong>
                  <p>适用于 465 等直接建立 SSL 连接的端口；关闭时通常使用 STARTTLS。</p>
                </div>
                <el-switch v-model="mailData.sslEnabled" />
              </div>

              <div class="form-grid bottom-grid">
                <el-form-item label="操作链接有效期" prop="actionTokenExpireMinutes">
                  <el-input-number
                    v-model="mailData.actionTokenExpireMinutes"
                    :min="1"
                    :max="1440"
                    :step="5"
                    class="full-number"
                  />
                  <div class="form-tip">单位：分钟，确认/关闭按钮使用一次后会失效。</div>
                </el-form-item>
                <el-form-item label="平台外部访问地址" prop="publicBaseUrl">
                  <el-input v-model="mailData.publicBaseUrl" :placeholder="publicBaseUrlPlaceholder" />
                  <div class="form-tip">可留空。留空时按当前访问域名生成邮件链接。</div>
                </el-form-item>
              </div>

              <div class="card-actions">
                <el-button type="primary" :loading="mailSaving" @click="handleSaveMail">
                  保存邮件服务
                </el-button>
              </div>
            </section>

            <section class="dark-card test-card">
              <div class="section-heading compact">
                <span class="card-icon"><el-icon><Connection /></el-icon></span>
                <div>
                  <h3>发送测试邮件</h3>
                  <p>使用已保存的 SMTP 配置验证发信能力。</p>
                </div>
              </div>
              <div class="test-row">
                <el-input v-model="testData.testEmail" placeholder="请输入实际接收测试邮件的邮箱" />
                <el-button type="primary" :loading="testSending" @click="handleSendTestMail">
                  发送测试邮件
                </el-button>
              </div>
            </section>

            <section class="dark-card template-card">
              <div class="section-heading">
                <span class="card-icon"><el-icon><Operation /></el-icon></span>
                <div>
                  <h3>邮件模板</h3>
                  <p>按真实业务事件自定义主题和 HTML，保存后立即用于发送。</p>
                </div>
                <div class="template-actions">
                  <el-button @click="resetCurrentTemplate">恢复默认</el-button>
                  <el-button type="primary" :loading="mailSaving" @click="handleSaveMail">
                    保存模板
                  </el-button>
                </div>
              </div>

              <div class="event-selector-row">
                <div class="event-select-field">
                  <label>邮件事件</label>
                  <el-select v-model="selectedEventKey" class="event-select">
                    <el-option
                      v-for="item in mailEvents"
                      :key="item.key"
                      :label="item.label"
                      :value="item.key"
                    />
                  </el-select>
                </div>
                <div class="event-desc-card">
                  <div class="event-desc-title">
                    <span>{{ currentEvent.label }}</span>
                    <el-switch v-model="mailData[currentEvent.enabledKey]" active-text="启用" inactive-text="停用" />
                  </div>
                  <p>{{ currentEvent.description }}</p>
                </div>
              </div>

              <el-form-item label="邮件主题">
                <el-input
                  ref="subjectInputRef"
                  v-model="mailData[currentEvent.subjectKey]"
                  placeholder="请输入邮件主题，可使用占位符"
                  @focus="activeEditor = 'subject'"
                />
              </el-form-item>

              <div class="template-grid">
                <div class="template-editor-panel">
                  <div class="panel-bar">
                    <span class="bar-label"><el-icon><EditPen /></el-icon> HTML 模板</span>
                    <em>{{ currentTemplateLength }} / 30,000</em>
                  </div>
                  <el-input
                    ref="templateInputRef"
                    v-model="mailData[currentEvent.templateKey]"
                    type="textarea"
                    :rows="20"
                    resize="vertical"
                    class="html-editor"
                    placeholder="请输入 HTML 模板，可使用占位符"
                    @focus="activeEditor = 'template'"
                  />
                  <div class="placeholder-strip">
                    <div>
                      <strong>可用占位符</strong>
                      <p>点击后插入到最近聚焦的字段，发送时由后端安全替换。</p>
                    </div>
                    <div class="placeholder-groups">
                      <div v-for="group in placeholderGroups" :key="group.title" class="placeholder-group">
                        <span class="group-title">{{ group.title }}</span>
                        <button
                          v-for="item in group.items"
                          :key="item.key"
                          type="button"
                          class="placeholder-chip"
                          @click="insertPlaceholder(item.key)"
                        >
                          <span v-text="formatPlaceholder(item.key)" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="preview-panel">
                  <div class="panel-bar">
                    <span class="bar-label"><el-icon><View /></el-icon> 实时预览</span>
                    <em>{{ previewSubject || currentEvent.label }}</em>
                  </div>
                  <iframe class="preview-frame" :srcdoc="previewHtml" sandbox="" />
                </div>
              </div>
            </section>
          </div>

          <aside class="mail-side">
            <div class="status-card">
              <div class="status-dot" :class="{ enabled: mailData.mailEnabled }">
                <el-icon v-if="mailData.mailEnabled"><CircleCheck /></el-icon>
                <span v-else>!</span>
              </div>
              <h3>{{ mailData.mailEnabled ? '邮件服务已启用' : '邮件服务未启用' }}</h3>
              <p>{{ mailData.smtpHost || '尚未填写 SMTP 服务器' }}</p>
              <div class="status-list">
                <div>
                  <span>发件名称</span>
                  <b>{{ mailData.fromName || '-' }}</b>
                </div>
                <div>
                  <span>发件邮箱</span>
                  <b>{{ mailData.fromEmail || '-' }}</b>
                </div>
                <div>
                  <span>密码状态</span>
                  <b>{{ passwordConfigured || mailData.smtpPassword ? '已配置' : '未配置' }}</b>
                </div>
                <div>
                  <span>可配置模板</span>
                  <b>{{ mailEvents.length }} 个</b>
                </div>
                <div>
                  <span>操作链接</span>
                  <b>{{ mailData.actionTokenExpireMinutes || 30 }} 分钟</b>
                </div>
                <div>
                  <span>最后更新</span>
                  <b>保存后即时生效</b>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
/**
 * 系统配置页面。
 * 维护网站信息、SMTP 发信配置和订单邮件事件模板。
 */
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Brush,
  CircleCheck,
  Connection,
  Cpu,
  CreditCard,
  DataLine,
  EditPen,
  Message,
  Operation,
  Setting,
  UploadFilled,
  View
} from '@element-plus/icons-vue'
import {
  getAdminBrandConfig,
  getMailConfig,
  sendTestMail,
  updateBrandConfig,
  updateMailConfig
} from '@/api'
import { notifyBrandConfigUpdated } from '@/utils/brand'

const activeTab = ref('mail')
const brandFormRef = ref()
const mailFormRef = ref()
const subjectInputRef = ref()
const templateInputRef = ref()
const brandSaving = ref(false)
const mailSaving = ref(false)
const testSending = ref(false)
const passwordConfigured = ref(false)
const selectedEventKey = ref('orderNotify')
const activeEditor = ref('template')
const publicBaseUrlPlaceholder = `${window.location.origin}（留空默认当前本站域名）`

const settingNavItems = [
  { key: 'site', target: 'site', label: '系统设置', icon: Setting },
  { key: 'creative', target: 'creative', label: '创作设置', icon: Brush, disabled: true },
  { key: 'storage', target: 'storage', label: '存储设置', icon: UploadFilled, disabled: true },
  { key: 'ai', target: 'ai', label: 'AI 配置', icon: Cpu, disabled: true },
  { key: 'mail', target: 'mail', label: '邮件配置', icon: Message },
  { key: 'switch', target: 'switch', label: '功能开关', icon: Operation, disabled: true },
  { key: 'pay', target: 'pay', label: '支付配置', icon: CreditCard, disabled: true },
  { key: 'backup', target: 'backup', label: '数据备份', icon: DataLine, disabled: true }
]

// 网站信息表单数据
const brandData = reactive({
  siteName: '',
  siteAuthor: ''
})

/**
 * 构建邮件基础壳。
 *
 * @param {string} title 标题
 * @param {string} contentHtml 正文 HTML
 * @returns {string} 完整邮件 HTML
 */
const buildMailShell = (title, contentHtml) => {
  return `<!doctype html>
<html>
<body style="margin:0;padding:24px;background:#f3f5fa;font-family:Arial,'Microsoft YaHei',sans-serif;color:#252a3a;">
  <div style="max-width:640px;margin:0 auto;overflow:hidden;border:1px solid #e2e5ef;border-radius:10px;background:#ffffff;box-shadow:0 12px 34px rgba(35,42,72,.10);">
    <div style="padding:24px 28px;color:#ffffff;background:#5968df;">
      <div style="font-size:13px;opacity:.82;">{{site_name}}</div>
      <h1 style="margin:7px 0 0;font-size:24px;line-height:1.35;">${title}</h1>
    </div>
    <div style="padding:28px;font-size:15px;line-height:1.8;">
${contentHtml}
    </div>
    <div style="padding:16px 28px;border-top:1px solid #eceef4;color:#9298a8;background:#fafbfc;font-size:12px;">
      此邮件由 {{site_name}} 系统自动发送，请勿直接回复。{{author_text}}
    </div>
  </div>
</body>
</html>`
}

/**
 * 构建订单通知默认模板。
 *
 * @returns {string} 订单通知模板
 */
const buildOrderNoticeTemplate = () => buildMailShell('新订单通知', `      <p style="margin:0 0 18px;">商户 <b>{{merchant_name}}</b> 的店铺 <b>{{shop_name}}</b> 收到一笔待确认订单。</p>
      <div style="margin:18px 0;padding:18px;border-radius:8px;color:#5968df;background:#f0f2ff;text-align:center;">
        <div style="font-size:13px;color:#70778a;">订单金额</div>
        <div style="font-size:32px;font-weight:700;letter-spacing:1px;">¥{{amount}}</div>
      </div>
      <table style="width:100%;border-collapse:collapse;font-size:14px;">
        <tr><td style="width:128px;padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;">平台订单号</td><td style="padding:10px 0;border-bottom:1px solid #eceef4;"><b>{{order_no}}</b></td></tr>
        <tr><td style="padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;">商户订单号</td><td style="padding:10px 0;border-bottom:1px solid #eceef4;">{{out_trade_no}}</td></tr>
        <tr><td style="padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;">商品名称</td><td style="padding:10px 0;border-bottom:1px solid #eceef4;">{{subject}}</td></tr>
        <tr><td style="padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;">支付类型</td><td style="padding:10px 0;border-bottom:1px solid #eceef4;">{{pay_type_text}}</td></tr>
        <tr><td style="padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;">创建时间</td><td style="padding:10px 0;border-bottom:1px solid #eceef4;">{{create_time}}</td></tr>
        <tr><td style="padding:10px 0;color:#70778a;">过期时间</td><td style="padding:10px 0;">{{expire_time}}</td></tr>
      </table>
      <div style="margin:24px 0 8px;">{{action_buttons}}</div>
      <p style="margin:14px 0 0;color:#9298a8;font-size:13px;">确认/关闭按钮为短时效单次操作链接，任意一个按钮使用后同订单其它按钮会失效。</p>`)

/**
 * 构建订单确认默认模板。
 *
 * @returns {string} 订单确认模板
 */
const buildOrderConfirmTemplate = () => buildMailShell('订单确认成功', `      <p style="margin:0 0 18px;">商户 <b>{{merchant_name}}</b> 的店铺 <b>{{shop_name}}</b> 订单已确认收款。</p>
      <div style="margin:18px 0;padding:18px;border-radius:8px;color:#1b9468;background:#eaf8f1;text-align:center;">
        <div style="font-size:13px;color:#60776c;">确认金额</div>
        <div style="font-size:32px;font-weight:700;letter-spacing:1px;">¥{{pay_amount}}</div>
      </div>
      <table style="width:100%;border-collapse:collapse;font-size:14px;">
        <tr><td style="width:128px;padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;">平台订单号</td><td style="padding:10px 0;border-bottom:1px solid #eceef4;"><b>{{order_no}}</b></td></tr>
        <tr><td style="padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;">商户订单号</td><td style="padding:10px 0;border-bottom:1px solid #eceef4;">{{out_trade_no}}</td></tr>
        <tr><td style="padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;">商品名称</td><td style="padding:10px 0;border-bottom:1px solid #eceef4;">{{subject}}</td></tr>
        <tr><td style="padding:10px 0;color:#70778a;">确认时间</td><td style="padding:10px 0;">{{pay_time}}</td></tr>
      </table>
      <p style="margin:24px 0 0;"><a href="{{order_url}}" style="display:inline-block;padding:11px 18px;border-radius:6px;color:#ffffff;background:#5968df;text-decoration:none;font-weight:700;">查看订单</a></p>`)

/**
 * 构建订单关闭默认模板。
 *
 * @returns {string} 订单关闭模板
 */
const buildOrderCloseTemplate = () => buildMailShell('订单已关闭', `      <p style="margin:0 0 18px;">商户 <b>{{merchant_name}}</b> 的店铺 <b>{{shop_name}}</b> 订单已关闭。</p>
      <div style="margin:18px 0;padding:18px;border-radius:8px;color:#d95050;background:#fff0f0;text-align:center;">
        <div style="font-size:13px;color:#8f6a6a;">订单金额</div>
        <div style="font-size:32px;font-weight:700;letter-spacing:1px;">¥{{amount}}</div>
      </div>
      <table style="width:100%;border-collapse:collapse;font-size:14px;">
        <tr><td style="width:128px;padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;">平台订单号</td><td style="padding:10px 0;border-bottom:1px solid #eceef4;"><b>{{order_no}}</b></td></tr>
        <tr><td style="padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;">商户订单号</td><td style="padding:10px 0;border-bottom:1px solid #eceef4;">{{out_trade_no}}</td></tr>
        <tr><td style="padding:10px 0;color:#70778a;border-bottom:1px solid #eceef4;">商品名称</td><td style="padding:10px 0;border-bottom:1px solid #eceef4;">{{subject}}</td></tr>
        <tr><td style="padding:10px 0;color:#70778a;">关闭时间</td><td style="padding:10px 0;">{{operation_time}}</td></tr>
      </table>
      <p style="margin:24px 0 0;"><a href="{{order_url}}" style="display:inline-block;padding:11px 18px;border-radius:6px;color:#ffffff;background:#5968df;text-decoration:none;font-weight:700;">查看订单</a></p>`)

// 邮件事件默认模板
const defaultTemplates = {
  orderNotify: {
    subject: '【{{site_name}}】新订单通知：{{order_no}}',
    template: buildOrderNoticeTemplate()
  },
  orderConfirm: {
    subject: '【{{site_name}}】订单确认成功：{{order_no}}',
    template: buildOrderConfirmTemplate()
  },
  orderClose: {
    subject: '【{{site_name}}】订单已关闭：{{order_no}}',
    template: buildOrderCloseTemplate()
  }
}

const mailEvents = [
  {
    key: 'orderNotify',
    label: '订单通知',
    description: '新订单创建后通知商户，可通过按钮占位符生成确认/关闭链接。',
    enabledKey: 'orderNotifyEnabled',
    subjectKey: 'orderNotifySubject',
    templateKey: 'orderNotifyTemplate'
  },
  {
    key: 'orderConfirm',
    label: '订单确认通知',
    description: '订单被确认收款后通知商户确认结果和回调状态。',
    enabledKey: 'orderConfirmNotifyEnabled',
    subjectKey: 'orderConfirmNotifySubject',
    templateKey: 'orderConfirmNotifyTemplate'
  },
  {
    key: 'orderClose',
    label: '订单关闭通知',
    description: '订单被关闭后通知商户关闭结果和订单信息。',
    enabledKey: 'orderCloseNotifyEnabled',
    subjectKey: 'orderCloseNotifySubject',
    templateKey: 'orderCloseNotifyTemplate'
  }
]

const placeholderGroups = [
  {
    title: '站点',
    items: [{ key: 'site_name' }, { key: 'site_author' }, { key: 'author_text' }]
  },
  {
    title: '商户与店铺',
    items: [{ key: 'merchant_name' }, { key: 'merchant_no' }, { key: 'shop_name' }, { key: 'shop_no' }]
  },
  {
    title: '订单',
    items: [
      { key: 'order_no' },
      { key: 'out_trade_no' },
      { key: 'subject' },
      { key: 'amount' },
      { key: 'pay_amount' },
      { key: 'pay_type_text' },
      { key: 'order_status' },
      { key: 'create_time' },
      { key: 'expire_time' },
      { key: 'pay_time' },
      { key: 'operation_time' },
      { key: 'client_ip' },
      { key: 'notify_url' },
      { key: 'return_url' },
      { key: 'order_url' }
    ]
  },
  {
    title: '邮件操作',
    items: [{ key: 'action_buttons' }, { key: 'confirm_button' }, { key: 'close_button' }]
  }
]

// 邮件配置表单数据
const mailData = reactive({
  mailEnabled: false,
  smtpHost: '',
  smtpPort: 465,
  smtpUsername: '',
  smtpPassword: '',
  fromEmail: '',
  fromName: '',
  sslEnabled: true,
  publicBaseUrl: '',
  actionTokenExpireMinutes: 30,
  orderNotifyEnabled: true,
  orderNotifySubject: defaultTemplates.orderNotify.subject,
  orderNotifyTemplate: defaultTemplates.orderNotify.template,
  orderConfirmNotifyEnabled: true,
  orderConfirmNotifySubject: defaultTemplates.orderConfirm.subject,
  orderConfirmNotifyTemplate: defaultTemplates.orderConfirm.template,
  orderCloseNotifyEnabled: true,
  orderCloseNotifySubject: defaultTemplates.orderClose.subject,
  orderCloseNotifyTemplate: defaultTemplates.orderClose.template
})

// 测试邮件表单数据
const testData = reactive({
  testEmail: ''
})

const sampleValues = computed(() => ({
  site_name: brandData.siteName || 'FAST 易支付',
  site_author: brandData.siteAuthor || '大熊Bigbear',
  author_text: `by ${brandData.siteAuthor || '大熊Bigbear'}`,
  merchant_name: '示例商户',
  merchant_no: 'M202608230001',
  shop_name: '示例店铺（SHOP001）',
  shop_no: 'SHOP001',
  order_no: 'FP2026082300012345',
  out_trade_no: 'ORDER20260823001',
  subject: '会员订阅套餐',
  amount: '99.00',
  pay_amount: '99.00',
  pay_type: 'alipay',
  pay_type_text: '支付宝',
  pay_method: 'page',
  order_status: '待支付',
  notify_status: '0',
  create_time: '2026-08-23 10:30:00',
  expire_time: '2026-08-23 10:45:00',
  pay_time: '2026-08-23 10:32:18',
  operation_time: '2026-08-23 10:33:00',
  operation_name: '确认收款',
  client_ip: '203.0.113.10',
  notify_url: 'https://merchant.example.com/notify',
  return_url: 'https://merchant.example.com/return',
  order_url: `${window.location.origin}/fastpay-merchant/console/order?orderNo=FP2026082300012345`
}))

const currentEvent = computed(() => mailEvents.find((item) => item.key === selectedEventKey.value) || mailEvents[0])
const previewSubject = computed(() => renderTemplate(mailData[currentEvent.value.subjectKey] || ''))
const previewHtml = computed(() => renderTemplate(mailData[currentEvent.value.templateKey] || '<p>HTML 模板预览</p>', true))
const currentTemplateLength = computed(() => String(mailData[currentEvent.value.templateKey] || '').length)

// 网站信息校验规则
const brandRules = {
  siteName: [{ required: true, message: '请输入网站名称', trigger: 'blur' }],
  siteAuthor: [{ required: true, message: '请输入网站署名', trigger: 'blur' }]
}

// 邮件启用后必填校验
const validateRequiredWhenMailEnabled = (message) => {
  return (rule, value, callback) => {
    if (mailData.mailEnabled && !String(value || '').trim()) {
      callback(new Error(message))
      return
    }
    callback()
  }
}

// 邮件配置校验规则
const mailRules = {
  smtpHost: [{ validator: validateRequiredWhenMailEnabled('请输入SMTP服务器'), trigger: 'blur' }],
  fromEmail: [
    { validator: validateRequiredWhenMailEnabled('请输入发件邮箱'), trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  smtpPort: [{ required: true, message: '请输入SMTP端口', trigger: 'blur' }],
  actionTokenExpireMinutes: [{ required: true, message: '请输入操作链接有效期', trigger: 'blur' }]
}

/**
 * 切换配置导航。
 *
 * @param {object} item 导航项
 */
const switchSettingTab = (item) => {
  if (item.disabled) {
    ElMessage.info('当前配置项暂未接入')
    return
  }
  activeTab.value = item.target
}

/**
 * 加载网站信息。
 */
const loadBrandData = async () => {
  const res = await getAdminBrandConfig()
  brandData.siteName = res.data.siteName || 'FAST 易支付'
  brandData.siteAuthor = res.data.siteAuthor || '大熊Bigbear'
  if (!mailData.fromName) {
    mailData.fromName = brandData.siteName
  }
}

/**
 * 加载邮件配置。
 */
const loadMailData = async () => {
  const res = await getMailConfig()
  const data = res.data || {}
  Object.assign(mailData, {
    mailEnabled: !!data.mailEnabled,
    smtpHost: data.smtpHost || '',
    smtpPort: data.smtpPort || 465,
    smtpUsername: data.smtpUsername || '',
    smtpPassword: '',
    fromEmail: data.fromEmail || '',
    fromName: data.fromName || brandData.siteName || 'FAST 易支付',
    sslEnabled: data.sslEnabled !== false,
    publicBaseUrl: data.publicBaseUrl || '',
    actionTokenExpireMinutes: data.actionTokenExpireMinutes || 30,
    orderNotifyEnabled: data.orderNotifyEnabled !== false,
    orderNotifySubject: data.orderNotifySubject || defaultTemplates.orderNotify.subject,
    orderNotifyTemplate: data.orderNotifyTemplate || defaultTemplates.orderNotify.template,
    orderConfirmNotifyEnabled: data.orderConfirmNotifyEnabled !== false,
    orderConfirmNotifySubject: data.orderConfirmNotifySubject || defaultTemplates.orderConfirm.subject,
    orderConfirmNotifyTemplate: data.orderConfirmNotifyTemplate || defaultTemplates.orderConfirm.template,
    orderCloseNotifyEnabled: data.orderCloseNotifyEnabled !== false,
    orderCloseNotifySubject: data.orderCloseNotifySubject || defaultTemplates.orderClose.subject,
    orderCloseNotifyTemplate: data.orderCloseNotifyTemplate || defaultTemplates.orderClose.template
  })
  passwordConfigured.value = !!data.passwordConfigured
}

/**
 * 保存网站信息。
 */
const handleSaveBrand = async () => {
  if (!brandFormRef.value) return

  await brandFormRef.value.validate(async (valid) => {
    if (!valid) return

    brandSaving.value = true
    try {
      const res = await updateBrandConfig(brandData)
      notifyBrandConfigUpdated(res.data)
      if (!mailData.fromName) {
        mailData.fromName = res.data.siteName
      }
      ElMessage.success('系统配置已保存')
    } catch (error) {
      console.error('保存系统配置失败:', error)
    } finally {
      brandSaving.value = false
    }
  })
}

/**
 * 保存邮件配置。
 */
const handleSaveMail = async () => {
  if (!mailFormRef.value) return

  await mailFormRef.value.validate(async (valid) => {
    if (!valid) return

    mailSaving.value = true
    try {
      const res = await updateMailConfig({ ...mailData })
      mailData.smtpPassword = ''
      passwordConfigured.value = !!res.data.passwordConfigured
      ElMessage.success('邮件配置已保存')
    } catch (error) {
      console.error('保存邮件配置失败:', error)
    } finally {
      mailSaving.value = false
    }
  })
}

/**
 * 发送测试邮件。
 */
const handleSendTestMail = async () => {
  if (!testData.testEmail) {
    ElMessage.warning('请输入测试邮箱')
    return
  }

  testSending.value = true
  try {
    await sendTestMail(testData)
    ElMessage.success('测试邮件已发送')
  } catch (error) {
    console.error('发送测试邮件失败:', error)
  } finally {
    testSending.value = false
  }
}

/**
 * 恢复当前邮件事件的默认主题和模板。
 */
const resetCurrentTemplate = () => {
  const event = currentEvent.value
  const defaults = defaultTemplates[event.key]
  mailData[event.subjectKey] = defaults.subject
  mailData[event.templateKey] = defaults.template
  ElMessage.success('已恢复当前事件默认模板')
}

/**
 * 插入占位符到当前编辑字段。
 *
 * @param {string} key 占位符键名
 */
const insertPlaceholder = async (key) => {
  const insertText = `{{${key}}}`
  const event = currentEvent.value
  const fieldKey = activeEditor.value === 'subject' ? event.subjectKey : event.templateKey
  const inputEl = activeEditor.value === 'subject'
    ? subjectInputRef.value?.input
    : templateInputRef.value?.textarea
  const oldValue = mailData[fieldKey] || ''

  if (inputEl && typeof inputEl.selectionStart === 'number') {
    const start = inputEl.selectionStart
    const end = inputEl.selectionEnd
    mailData[fieldKey] = oldValue.slice(0, start) + insertText + oldValue.slice(end)
    await nextTick()
    inputEl.focus()
    inputEl.setSelectionRange(start + insertText.length, start + insertText.length)
    return
  }

  mailData[fieldKey] = oldValue ? `${oldValue}${insertText}` : insertText
}

/**
 * 渲染邮件模板预览。
 *
 * @param {string} template 原始模板
 * @param {boolean} htmlMode 是否为 HTML 模板
 * @returns {string} 渲染后的内容
 */
const renderTemplate = (template, htmlMode = false) => {
  let result = template || ''
  Object.entries(sampleValues.value).forEach(([key, value]) => {
    result = replaceAll(result, `{{${key}}}`, value)
  })
  if (htmlMode) {
    result = replaceAll(result, '{{action_buttons}}', sampleActionButtons())
    result = replaceAll(result, '{{confirm_button}}', sampleConfirmButton())
    result = replaceAll(result, '{{close_button}}', sampleCloseButton())
  }
  return result
}

/**
 * 全量替换字符串。
 *
 * @param {string} source 原始文本
 * @param {string} search 查询文本
 * @param {string} replacement 替换文本
 * @returns {string} 替换结果
 */
const replaceAll = (source, search, replacement) => {
  return String(source).split(search).join(replacement)
}

/**
 * 格式化占位符显示文本。
 *
 * @param {string} key 占位符键名
 * @returns {string} 占位符文本
 */
const formatPlaceholder = (key) => {
  return `{{${key}}}`
}

/**
 * 构造预览用确认按钮。
 *
 * @returns {string} 确认按钮 HTML
 */
const sampleConfirmButton = () => {
  return '<a href="#" style="display:inline-block;margin-right:10px;padding:11px 18px;border-radius:6px;background:#5968df;color:#fff;text-decoration:none;font-weight:700;">确认收款</a>'
}

/**
 * 构造预览用关闭按钮。
 *
 * @returns {string} 关闭按钮 HTML
 */
const sampleCloseButton = () => {
  return '<a href="#" style="display:inline-block;padding:11px 18px;border-radius:6px;background:#d95050;color:#fff;text-decoration:none;font-weight:700;">关闭订单</a>'
}

/**
 * 构造预览用组合按钮。
 *
 * @returns {string} 组合按钮 HTML
 */
const sampleActionButtons = () => {
  return sampleConfirmButton() + sampleCloseButton()
}

onMounted(async () => {
  await loadBrandData()
  await loadMailData()
})
</script>

<style scoped lang="scss">
.dark-config-page {
  min-height: calc(100vh - 120px);
  padding: 14px;
  color: #dce5ff;
  background: #0d1424;
}

.settings-nav {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
  padding: 10px 18px;
  border: 1px solid #26324c;
  border-radius: 8px;
  background: #111a2c;
}

.settings-nav-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 48px;
  border: 1px solid transparent;
  border-radius: 7px;
  color: #aeb9d8;
  background: transparent;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;

  &.active {
    color: #f2f5ff;
    border-color: #34436b;
    background: #252f50;
    box-shadow: inset 0 -2px 0 #7868ff;
  }

  &.disabled {
    cursor: default;
    opacity: .75;
  }
}

.nav-icon,
.card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  color: #dfe5ff;
  background: #25305a;
  font-size: 16px;
  font-weight: 800;

  .el-icon {
    font-size: 18px;
  }
}

.config-shell {
  overflow: hidden;
  border: 1px solid #26324c;
  border-radius: 8px;
  background: #111827;
}

.mail-titlebar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 22px;
  border-bottom: 1px solid #26324c;
  background: #121b2d;
}

.title-left {
  display: flex;
  align-items: center;
  gap: 12px;

  h2 {
    margin: 0;
    color: #f5f7ff;
    font-size: 22px;
  }

  p {
    margin: 4px 0 0;
    color: #8795b8;
    font-size: 13px;
  }
}

.step-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 7px;
  color: #fff;
  background: #665ff0;
  font-size: 18px;
  font-weight: 800;
}

.permission-pill {
  color: #7c8cff;
  font-size: 13px;
}

.config-form {
  padding: 22px;
}

.mail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 352px;
  gap: 18px;
  align-items: start;
}

.mail-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 18px;
}

.dark-card,
.status-card {
  border: 1px solid #26324c;
  border-radius: 8px;
  background: #121b2d;
}

.dark-card {
  padding: 24px 26px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 24px;
  padding-bottom: 18px;
  border-bottom: 1px solid #2a3652;

  h3 {
    margin: 0;
    color: #f5f7ff;
    font-size: 20px;
  }

  p {
    margin: 4px 0 0;
    color: #8795b8;
    font-size: 13px;
  }

  :deep(.el-switch) {
    margin-left: auto;
  }

  &.compact {
    margin-bottom: 18px;
    padding-bottom: 0;
    border-bottom: 0;
  }
}

.form-grid,
.site-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

.bottom-grid {
  margin-top: 18px;
}

.ssl-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 12px;
  padding: 18px 0;
  border-top: 1px solid #2a3652;
  border-bottom: 1px solid #2a3652;

  strong {
    color: #f5f7ff;
    font-size: 15px;
  }

  p {
    margin: 5px 0 0;
    color: #8795b8;
    font-size: 13px;
  }
}

.card-actions,
.template-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.test-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 170px;
  gap: 12px;
}

.event-selector-row {
  display: grid;
  grid-template-columns: 36% minmax(0, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}

.event-select-field label {
  display: block;
  margin-bottom: 8px;
  color: #cbd6f4;
  font-size: 13px;
  font-weight: 700;
}

.event-select {
  width: 100%;
}

.event-desc-card {
  padding: 12px 16px;
  border-left: 3px solid #7466ff;
  background: #1a2438;

  p {
    margin: 6px 0 0;
    color: #8795b8;
    font-size: 13px;
  }
}

.event-desc-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #f5f7ff;
  font-weight: 700;
}

.template-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 47%);
  gap: 16px;
}

.template-editor-panel,
.preview-panel {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #2a3652;
  border-radius: 8px;
  background: #0e1626;
}

.panel-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 16px;
  border-bottom: 1px solid #2a3652;
  color: #cfd8f4;
  background: #1a2438;
  font-weight: 800;

  em {
    overflow: hidden;
    color: #8795b8;
    font-size: 12px;
    font-style: normal;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.bar-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.html-editor {
  :deep(.el-textarea__inner) {
    min-height: 520px !important;
    border: 0;
    border-radius: 0;
    box-shadow: none;
    background: #0e1626;
    color: #dce5ff;
    font-family: Consolas, 'Courier New', monospace;
    line-height: 1.7;
  }
}

.placeholder-strip {
  padding: 14px 16px;
  border-top: 1px solid #2a3652;

  strong {
    color: #f5f7ff;
  }

  p {
    margin: 4px 0 12px;
    color: #8795b8;
    font-size: 12px;
  }
}

.placeholder-group {
  margin-top: 10px;
}

.group-title {
  display: inline-block;
  min-width: 72px;
  color: #8795b8;
  font-size: 12px;
  font-weight: 700;
}

.placeholder-chip {
  margin: 0 6px 8px 0;
  padding: 7px 10px;
  border: 1px solid #354369;
  border-radius: 6px;
  color: #dce5ff;
  background: #25305a;
  font-size: 12px;
  cursor: pointer;

  &:hover {
    border-color: #7466ff;
    color: #fff;
    background: #34306f;
  }
}

.preview-frame {
  display: block;
  width: 100%;
  min-height: 662px;
  border: 0;
  background: #f3f5fa;
}

.mail-side {
  position: sticky;
  top: 16px;
}

.status-card {
  padding: 26px 24px;

  h3 {
    margin: 16px 0 4px;
    color: #f5f7ff;
    font-size: 19px;
  }

  > p {
    margin: 0 0 18px;
    color: #8795b8;
  }
}

.status-dot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 74px;
  height: 74px;
  border-radius: 50%;
  color: #ef6b6b;
  background: #ffecec;
  font-size: 28px;
  font-weight: 900;

  &.enabled {
    color: #169f73;
    background: #e8fff5;
  }
}

.status-list {
  border-top: 1px solid #2a3652;

  div {
    display: flex;
    justify-content: space-between;
    gap: 14px;
    padding: 13px 0;
    border-bottom: 1px solid #2a3652;
  }

  span {
    color: #8795b8;
  }

  b {
    max-width: 180px;
    overflow: hidden;
    color: #dce5ff;
    text-align: right;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.brand-preview {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 8px;
  padding: 18px;
  border: 1px solid #2a3652;
  border-radius: 8px;
  background: #0e1626;
}

.preview-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #5968df, #7b5ff2);
  font-family: STKaiti, KaiTi, SimSun, serif;
  font-size: 24px;
  font-weight: 700;
}

.preview-name {
  color: #f5f7ff;
  font-weight: 700;
}

.preview-author {
  margin-top: 4px;
  color: #8795b8;
  font-size: 13px;
}

.form-tip {
  margin-top: 7px;
  color: #8795b8;
  font-size: 12px;
  line-height: 1.5;
}

.full-number {
  width: 100%;
}

:deep(.el-form-item__label) {
  color: #cbd6f4;
  font-weight: 800;
}

:deep(.el-input__wrapper),
:deep(.el-input-number__decrease),
:deep(.el-input-number__increase),
:deep(.el-select__wrapper) {
  border: 1px solid #33405f;
  border-radius: 6px;
  box-shadow: none;
  background: #172137;
  color: #dce5ff;
}

:deep(.el-input__inner),
:deep(.el-select__placeholder),
:deep(.el-select__selected-item) {
  color: #dce5ff;
  font-weight: 700;
}

:deep(.el-input__inner::placeholder),
:deep(.el-textarea__inner::placeholder) {
  color: #63708e;
}

:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  color: #9ba8cc;
  background: #202b44;
}

:deep(.el-button--primary) {
  border-color: #6d63f4;
  background: linear-gradient(135deg, #5669f2, #7b5ff2);
}

:deep(.el-button:not(.el-button--primary)) {
  border-color: #33405f;
  color: #cbd6f4;
  background: #1b2540;
}

@media (max-width: 1420px) {
  .settings-nav {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .mail-layout,
  .template-grid {
    grid-template-columns: 1fr;
  }

  .mail-side {
    position: static;
  }
}

@media (max-width: 768px) {
  .dark-config-page,
  .config-form {
    padding: 12px;
  }

  .settings-nav,
  .form-grid,
  .site-grid,
  .event-selector-row,
  .test-row {
    grid-template-columns: 1fr;
  }

  .section-heading {
    flex-wrap: wrap;
  }
}
</style>
