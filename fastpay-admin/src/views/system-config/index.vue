<template>
  <div class="system-config-page">
    <div class="page-header">
      <h2>系统配置</h2>
      <p>管理网站名称、署名等全局展示信息</p>
    </div>

    <div class="page-card config-card">
      <div class="config-tabs">
        <div class="config-tab active">网站信息</div>
      </div>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-position="top"
        class="config-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="网站名称" prop="siteName">
              <el-input v-model="formData.siteName" placeholder="请输入网站名称" maxlength="50" show-word-limit />
              <div class="form-tip">用于后台、商户端、浏览器标题等位置展示。</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="网站署名" prop="siteAuthor">
              <el-input v-model="formData.siteAuthor" placeholder="请输入署名，例如 xxx" maxlength="50" show-word-limit />
              <div class="form-tip">侧边栏和页脚会显示为：by {{ formData.siteAuthor || 'xxx' }}</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="展示预览">
              <div class="brand-preview">
                <div class="preview-logo">易</div>
                <div>
                  <div class="preview-name">{{ formData.siteName || '网站名称' }}</div>
                  <div class="preview-author">by {{ formData.siteAuthor || 'xxx' }}</div>
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button @click="loadData">重置</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">
            保存配置
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
/**
 * 系统配置页面
 * 用于维护网站名称和站点署名。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminBrandConfig, updateBrandConfig } from '@/api'
import { notifyBrandConfigUpdated } from '@/utils/brand'

const formRef = ref()
const saving = ref(false)

// 表单数据
const formData = reactive({
  siteName: '',
  siteAuthor: ''
})

// 表单校验规则
const rules = {
  siteName: [{ required: true, message: '请输入网站名称', trigger: 'blur' }],
  siteAuthor: [{ required: true, message: '请输入网站署名', trigger: 'blur' }]
}

// 加载配置
const loadData = async () => {
  const res = await getAdminBrandConfig()
  formData.siteName = res.data.siteName || 'FAST 易支付'
  formData.siteAuthor = res.data.siteAuthor || '大熊Bigbear'
}

// 保存配置
const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      const res = await updateBrandConfig(formData)
      notifyBrandConfigUpdated(res.data)
      ElMessage.success('系统配置已保存')
    } catch (error) {
      console.error('保存系统配置失败:', error)
    } finally {
      saving.value = false
    }
  })
}

onMounted(() => {
  loadData()
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
  color: #303133;
  cursor: default;

  &.active {
    color: #409eff;
    border-bottom: 2px solid #409eff;
  }
}

.config-form {
  padding: 24px;
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}
</style>
