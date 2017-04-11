import React from 'react';
import { configure, addDecorator } from '@kadira/storybook';
import { setOptions } from '@kadira/storybook-addon-options';

import '!style!css!../src/css/app.css';

const style = {
  position: 'absolute',
  top: '50%',
  transform: 'translateY(-50%)',
  width: '100%',
  textAlign: 'center',
};

const CenterDecorator = (story) => (
  <div style={style}>
    {story()}
  </div>
);

addDecorator(CenterDecorator);

setOptions({
  name: 'Fox Ashes Storybook',
  url: 'https://stage.foxcommerce.com',
});

const req = require.context('../src/components', true, /.stories.js$/);

function loadStories() {
  req.keys().forEach((filename) => req(filename));
}

configure(loadStories, module);
