const path = require('path');
const { camelCase, upperFirst } = require('lodash');

module.exports = {
  title: `${process.env.GIT_COMMIT}`,
  template: path.join(__dirname, 'template.html'),
  showCode: false,
  showUsage: true,
  ignore: [path.join(__dirname, '../src/components/core/**/*.spec.jsx')],
  webpackConfig: require('./webpack.styleguide.js'),
  styleguideDir: path.resolve('build/admin/styleguide'),
  highlightTheme: 'neo',
  getComponentPathLine: componentPath => {
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
          name: 'Colors and Typography',
          components: () => [
            path.resolve(__dirname, '../src/components/docs/colors/colors.jsx'),
            path.resolve(__dirname, '../src/components/docs/fonts/fonts.jsx'),
          ],
        },
        {
          name: 'Iconography',
          components: () => [
            path.resolve(__dirname, '../src/components/docs/icons/icons.jsx'),
            path.resolve(__dirname, '../src/components/docs/svg-icons/svg-icons.jsx'),
            path.resolve(__dirname, '../src/components/docs/logos/logos.jsx'),
          ],
        },
      ],
    },
    {
      name: 'Core Components',
      sections: [
        {
          name: 'Alerts',
          components: () => [path.resolve(__dirname, '../src/components/core/alert/alert.jsx')],
        },
        {
          name: 'Buttons',
          components: () => [
            path.resolve(__dirname, '../src/components/core/button/button.jsx'),
            path.resolve(__dirname, '../src/components/core/button-with-menu/button-with-menu.jsx'),
            path.resolve(__dirname, '../src/components/core/save-cancel/save-cancel.jsx'),
          ],
        },
        {
          name: 'Navigation',
          components: () => [path.resolve(__dirname, '../src/components/core/page-nav/page-nav.jsx')],
        },
        {
          name: 'Forms',
          components: () => [
            path.resolve(__dirname, '../src/components/core/text-mask/text-mask.jsx'),
            path.resolve(__dirname, '../src/components/core/swatch-input/swatch-input.jsx'),
            path.resolve(__dirname, '../src/components/core/radio-button/radio-button.jsx'),
            path.resolve(__dirname, '../src/components/core/checkbox/checkbox.jsx'),
            path.resolve(__dirname, '../src/components/core/counter/counter.jsx'),
            path.resolve(__dirname, '../src/components/core/text-input/text-input.jsx'),
          ],
        },
        {
          name: 'Modal',
          components: () => [
            path.resolve(__dirname, '../src/components/core/modal-container/modal-container.jsx'),
            path.resolve(__dirname, '../src/components/core/modal/modal.jsx'),
            path.resolve(__dirname, '../src/components/core/confirmation-modal/confirmation-modal.jsx'),
          ],
        },
        {
          name: 'Other',
          components: () => [
            path.resolve(__dirname, '../src/components/core/rounded-pill/rounded-pill.jsx'),
            path.resolve(__dirname, '../src/components/core/spinner/spinner.jsx'),
            path.resolve(__dirname, '../src/components/core/countdown/countdown.jsx'),
            path.resolve(__dirname, '../src/components/core/svg-icon/svg-icon.jsx'),
            path.resolve(__dirname, '../src/components/core/icon/icon.jsx'),
          ],
        },
      ],
    },
    {
      name: 'Utils Components',
      sections: [
        {
          name: 'Errors',
          components: () => [
            path.resolve(__dirname, '../src/components/utils/errors/errors.jsx'),
            path.resolve(__dirname, '../src/components/utils/errors/api-errors.jsx'),
          ],
        },
        {
          name: 'Activity Notifications',
          components: () => [
            path.resolve(__dirname, '../src/components/activity-notifications/item.jsx'),
            path.resolve(__dirname, '../src/components/activity-notifications/panel.jsx'),
            path.resolve(__dirname, '../src/components/activity-notifications/indicator.jsx'),
          ],
        },
        {
          name: 'Other',
          components: () => [
            path.resolve(__dirname, '../src/components/utils/change/change.jsx'),
            path.resolve(__dirname, '../src/components/utils/currency/currency.jsx'),
            path.resolve(__dirname, '../src/components/utils/datetime/datetime.jsx'),
          ],
        },
      ],
    },
  ],
  require: [path.join(__dirname, '../src/images/favicons/favicon.ico'), path.join(__dirname, 'styleguide.css')],
};
