import { defineComponent, h, inject, provide } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import AdminTablePage from '@/modules/admin/AdminTablePage.vue';

interface Row {
  id: string;
  name: string;
  status: string;
}

interface TableStubProps {
  data: unknown[];
}

const rows: Row[] = [
  { id: '1', name: '陈一鸣', status: 'active' },
  { id: '2', name: '静默作者', status: 'banned' },
];

function mountTable() {
  return mount(AdminTablePage<Row>, {
    props: {
      title: '用户管理',
      description: '管理社区用户状态',
      rows,
      columns: [
        { key: 'name', label: '用户' },
        {
          key: 'status',
          label: '状态',
          tag: (row) => ({
            label: row.status === 'active' ? '正常' : '封禁',
            type: row.status === 'active' ? 'success' : 'danger',
          }),
        },
      ],
      pageSize: 10,
    },
    global: {
      stubs: {
        ElInput: defineComponent({
          props: {
            modelValue: { type: String, default: '' },
          },
          emits: ['update:modelValue'],
          setup(props, { attrs, emit }) {
            return () =>
              h('input', {
                ...attrs,
                value: props.modelValue,
                onInput: (event: Event) => {
                  emit('update:modelValue', (event.target as HTMLInputElement).value);
                },
              });
          },
        }),
        ElTable: defineComponent({
          props: {
            data: { type: Array, default: () => [] },
            emptyText: { type: String, default: '' },
          },
          setup(props, { slots }) {
            provide('admin-table-test-props', props);
            return () =>
              h(
                'div',
                props.data.length
                  ? slots.default?.()
                  : props.emptyText,
              );
          },
        }),
        ElTableColumn: defineComponent({
          props: {
            label: { type: String, default: '' },
          },
          setup(props, { attrs, slots }) {
            const tableProps = inject<TableStubProps>('admin-table-test-props', { data: [] });
            return () =>
              h('div', [
                props.label,
                ...tableProps.data.map((row) => h('div', slots.default?.({ ...attrs, row }))),
              ]);
          },
        }),
        ElPagination: true,
        ElTag: defineComponent({
          setup(_, { slots }) {
            return () => h('span', slots.default?.());
          },
        }),
        ElButton: defineComponent({
          setup(_, { slots }) {
            return () => h('button', slots.default?.());
          },
        }),
      },
    },
  });
}

describe('AdminTablePage', () => {
  it('filters rows and shows an empty state when no row matches', async () => {
    const wrapper = mountTable();

    expect(wrapper.text()).toContain('陈一鸣');
    expect(wrapper.text()).toContain('静默作者');

    await wrapper.find('[data-testid="admin-table-filter"]').setValue('静默');

    expect(wrapper.text()).not.toContain('陈一鸣');
    expect(wrapper.text()).toContain('静默作者');

    await wrapper.find('[data-testid="admin-table-filter"]').setValue('不存在');

    expect(wrapper.text()).toContain('没有匹配的数据');
  });

  it('filters rows by visible tag labels', async () => {
    const wrapper = mountTable();

    await wrapper.find('[data-testid="admin-table-filter"]').setValue('封禁');

    expect(wrapper.text()).not.toContain('陈一鸣');
    expect(wrapper.text()).toContain('静默作者');
    expect(wrapper.text()).toContain('封禁');
  });
});
