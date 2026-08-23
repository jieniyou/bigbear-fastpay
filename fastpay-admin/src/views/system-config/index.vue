<template>
  <div class="system-config-page">
    <div class="page-header">
      <h2>系统配置</h2>
      <p>管理网站展示信息、邮件服务和订单通知事件</p>
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
        class="config-form"
      >
        <div class="section-title">SMTP 发信配置</div>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="启用邮件服务" prop="mailEnabled">
              <el-switch v-model="mailData.mailEnabled" active-text="启用" inactive-text="停用" />
              <div class="form-tip">停用后不会向商户发送订单邮件。</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="使用 SSL" prop="sslEnabled">
              <el-switch v-model="mailData.sslEnabled" active-text="SSL" inactive-text="普通" />
              <div class="form-tip">常见 SSL 端口为 465。</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="操作链接有效期" prop="actionTokenExpireMinutes">
              <el-input-number v-model="mailData.actionTokenExpireMinutes" :min="1" :max="1440" :step="5" />
              <div class="form-tip">单位：分钟，用于邮件按钮的单点操作链接。</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="SMTP服务器" prop="smtpHost">
              <el-input v-model="mailData.smtpHost" placeholder="例如 smtp.example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SMTP端口" prop="smtpPort">
              <el-input-number v-model="mailData.smtpPort" :min="1" :max="65535" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="SMTP登录账号" prop="smtpUsername">
              <el-input v-model="mailData.smtpUsername" placeholder="通常为发件邮箱或邮箱账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
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
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发件邮箱" prop="fromEmail">
              <el-input v-model="mailData.fromEmail" placeholder="例如 notice@example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发件名称" prop="fromName">
              <el-input v-model="mailData.fromName" placeholder="例如 FAST 易支付" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="平台外部访问地址" prop="publicBaseUrl">
              <el-input v-model="mailData.publicBaseUrl" :placeholder="publicBaseUrlPlaceholder" />
              <div class="form-tip">用于邮件中的订单列表和确认/关闭按钮，建议填写外部可访问域名，例如 https://pay.example.com。</div>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-title">邮件事件</div>
        <div class="event-list">
          <div class="event-item">
            <div>
              <div class="event-name">订单通知</div>
              <div class="event-desc">新订单创建后通知商户：店铺、订单号、商品、金额、支付类型和订单列表入口。</div>
            </div>
            <el-switch v-model="mailData.orderNotifyEnabled" />
          </div>
          <div class="event-item">
            <div>
              <div class="event-name">订单通知（带操作）</div>
              <div class="event-desc">邮件内增加“确认收款”和“关闭订单”按钮，链接带签名和有效期。</div>
            </div>
            <el-switch v-model="mailData.orderActionNotifyEnabled" />
          </div>
        </div>

        <div class="test-panel">
          <div class="test-title">发送测试邮件</div>
          <div class="test-row">
            <el-input v-model="testData.testEmail" placeholder="请输入测试收件邮箱" />
            <el-button :loading="testSending" @click="handleSendTestMail">发送测试</el-button>
          </div>
        </div>

        <div class="form-actions">
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
 * 系统配置页面
 * 用于维护网站信息、邮件发信配置和订单通知事件。
 */
import { onMounted, reactive, ref } from 'vue'
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
const brandSaving = ref(false)
const mailSaving = ref(false)
const testSending = ref(false)
const passwordConfigured = ref(false)
const publicBaseUrlPlaceholder = window.location.origin

// 网站信息表单数据
const brandData = reactive({
  siteName: '',
  siteAuthor: ''
})

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
  orderNotifyEnabled: true,
  orderActionNotifyEnabled: false,
  actionTokenExpireMinutes: 30
})

// 测试邮件表单数据
const testData = reactive({
  testEmail: ''
})

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

// 加载网站信息
const loadBrandData = async () => {
  const res = await getAdminBrandConfig()
  brandData.siteName = res.data.siteName || 'FAST 易支付'
  brandData.siteAuthor = res.data.siteAuthor || '大熊Bigbear'
  if (!mailData.fromName) {
    mailData.fromName = brandData.siteName
  }
}

// 加载邮件配置
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
    orderNotifyEnabled: data.orderNotifyEnabled !== false,
    orderActionNotifyEnabled: !!data.orderActionNotifyEnabled,
    actionTokenExpireMinutes: data.actionTokenExpireMinutes || 30
  })
  passwordConfigured.value = !!data.passwordConfigured
}

// 保存网站信息
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

// 保存邮件配置
const handleSaveMail = async () => {
  if (!mailFormRef.value) return

  await mailFormRef.value.validate(async (valid) => {
    if (!valid) return

    mailSaving.value = true
    try {
      const res = await updateMailConfig(mailData)
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

// 发送测试邮件
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

.section-title {
  margin: 4px 0 18px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
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
  width: 40px;
  height: 40px;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #93c5fd 0%, #3b82f6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
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

.event-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 22px;
}

.event-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #f8fafc;
}

.event-name {
  font-weight: 600;
  color: #303133;
}

.event-desc {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.test-panel {
  margin: 4px 0 22px;
  padding: 16px;
  border: 1px dashed #c6e2ff;
  border-radius: 8px;
  background: #f5faff;
}

.test-title {
  margin-bottom: 10px;
  font-weight: 600;
  color: #303133;
}

.test-row {
  display: flex;
  gap: 12px;

  .el-input {
    max-width: 360px;
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}
</style>
