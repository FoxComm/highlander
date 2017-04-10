import React from 'react';
import { storiesOf, action } from '@kadira/storybook';
import {
  Button,
  LeftButton,
  RightButton,
  DecrementButton,
  IncrementButton,
  DeleteButton,
  EditButton,
  AddButton,
  CloseButton,
  PrimaryButton,
} from '../../src/components/common/buttons';

import '!style!css!../../src/css/app.css';

storiesOf('core.Button', module)
  .add('General Button', () => (
    <Button onClick={action('clicked')}>Hello General Button</Button>
  ))
  .add('Loading Button', () => (
    <Button onClick={action('clicked')} isLoading>Hello Loading Button</Button>
  ))
  .add('Primary Button', () => (
    <PrimaryButton onClick={action('clicked')}>Hello Primary Button</PrimaryButton>
  ))
  .add('Left Button', () => (
    <LeftButton onClick={action('clicked')}>Hello Left Button</LeftButton>
  ))
  .add('Right Button', () => (
    <RightButton onClick={action('clicked')}>Hello Right Button</RightButton>
  ))
  .add('Decrement Button', () => (
    <DecrementButton onClick={action('clicked')}>Hello Decrement Button</DecrementButton>
  ))
  .add('Increment Button', () => (
    <IncrementButton onClick={action('clicked')}>Hello Increment Button</IncrementButton>
  ))
  .add('Delete Button', () => (
    <DeleteButton onClick={action('clicked')}>Hello Delete Button</DeleteButton>
  ))
  .add('Edit Button', () => (
    <EditButton onClick={action('clicked')}>Hello Edit Button</EditButton>
  ))
  .add('Add Button', () => (
    <AddButton onClick={action('clicked')}>Hello Add Button</AddButton>
  ))
  .add('Close Button', () => (
    <CloseButton onClick={action('clicked')}>Hello Close Button</CloseButton>
  ));
