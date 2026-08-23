<template>
  <div class="system-config-page">
    <div class="page-header">
      <h2>系统配置</h2>
      <p>管理网站展示信息、SMTP 发信服务和订单邮件模板事件</p>
    </div>

    <div class="page-card config-card">
      <div class="config-tabs">
        <div
          class="config-tab"
          :class="{ active: activeTab === 'site' }"
          @click="activeTab = 'site'"
        >
          网站信息
        </div>
        <div
          class="config-tab"
          :class="{ active: activeTab === 'mail' }"
          @click="activeTab = 'mail'"
        >
          邮件配置
        </div>
      </div>

      <el-form
        v-if="activeTab === 'site'"
        ref="brandFormRef"
        :model="brandData"
        :rules="brandRules"
        label-position="top"
        class="config-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="网站名称" prop="siteName">
              <el-input v-model="brandData.siteName" placeholder="请输入网站名称" maxlength="50" show-word-limit />
              <div class="form-tip">用于后台、商户端、浏览器标题等位置展示。</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="网站署名" prop="siteAuthor">
              <el-input v-model="brandData.siteAuthor" placeholder="请输入署名，例如 xxx" maxlength="50" show-word-limit />
              <div class="form-tip">侧边栏和页脚会显示为：by {{ brandData.siteAuthor || 'xxx' }}</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="展示预览">
              <div class="brand-preview">
                <div class="preview-logo">易</div>
                <div>
                  <div class="preview-name">{{ brandData.siteName || '网站名称' }}</div>
                  <div class="preview-author">by {{ brandData.siteAuthor || 'xxx' }}</div>
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button @click="loadBrandData">重置</el-button>
          <el-button type="primary" :loading="brandSaving" @click="handleSaveBrand">
            保存配置
          </el-button>
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
        <div class="mail-top-grid">
          <div class="config-section smtp-card">
            <div class="section-header">
              <div>
                <div class="section-kicker">SMTP</div>
                <h3>邮件服务</h3>
                <p>用于订单通知、确认通知和关闭通知。</p>
              </div>
              <el-switch v-model="mailData.mailEnabled" active-text="启用" inactive-text="停用" />
            </div>

            <el-row :gutter="18">
              <el-col :xs="24" :sm="12">
                <el-form-item label="SMTP服务器" prop="smtpHost">
                  <el-input v-model="mailData.smtpHost" placeholder="例如 smtp.qq.com" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="SMTP端口" prop="smtpPort">
                  <el-input-number v-model="mailData.smtpPort" :min="1" :max="65535" class="full-number" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="SMTP登录账号" prop="smtpUsername">
                  <el-input v-model="mailData.smtpUsername" placeholder="通常为发件邮箱或邮箱账号" />
                  <div class="form-tip">只用于 SMTP 服务身份认证。</div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="SMTP密码" prop="smtpPassword">
                  <el-input
                    v-model="mailData.smtpPassword"
                    type="password"
                    show-password
                    :placeholder="passwordConfigured ? '留空则保持当前密码' : '请输入SMTP授权码或密码'"
                  />
                  <div v-if="passwordConfigured" class="form-tip">当前已保存密码，重新输入才会覆盖。</div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="发件邮箱" prop="fromEmail">
                  <el-input v-model="mailData.fromEmail" placeholder="例如 notice@example.com" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="发件名称" prop="fromName">
                  <el-input v-model="mailData.fromName" placeholder="例如 FAST 易支付" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="使用 SSL" prop="sslEnabled">
                  <el-switch v-model="mailData.sslEnabled" active-text="SSL" inactive-text="普通" />
                  <div class="form-tip">常见 SSL 端口为 465，STARTTLS 通常使用 587。</div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
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
              </el-col>
              <el-col :span="24">
                <el-form-item label="平台外部访问地址" prop="publicBaseUrl">
                  <el-input v-model="mailData.publicBaseUrl" :placeholder="publicBaseUrlPlaceholder" />
                  <div class="form-tip">可留空。留空时按当前访问域名生成邮件链接；若使用反向代理，请保证 Host、X-Forwarded-Proto 透传正确。</div>
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <div class="mail-side">
            <div class="status-card">
              <div class="status-dot" :class="{ enabled: mailData.mailEnabled }">
                {{ mailData.mailEnabled ? '✓' : '!' }}
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
                  <span>邮件事件</span>
                  <b>{{ enabledEventCount }} / {{ mailEvents.length }}</b>
                </div>
              </div>
            </div>

            <div class="test-panel">
              <div class="test-title">发送测试邮件</div>
              <p>使用上方当前配置验证 SMTP，保存前也可以测试。</p>
              <div class="test-row">
                <el-input v-model="testData.testEmail" placeholder="请输入测试收件邮箱" />
                <el-button :loading="testSending" @click="handleSendTestMail">发送测试</el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="config-section event-section">
          <div class="section-header compact">
            <div>
              <div class="section-kicker">Template Events</div>
              <h3>邮件事件模板</h3>
              <p>每个事件独立配置邮件主题和 HTML 模板，右侧实时预览渲染效果。</p>
            </div>
            <el-button @click="resetCurrentTemplate">恢复当前事件默认模板</el-button>
          </div>

          <div class="event-workbench">
            <div class="event-sidebar">
              <button
                v-for="item in mailEvents"
                :key="item.key"
                type="button"
                class="event-tab-button"
                :class="{ active: selectedEventKey === item.key }"
                @click="selectedEventKey = item.key"
              >
                <span>{{ item.label }}</span>
                <small>{{ item.description }}</small>
              </button>
            </div>

            <div class="event-editor">
              <div class="event-editor-head">
                <div>
                  <h4>{{ currentEvent.label }}</h4>
                  <p>{{ currentEvent.description }}</p>
                </div>
                <el-switch v-model="mailData[currentEvent.enabledKey]" active-text="启用" inactive-text="停用" />
              </div>

              <el-form-item label="邮件主题">
                <el-input
                  ref="subjectInputRef"
                  v-model="mailData[currentEvent.subjectKey]"
                  placeholder="请输入邮件主题，可使用占位符"
                  @focus="activeEditor = 'subject'"
                />
              </el-form-item>

              <el-form-item label="HTML模板">
                <el-input
                  ref="templateInputRef"
                  v-model="mailData[currentEvent.templateKey]"
                  type="textarea"
                  :autosize="{ minRows: 14, maxRows: 22 }"
                  placeholder="请输入 HTML 模板，可使用占位符"
                  @focus="activeEditor = 'template'"
                />
                <div class="form-tip">
                  订单通知模板中写入 {{ actionPlaceholderText }}、{{ confirmPlaceholderText }} 或 {{ closePlaceholderText }} 时，系统会自动生成单次确认/关闭链接。
                </div>
              </el-form-item>
            </div>

            <div class="preview-pane">
              <div class="placeholder-panel">
                <div class="placeholder-head">
                  <b>占位符</b>
                  <p>点击插入到当前{{ activeEditor === 'subject' ? '邮件主题' : 'HTML模板' }}</p>
                </div>
                <div
                  v-for="group in placeholderGroups"
                  :key="group.title"
                  class="placeholder-group"
                >
                  <div class="placeholder-group-title">{{ group.title }}</div>
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

              <div class="email-preview">
                <div class="preview-bar">
                  <span>实时预览</span>
                  <small>{{ currentEvent.label }}</small>
                </div>
                <div class="preview-subject">{{ previewSubject || '邮件主题预览' }}</div>
                <div class="preview-body" v-html="previewHtml"></div>
              </div>
            </div>
          </div>
        </div>

        <div class="form-actions sticky-actions">
          <el-button @click="loadMailData">重置</el-button>
          <el-button type="primary" :loading="mailSaving" @click="handleSaveMail">
            保存邮件配置
          </el-button>
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
  getAdminBrandConfig,
  getMailConfig,
  sendTestMail,
  updateBrandConfig,
  updateMailConfig
} from '@/api'
import { notifyBrandConfigUpdated } from '@/utils/brand'

