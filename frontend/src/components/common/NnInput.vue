<script setup>
const props = defineProps({
  modelValue: {
    type: String,
    default: "",
  },
  placeholder: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["update:modelValue"]);

const onInput = (event) => {
  emit("update:modelValue", event.target.value);
};
</script>

<template>
  <input
    class="nn-input"
    :value="modelValue"
    :placeholder="placeholder"
    @input="onInput"
  />
</template>

<style scoped>
.nn-input {
  width: 100%;
  padding: 8px 10px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  font-size: 13px;
  outline: none;
  background: #ffffff;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.nn-input:focus {
  border-color: #22c55e;
  box-shadow: 0 0 0 1px rgba(34, 197, 94, 0.15);
}
</style>
