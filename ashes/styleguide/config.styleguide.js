const path = require('path');
const { camelCase, upperFirst } = require('lodash');

module.exports = {
  title: `Version: <span class="hash">${process.env.GIT_COMMIT}</span>`,
  template: path.join(__dirname, 'template.html'),
  showCode: false,
  ignore: [path.join(__dirname, '../src/components/core/**/*.spec.jsx')],
  webpackConfig: require('./webpack.styleguide.js'),
  styleguideDir: path.resolve('build/admin/styleguide'),
  getComponentPathLine: (componentPath) => {
    const dirname = path.dirname(componentPath, '.jsx');
    const name = dirname.split('/').slice(-1)[0];
    const componentName = upperFirst(camelCase(name));

    const importPath = dirname.split(/\/src\//).pop();

    return `import ${componentName} from ${importPath}`;
  },
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
          sections: [
            {
              name: 'Buttons',
              components: () => ([
                path.resolve(__dirname, '../src/components/core/button/button.jsx'),
                path.resolve(__dirname, '../src/components/core/button-with-menu/button-with-menu.jsx'),
                path.resolve(__dirname, '../src/components/core/save-cancel/save-cancel.jsx'),
              ]),
            },
            {
              name: 'Navigation',
              components: () => ([
                path.resolve(__dirname, '../src/components/core/page-nav/page-nav.jsx'),
              ]),
            },
            {
              name: 'Forms',
              components: () => ([
                path.resolve(__dirname, '../src/components/core/text-mask/text-mask.jsx'),
                path.resolve(__dirname, '../src/components/core/swatch-input/swatch-input.jsx'),
              ]),
            },
            {
              name: 'Other',
              components: () => ([
                path.resolve(__dirname, '../src/components/core/rounded-pill/rounded-pill.jsx'),
              ]),
            },
          ],
        },
      ],
    },
  ],
  require: [
    path.join(__dirname, '../src/css/base.css'),
    path.join(__dirname, '../src/images/favicons/favicon.ico'),
    path.join(__dirname, 'styleguide.less'),
  ]
};
