<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { ToolParameterField, ToolParameterGroup } from '@/modules/tool/api'

const props = defineProps<{
  schemaJson: string | null
  modelValue: Record<string, unknown>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, unknown>]
}>()

const schema = computed(() => {
  if (!props.schemaJson) return null
  try {
    return JSON.parse(props.schemaJson)
  } catch {
    return null
  }
})

const fields = computed<ToolParameterField[]>(() => {
  if (!schema.value) return []
  return schema.value.fields || []
})

const groups = computed<ToolParameterGroup[]>(() => {
  if (!schema.value) return []
  return schema.value.groups || []
})

const schemaVersion = computed(() => {
  return schema.value?.schemaVersion || 1
})

const hasSchema = computed(() => fields.value.length > 0)
const hasGroups = computed(() => groups.value.length > 0)

function buildValues(source: Record<string, unknown>): Record<string, unknown> {
  const values: Record<string, unknown> = { ...source }
  if (!hasSchema.value) return values
  for (const field of fields.value) {
    if (values[field.key] !== undefined) continue
    if (field.defaultValue !== undefined) values[field.key] = field.defaultValue
    else if (field.type === 'boolean') values[field.key] = false
    else if (field.type === 'number') values[field.key] = field.min || 0
    else if (field.type === 'array') values[field.key] = []
    else values[field.key] = ''
  }
  return values
}

const localValues = ref<Record<string, unknown>>(buildValues(props.modelValue))

watch(() => props.modelValue, (nv) => {
  localValues.value = buildValues(nv)
}, { deep: true })

function emitUpdate() {
  emit('update:modelValue', { ...localValues.value })
}

function parseNumber(val: string | number | undefined, field: ToolParameterField): number {
  if (val === undefined || val === null || val === '') return field.min || 0
  const n = typeof val === 'string' ? parseInt(val, 10) : val
  return isNaN(n) ? (field.min || 0) : n
}

// dependsOn check
function isFieldVisible(field: ToolParameterField): boolean {
  if (!field.dependsOn) return true
  const depValue = localValues.value[field.dependsOn.field]
  return String(depValue) === String(field.dependsOn.equals)
}

// Array item management
function addArrayItem(field: ToolParameterField) {
  const arr = (localValues.value[field.key] as string[]) || []
  if (field.maxItems && arr.length >= field.maxItems) return
  arr.push('')
  localValues.value[field.key] = [...arr]
  emitUpdate()
}

function removeArrayItem(field: ToolParameterField, index: number) {
  const arr = (localValues.value[field.key] as string[]) || []
  arr.splice(index, 1)
  localValues.value[field.key] = [...arr]
  emitUpdate()
}

function updateArrayItem(field: ToolParameterField, index: number, value: string) {
  const arr = (localValues.value[field.key] as string[]) || []
  if (field.itemMaxLength && value.length > field.itemMaxLength) {
    value = value.slice(0, field.itemMaxLength)
  }
  arr[index] = value
  localValues.value[field.key] = [...arr]
  emitUpdate()
}

// Get ungrouped fields (fields not in any group)
const ungroupedFields = computed<ToolParameterField[]>(() => {
  if (!hasGroups.value) return fields.value
  const groupedKeys = new Set<string>()
  for (const g of groups.value) {
    for (const f of g.fields) groupedKeys.add(f)
  }
  return fields.value.filter(f => !groupedKeys.has(f.key))
})

// Get group's visible fields
function getGroupFields(group: ToolParameterGroup): ToolParameterField[] {
  const fieldMap = new Map(fields.value.map(f => [f.key, f]))
  return group.fields
    .map(key => fieldMap.get(key))
    .filter((f): f is ToolParameterField => f != null && isFieldVisible(f))
}

// Schema version display
const schemaVersionLabel = computed(() => {
  return schemaVersion.value >= 2 ? `v${schemaVersion.value}` : ''
})
</script>

