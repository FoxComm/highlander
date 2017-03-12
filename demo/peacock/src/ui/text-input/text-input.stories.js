import React from 'react';
import { storiesOf, action } from '@kadira/storybook';
import TextInput from './text-input';

storiesOf('ui.TextInput', module)
  .add('base', () => (
    <TextInput />
  ))
  .add('adjoin vertical 2', () => (
    <div>
      <TextInput adjoin="b" />
      <TextInput adjoin="t" />
    </div>
  ))
  .add('adjoin vertical 3', () => (
    <div>
      <TextInput adjoin="b" />
      <TextInput adjoin="bt" />
      <TextInput adjoin="t" />
    </div>
  ))
  .add('adjoin horizontal 2', () => (
    <div style={{display: 'inline-flex'}}>
      <TextInput adjoin="r" />
      <TextInput adjoin="l" />
    </div>
  ))
  .add('adjoin horizontal 3', () => (
    <div style={{display: 'inline-flex'}}>
      <TextInput adjoin="r" />
      <TextInput adjoin="rl" />
      <TextInput adjoin="l" />
    </div>
  ))


