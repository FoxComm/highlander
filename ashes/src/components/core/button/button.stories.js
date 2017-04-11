import React from 'react';
import { storiesOf, action } from '@kadira/storybook';
import { WithNotes } from '@kadira/storybook-addon-notes';
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

storiesOf('core.Button', module)
  .add('General Button', () => (
    <WithNotes notes={'We can add some notes to the story from the code'}>
      <Button onClick={action('clicked')}>General</Button>
    </WithNotes>
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