const activeTab = ref('site')
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
const actionPlaceholderText = '{{action_buttons}}'
const confirmPlaceholderText = '{{confirm_button}}'
const closePlaceholderText = '{{close_button}}'

// 网站信息表单数据
const brandData = reactive({
  siteName: '',
  siteAuthor: ''
})

// 邮件事件默认模板
const defaultTemplates = {
  orderNotify: {
    subject: '【{{site_name}}】新订单通知：{{order_no}}',
    template: `<div style="font-family:Arial,'Microsoft YaHei',sans-serif;line-height:1.7;color:#303133;max-width:760px;">
  <h2 style="margin:0 0 8px;">{{site_name}} 新订单通知</h2>
  <p style="margin:0 0 16px;color:#606266;">商户 {{merchant_name}} 的店铺 {{shop_name}} 收到一笔待确认订单。</p>
  <table style="border-collapse:collapse;width:100%;font-size:14px;">
    <tr><td style="width:130px;padding:9px 12px;border:1px solid #ebeef5;background:#f8fafc;color:#606266;">平台订单号</td><td style="padding:9px 12px;border:1px solid #ebeef5;">{{order_no}}</td></tr>
    <tr><td style="padding:9px 12px;border:1px solid #ebeef5;background:#f8fafc;color:#606266;">商户订单号</td><td style="padding:9px 12px;border:1px solid #ebeef5;">{{out_trade_no}}</td></tr>
    <tr><td style="padding:9px 12px;border:1px solid #ebeef5;background:#f8fafc;color:#606266;">店铺</td><td style="padding:9px 12px;border:1px solid #ebeef5;">{{shop_name}}</td></tr>
    <tr><td style="padding:9px 12px;border:1px solid #ebeef5;background:#f8fafc;color:#606266;">商品名称</td><td style="padding:9px 12px;border:1px solid #ebeef5;">{{subject}}</td></tr>
    <tr><td style="padding:9px 12px;border:1px solid #ebeef5;background:#f8fafc;color:#606266;">订单金额</td><td style="padding:9px 12px;border:1px solid #ebeef5;">¥{{amount}}</td></tr>
    <tr><td style="padding:9px 12px;border:1px solid #ebeef5;background:#f8fafc;color:#606266;">支付类型</td><td style="padding:9px 12px;border:1px solid #ebeef5;">{{pay_type_text}}</td></tr>
    <tr><td style="padding:9px 12px;border:1px solid #ebeef5;background:#f8fafc;color:#606266;">创建时间</td><td style="padding:9px 12px;border:1px solid #ebeef5;">{{create_time}}</td></tr>
  </table>
  <div style="margin:22px 0;">{{action_buttons}}</div>
  <p style="margin:0;color:#909399;font-size:13px;">确认/关闭按钮为短时效单次操作链接，任意一个按钮使用后同订单其它按钮会失效。</p>
  <p style="margin:12px 0 0;color:#909399;font-size:13px;">{{author_text}}</p>
</div>`
  },
  orderConfirm: {
    subject: '【{{site_name}}】订单确认成功：{{order_no}}',
    template: `<div style="font-family:Arial,'Microsoft YaHei',sans-serif;line-height:1.7;color:#303133;max-width:760px;">
  <h2 style="margin:0 0 8px;">订单确认成功</h2>
  <p style="margin:0 0 16px;color:#606266;">商户 {{merchant_name}} 的店铺 {{shop_name}} 订单已确认收款。</p>
  <p>平台订单号：<b>{{order_no}}</b></p>
  <p>商户订单号：{{out_trade_no}}</p>
  <p>商品名称：{{subject}}</p>
  <p>确认金额：<b>¥{{pay_amount}}</b></p>
  <p>确认时间：{{pay_time}}</p>
  <p><a href="{{order_url}}" style="display:inline-block;padding:10px 16px;border-radius:4px;background:#409eff;color:#fff;text-decoration:none;">查看订单</a></p>
  <p style="margin:12px 0 0;color:#909399;font-size:13px;">{{author_text}}</p>
</div>`
  },
  orderClose: {
    subject: '【{{site_name}}】订单已关闭：{{order_no}}',
    template: `<div style="font-family:Arial,'Microsoft YaHei',sans-serif;line-height:1.7;color:#303133;max-width:760px;">
  <h2 style="margin:0 0 8px;">订单已关闭</h2>
  <p style="margin:0 0 16px;color:#606266;">商户 {{merchant_name}} 的店铺 {{shop_name}} 订单已关闭。</p>
  <p>平台订单号：<b>{{order_no}}</b></p>
  <p>商户订单号：{{out_trade_no}}</p>
  <p>商品名称：{{subject}}</p>
  <p>订单金额：<b>¥{{amount}}</b></p>
  <p>关闭时间：{{operation_time}}</p>
  <p><a href="{{order_url}}" style="display:inline-block;padding:10px 16px;border-radius:4px;background:#409eff;color:#fff;text-decoration:none;">查看订单</a></p>
  <p style="margin:12px 0 0;color:#909399;font-size:13px;">{{author_text}}</p>
</div>`
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
const enabledEventCount = computed(() => mailEvents.filter((item) => mailData[item.enabledKey]).length)
const previewSubject = computed(() => renderTemplate(mailData[currentEvent.value.subjectKey] || ''))
const previewHtml = computed(() => renderTemplate(mailData[currentEvent.value.templateKey] || '<p>HTML 模板预览</p>', true))

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
  return '<a href="#" style="display:inline-block;margin-right:10px;padding:10px 16px;border-radius:4px;background:#67c23a;color:#fff;text-decoration:none;">确认收款</a>'
}

/**
 * 构造预览用关闭按钮。
 *
 * @returns {string} 关闭按钮 HTML
 */
const sampleCloseButton = () => {
  return '<a href="#" style="display:inline-block;padding:10px 16px;border-radius:4px;background:#f56c6c;color:#fff;text-decoration:none;">关闭订单</a>'
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
.system-config-page {
  .page-header {
    margin-bottom: 20px;

    h2 {
      margin: 0 0 8px;
      font-size: 24px;
      color: #303133;
    }

    p {
      margin: 0;
      color: #909399;
    }
  }
}

.config-card {
  padding: 0;
}

.config-tabs {
  display: flex;
  gap: 28px;
  padding: 0 24px;
  border-bottom: 1px solid #ebeef5;
}

.config-tab {
  padding: 18px 0 14px;
  color: #606266;
  cursor: pointer;

  &.active {
    color: #409eff;
    border-bottom: 2px solid #409eff;
  }
}

.config-form {
  padding: 24px;
}

.mail-config-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.mail-top-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 292px;
  gap: 16px;
  align-items: start;
}

.config-section,
.status-card,
.test-panel {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.config-section {
  padding: 20px 22px;
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;

  h3 {
    margin: 2px 0 4px;
    font-size: 18px;
    color: #303133;
  }

  p {
    margin: 0;
    color: #909399;
    font-size: 13px;
  }

  &.compact {
    align-items: center;
  }
}

.section-kicker {
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.mail-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-card {
  padding: 20px;

  h3 {
    margin: 12px 0 4px;
    color: #303133;
    font-size: 16px;
  }

  p {
    margin: 0 0 16px;
    color: #909399;
    font-size: 13px;
  }
}

.status-dot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  color: #fff;
  background: #f56c6c;
  font-weight: 700;

  &.enabled {
    background: #67c23a;
  }
}

.status-list {
  display: flex;
  flex-direction: column;
  border-top: 1px solid #ebeef5;

  div {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #ebeef5;
    font-size: 13px;
  }

  span {
    color: #909399;
  }

  b {
    max-width: 150px;
    overflow: hidden;
    color: #303133;
    text-align: right;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.test-panel {
  padding: 16px;
  background: #f5faff;
  border-color: #c6e2ff;

  p {
    margin: 0 0 12px;
    color: #909399;
    font-size: 12px;
  }
}

.test-title {
  margin-bottom: 4px;
  font-weight: 600;
  color: #303133;
}

.test-row {
  display: flex;
  gap: 10px;
}

.event-workbench {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 360px;
  gap: 16px;
}

.event-sidebar {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.event-tab-button {
  width: 100%;
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #f8fafc;
  color: #606266;
  text-align: left;
  cursor: pointer;

  span {
    display: block;
    margin-bottom: 6px;
    color: #303133;
    font-weight: 600;
  }

  small {
    color: #909399;
    line-height: 1.5;
  }

  &.active {
    border-color: #409eff;
    background: #ecf5ff;

    span {
      color: #409eff;
    }
  }
}

.event-editor {
  min-width: 0;
}

.event-editor-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;

  h4 {
    margin: 0 0 4px;
    color: #303133;
    font-size: 16px;
  }

  p {
    margin: 0;
    color: #909399;
    font-size: 13px;
    line-height: 1.5;
  }
}

.preview-pane {
  min-width: 0;
}

.placeholder-panel,
.email-preview {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.placeholder-panel {
  margin-bottom: 14px;
  padding: 14px;
}

.placeholder-head {
  margin-bottom: 10px;

  p {
    margin: 4px 0 0;
    color: #909399;
    font-size: 12px;
  }
}

.placeholder-group {
  margin-top: 12px;
}

.placeholder-group-title {
  margin-bottom: 8px;
  color: #606266;
  font-size: 12px;
  font-weight: 600;
}

.placeholder-chip {
  margin: 0 6px 6px 0;
  padding: 6px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #f8fafc;
  color: #606266;
  font-size: 12px;
  cursor: pointer;

  &:hover {
    color: #409eff;
    border-color: #409eff;
    background: #ecf5ff;
  }
}

.email-preview {
  overflow: hidden;
}

.preview-bar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid #ebeef5;
  background: #f8fafc;
  color: #606266;
  font-size: 13px;

  small {
    color: #909399;
  }
}

.preview-subject {
  padding: 12px 14px;
  border-bottom: 1px solid #ebeef5;
  color: #303133;
  font-weight: 600;
  word-break: break-word;
}

.preview-body {
  min-height: 280px;
  max-height: 460px;
  padding: 16px;
  overflow: auto;
  background: #fff;
  color: #303133;
  word-break: break-word;
}

.form-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.brand-preview {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 72px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #f8fafc;
}

.preview-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #93c5fd 0%, #3b82f6 100%);
  font-family: STKaiti, KaiTi, SimSun, serif;
  font-size: 22px;
  font-weight: 700;
}

.preview-name {
  font-weight: 600;
  color: #303133;
}

.preview-author {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.full-number {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}

.sticky-actions {
  position: sticky;
  bottom: 0;
  z-index: 2;
  padding: 16px 0 0;
  background: #fff;
}

@media (max-width: 1320px) {
  .mail-top-grid,
  .event-workbench {
    grid-template-columns: 1fr;
  }

  .event-sidebar {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .config-form {
    padding: 16px;
  }

  .event-sidebar {
    grid-template-columns: 1fr;
  }

  .section-header,
  .event-editor-head,
  .test-row {
    flex-direction: column;
  }
}
</style>
