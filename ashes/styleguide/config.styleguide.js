const path = require('path');

module.exports = {
  title: 'Ashes Components Style Guide',
  showCode: true,
  ignore: [path.join(__dirname, '../src/components/core/**/*.spec.jsx')],
  webpackConfig: require('./webpack.styleguide.js'),
  styleguideDir: path.resolve('build/admin/styleguide'),
  sections: [
    {
      name: 'Documentation',
      sections: [
        {
          name: 'Components',
          content: '../docs/components.md',
        },
        {
          name: 'Styles',
          content: '../docs/styles.md',
        },
        {
          name: 'Flow',
          content: '../docs/flow.md',
        },
        {
          name: 'Tests',
          content: '../docs/tests.md',
        },
      ],
    },
    {
      name: 'Components',
      sections: [
        {
          name: 'Core',
          components: '../src/components/core/**/*.jsx',
        },
      ],
    },
  ],
  require: [
    path.join(__dirname, 'styleguide.less'),
    path.join(__dirname, '../src/css/base.css'),
  ]
};
