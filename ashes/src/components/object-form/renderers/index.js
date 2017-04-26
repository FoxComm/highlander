/* @flow */

import { Element } from 'react';

import renderString from './string';
import renderRichText from './richText';
import renderBoolean from './boolean';
import renderDate from './date';
import renderPrice from './price';
import renderColor from './color';
import renderText from './text';
import renderNumber from './number';
import renderOptions from './options';

// types
export type ChangeHandler = (name: string, type: string, value: any) => any;
export type FieldErrors = { [id: string]: any };
export type FieldRenderer = (errors: FieldErrors, onChange: ChangeHandler) =>
  (name: string, value: any, options: AttrOptions) => Element<any>;

const renderers: { [key: string]: FieldRenderer } = {
  renderString,
  renderRichText,
  renderBoolean,
  renderBool: renderBoolean,
  renderDate,
  renderPrice,
  renderColor,
  renderText,
  renderNumber,
  renderOptions,
};

export default renderers;
