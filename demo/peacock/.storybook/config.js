import { configure, addDecorator } from '@kadira/storybook';

const req = require.context('../src/ui', true, /.stories.js$/)

function loadStories() {
  req.keys().forEach((filename) => req(filename))
}

configure(loadStories, module);
