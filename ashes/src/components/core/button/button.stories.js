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
} from './index';

import '!style!css!../../../css/app.css';

const style = {
  position: 'absolute',
  top: '50%',
  transform: 'translateY(-50%)',
  width: '100%',
  textAlign: 'center',
};

storiesOf('core.Button', module)
  .addDecorator((story) => (
    <div style={style}>{story()}</div>
  ))
  .add('General Button', () => (
    <Button onClick={action('clicked')}>General</Button>
  ))
  .add('Empty Button', () => (
    <Button onClick={action('clicked')} />
  ))
  .add('Button With Icon Only', () => (
    <Button onClick={action('clicked')}><i className="icon-align-justify" /></Button>
  ))
  .add('Loading Button', () => (
    <Button onClick={action('clicked')} isLoading>Loading</Button>
  ))
  .add('Primary Button', () => (
    <PrimaryButton onClick={action('clicked')}>Primary</PrimaryButton>
  ))
  .add('Loading Primary Button', () => (
    <PrimaryButton onClick={action('clicked')} isLoading>Loading Primary</PrimaryButton>
  ))
  .add('Left Button', () => (
    <LeftButton onClick={action('clicked')}>Left</LeftButton>
  ))
  .add('Right Button', () => (
    <RightButton onClick={action('clicked')}>Right</RightButton>
  ))
  .add('Decrement Button', () => (
    <DecrementButton onClick={action('clicked')}>Decrement</DecrementButton>
  ))
  .add('Increment Button', () => (
    <IncrementButton onClick={action('clicked')}>Increment</IncrementButton>
  ))
  .add('Delete Button', () => (
    <DeleteButton onClick={action('clicked')}>Delete</DeleteButton>
  ))
  .add('Edit Button', () => (
    <EditButton onClick={action('clicked')}>Edit</EditButton>
  ))
  .add('Add Button', () => (
    <AddButton onClick={action('clicked')}>Add</AddButton>
  ))
  .add('Close Button', () => (
    <CloseButton onClick={action('clicked')}>Close</CloseButton>
  ));
