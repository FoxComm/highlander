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
          name: 'Colors and Typo',
          sections: [{
            content: '../docs/colors-and-typos.md',
          }, {
            components: () => ([
              path.resolve(__dirname, '../src/components/docs/colors/text-colors.jsx'),
              path.resolve(__dirname, '../src/components/docs/colors/bg-colors.jsx'),
              path.resolve(__dirname, '../src/components/docs/fonts/fonts.jsx'),
            ]),
          }]
        },
        {
          name: 'Iconography',
          components: () => ([
            path.resolve(__dirname, '../src/components/docs/icons/icons.jsx'),
            path.resolve(__dirname, '../src/components/docs/svg-icons/svg-icons.jsx'),
            path.resolve(__dirname, '../src/components/docs/logos/logos.jsx'),
          ]),
        },
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
              name: 'Alerts',
              components: () => ([
                path.resolve(__dirname, '../src/components/core/alert/alert.jsx'),
              ]),
            },
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
                path.resolve(__dirname, '../src/components/core/radio-button/radio-button.jsx'),
                path.resolve(__dirname, '../src/components/core/checkbox/checkbox.jsx'),
                path.resolve(__dirname, '../src/components/core/counter/counter.jsx'),
                path.resolve(__dirname, '../src/components/core/text-input/text-input.jsx'),
              ]),
            },
            {
              name: 'Modal',
              components: () => ([
                path.resolve(__dirname, '../src/components/core/modal-container/modal-container.jsx'),
                path.resolve(__dirname, '../src/components/core/modal/modal.jsx'),
                path.resolve(__dirname, '../src/components/core/confirmation-modal/confirmation-modal.jsx'),
              ]),
            },
            {
              name: 'Other',
              components: () => ([
                path.resolve(__dirname, '../src/components/core/rounded-pill/rounded-pill.jsx'),
                path.resolve(__dirname, '../src/components/core/spinner/spinner.jsx'),
                path.resolve(__dirname, '../src/components/core/countdown/countdown.jsx'),
                path.resolve(__dirname, '../src/components/core/svg-icon/svg-icon.jsx'),
                path.resolve(__dirname, '../src/components/core/icon/icon.jsx'),
              ]),
            },
          ],
        },
        {
          name: 'Utils',
          sections: [
            {
              name: 'Errors',
              components: () => ([
                path.resolve(__dirname, '../src/components/utils/errors/errors.jsx'),
                path.resolve(__dirname, '../src/components/utils/errors/api-errors.jsx'),
              ]),
            },
            {
              name: 'Activity Notifications',
              components: () => ([
                path.resolve(__dirname, '../src/components/activity-notifications/item.jsx'),
                path.resolve(__dirname, '../src/components/activity-notifications/panel.jsx'),
                path.resolve(__dirname, '../src/components/activity-notifications/indicator.jsx'),
              ]),
            },
            {
              name: 'Other',
              components: () => ([
                path.resolve(__dirname, '../src/components/utils/change/change.jsx'),
              ]),
            },
          ]
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
