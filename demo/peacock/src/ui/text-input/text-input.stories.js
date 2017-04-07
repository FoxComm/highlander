import React from 'react';
import { storiesOf } from '@kadira/storybook';
import TextInput from './text-input';

storiesOf('ui.TextInput', module)
  .addDecorator(story => (
    <div className="_story">
      {story()}
    </div>
  ))
  .add('base', () => (
    <TextInput placeholder="hello" />
  ))
  .add('error', () => (
    <TextInput value="wrong value" error />
  ))
  .add('placeholder', () => (
    <TextInput value="Some value" placeholder="Field YyBvarV" />
  ))
  .add('with label', () => (
    <div style={{width: '200px'}}>
      <TextInput label="EXPAND" />
    </div>
  ))
  .add('error message', () => (
    <div style={{width: '200px'}}>
      <TextInput error="Something went wrong" placeholder="Address" />
    </div>
  ))
  .add('adjoin vertical 2', () => (
    <div>
      <TextInput pos="t" />
      <TextInput pos="b" />
    </div>
  ))
  .add('adjoin vertical 3', () => (
    <div>
      <TextInput placeholder="test" pos="t" error="error" />
      <TextInput pos="middle-v" />
      <TextInput pos="b" error />
    </div>
  ))
  .add('adjoin horizontal 2', () => (
    <div style={{display: 'inline-flex'}}>
      <TextInput pos="l" />
      <TextInput pos="r" />
    </div>
  ))
  .add('full ', () => (
    <div style={{display: 'inline-flex'}}>
      <TextInput pos="l" error="something went wrong" />
      <TextInput pos="middle-h" />
      <TextInput pos="r" error />
    </div>
  ));
