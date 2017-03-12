import React from 'react';
import { storiesOf, action } from '@kadira/storybook';
import TextInputWithLabel from './text-input-with-label';

storiesOf('ui.TextInputWithLabel', module)
  .add('base', () => (
    <TextInputWithLabel label="LABEL" />
  ));
