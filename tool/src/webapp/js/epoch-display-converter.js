Vue.component('epoch-display-converter', {
  template: `
  <span>{{getDisplayString()}}</span>
`,
  data: function() {
    return {};
  },
  props: ['epoch'],
  methods: {
    getDisplayString: function() {
      return new Date(parseInt(this.epoch)).toLocaleString();
    },
  },
});