<template>
  <div class="tpf-root" data-testid="tool-parameter-form">
    <div v-if="!hasSchema" class="tpf-empty">
      该工具暂无可配置参数
    </div>

    <!-- Schema Version Badge -->
    <div v-if="schemaVersionLabel" class="tpf-version-badge" data-testid="tool-param-schema-version">
      {{ schemaVersionLabel }}
    </div>

    <div v-if="hasSchema" class="tpf-fields">
      <!-- Render by groups -->
      <div v-for="group in groups" :key="group.key" class="tpf-group" data-testid="tool-param-group">
        <div class="tpf-group-header">
          <span class="tpf-group-title">{{ group.title }}</span>
          <span v-if="group.description" class="tpf-group-desc">{{ group.description }}</span>
        </div>
        <div class="tpf-group-fields">
          <div
            v-for="field in getGroupFields(group)"
            :key="field.key"
            class="tpf-field"
            :data-testid="'tool-param-' + field.key"
          >
            <label class="tpf-label">
              {{ field.label }}
              <span v-if="field.required" class="tpf-required">*</span>
              <span v-if="field.type === 'array'" class="tpf-type-badge">array</span>
            </label>

            <!-- Text -->
            <el-input
              v-if="field.type === 'text'"
              v-model="localValues[field.key]"
              :maxlength="field.maxLength"
              :placeholder="'请输入' + field.label"
              size="small"
              @update:model-value="emitUpdate"
            />

            <!-- Textarea -->
            <el-input
              v-else-if="field.type === 'textarea'"
              v-model="localValues[field.key]"
              type="textarea"
              :maxlength="field.maxLength"
              :rows="2"
              :placeholder="'请输入' + field.label"
              size="small"
              @update:model-value="emitUpdate"
            />

            <!-- Boolean -->
            <el-switch
              v-else-if="field.type === 'boolean'"
              v-model="localValues[field.key]"
              active-text="是"
              inactive-text="否"
              @update:model-value="emitUpdate"
            />

            <!-- Number -->
            <el-input-number
              v-else-if="field.type === 'number'"
              v-model="localValues[field.key]"
              :min="field.min ?? 0"
              :max="field.max ?? 999"
              size="small"
              controls-position="right"
              @update:model-value="(val: number) => { localValues[field.key] = val; emitUpdate() }"
            />

            <!-- Select -->
            <el-select
              v-else-if="field.type === 'select'"
              v-model="localValues[field.key]"
              :placeholder="'请选择' + field.label"
              size="small"
              style="width:100%"
              @update:model-value="emitUpdate"
            >
              <el-option
                v-for="opt in field.options"
                :key="opt"
                :label="opt"
                :value="opt"
              />
            </el-select>

            <!-- Array -->
            <div v-else-if="field.type === 'array'" class="tpf-array" :data-testid="'tool-param-array-' + field.key">
              <div
                v-for="(item, idx) in (localValues[field.key] as string[] || [])"
                :key="idx"
                class="tpf-array-item"
              >
                <el-input
                  v-model="(localValues[field.key] as string[])[idx]"
                  :maxlength="field.itemMaxLength"
                  size="small"
                  placeholder="输入文件路径"
                  @update:model-value="(val: string) => updateArrayItem(field, idx, val)"
                />
                <el-button
                  size="small"
                  type="danger"
                  text
                  :data-testid="'btn-remove-array-item-' + field.key"
                  @click="removeArrayItem(field, idx)"
                >✕</el-button>
              </div>
              <el-button
                size="small"
                type="primary"
                plain
                :disabled="field.maxItems ? (localValues[field.key] as string[] || []).length >= field.maxItems : false"
                :data-testid="'btn-add-array-item-' + field.key"
                @click="addArrayItem(field)"
              >+ 新增一项</el-button>
            </div>
          </div>
        </div>

        <!-- Path rules hint -->
        <template v-for="field in getGroupFields(group)" :key="'pr-' + field.key">
          <div
            v-if="field.pathRules && isFieldVisible(field)"
            class="tpf-path-rules"
            data-testid="tool-param-path-rules"
          >
            <div v-if="field.pathRules?.allowPrefixes" class="tpf-path-rule-line">
              允许前缀：{{ field.pathRules.allowPrefixes.join(', ') }}
            </div>
            <div v-if="field.pathRules?.deny" class="tpf-path-rule-line">
              禁止：{{ field.pathRules.deny.join(', ') }}
            </div>
          </div>
        </template>
      </div>

      <!-- Ungrouped fields -->
      <div v-if="ungroupedFields.length > 0" class="tpf-group" data-testid="tool-param-group-ungrouped">
        <div class="tpf-group-header">
          <span class="tpf-group-title">其他参数</span>
        </div>
        <div class="tpf-group-fields">
          <template v-for="field in ungroupedFields" :key="field.key">
            <div
              v-if="isFieldVisible(field)"
              class="tpf-field"
              :data-testid="'tool-param-' + field.key"
            >
            <label class="tpf-label">
              {{ field.label }}
              <span v-if="field.required" class="tpf-required">*</span>
            </label>

            <el-input
              v-if="field.type === 'text'"
              v-model="localValues[field.key]"
              :maxlength="field.maxLength"
              size="small"
              @update:model-value="emitUpdate"
            />
            <el-input
              v-else-if="field.type === 'textarea'"
              v-model="localValues[field.key]"
              type="textarea"
              :rows="2"
              size="small"
              @update:model-value="emitUpdate"
            />
            <el-switch
              v-else-if="field.type === 'boolean'"
              v-model="localValues[field.key]"
              active-text="是"
              inactive-text="否"
              @update:model-value="emitUpdate"
            />
            <el-input-number
              v-else-if="field.type === 'number'"
              v-model="localValues[field.key]"
              :min="field.min ?? 0"
              :max="field.max ?? 999"
              size="small"
              controls-position="right"
              @update:model-value="(val: number) => { localValues[field.key] = val; emitUpdate() }"
            />
            <el-select
              v-else-if="field.type === 'select'"
              v-model="localValues[field.key]"
              size="small"
              style="width:100%"
              @update:model-value="emitUpdate"
            >
              <el-option v-for="opt in field.options || []" :key="opt" :label="opt" :value="opt" />
            </el-select>
          </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tpf-root {
  min-height: 40px;
}
.tpf-empty {
  font-size: 12px;
  color: var(--app-text-muted);
  padding: 12px 0;
  text-align: center;
}
.tpf-version-badge {
  display: inline-block;
  font-size: 10px;
  font-weight: 600;
  color: var(--app-primary, #409eff);
  background: rgba(64, 158, 255, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
  margin-bottom: 12px;
  letter-spacing: 0.5px;
}
.tpf-fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.tpf-group {
  border: 1px solid var(--app-border, rgba(255,255,255,0.06));
  border-radius: var(--app-radius, 6px);
  padding: 12px;
  background: rgba(255,255,255,0.02);
}
.tpf-group-header {
  margin-bottom: 12px;
}
.tpf-group-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
}
.tpf-group-desc {
  display: block;
  font-size: 11px;
  color: var(--app-text-muted);
  margin-top: 2px;
}
.tpf-group-fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.tpf-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tpf-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--app-text-soft);
}
.tpf-required {
  color: var(--el-color-danger, #f56c6c);
  margin-left: 2px;
}
.tpf-type-badge {
  font-size: 9px;
  color: var(--app-primary, #409eff);
  background: rgba(64, 158, 255, 0.1);
  padding: 1px 5px;
  border-radius: 4px;
  margin-left: 4px;
  vertical-align: middle;
}
.tpf-array {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.tpf-array-item {
  display: flex;
  gap: 4px;
  align-items: center;
}
.tpf-path-rules {
  margin-top: 4px;
  margin-bottom: 8px;
  padding: 6px 10px;
  background: rgba(148,163,184,0.04);
  border: 1px solid var(--app-border, rgba(255,255,255,0.06));
  border-radius: var(--app-radius, 4px);
}
.tpf-path-rule-line {
  font-size: 10px;
  color: var(--app-text-muted);
  font-family: monospace;
}
</style>
