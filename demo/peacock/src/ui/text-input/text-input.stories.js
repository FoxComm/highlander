import React from 'react';
import { storiesOf, action } from '@kadira/storybook';
import TextInput from './text-input';

storiesOf('ui.TextInput', module)
  .add('base', () => (
    <TextInput />
  ))
  .add('adjoin vertical 2', () => (
    <div>
      <TextInput pos="t" />
      <TextInput pos="b" />
    </div>
  ))
  .add('adjoin vertical 3', () => (
    <div>
      <TextInput pos="t" />
      <TextInput pos="middle" />
      <TextInput pos="b" />
    </div>
  ))
  .add('adjoin horizontal 2', () => (
    <div style={{display: 'inline-flex'}}>
      <TextInput pos="l" />
      <TextInput pos="r" />
    </div>
  ))
  .add('adjoin horizontal 3', () => (
    <div style={{display: 'inline-flex'}}>
      <TextInput pos="l" />
      <TextInput pos="middle" />
      <TextInput pos="r" />
    </div>
  ));


