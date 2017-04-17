import renderString from './string';
import renderRichText from './richText';
import renderBoolean from './boolean';
import renderBool from './bool';
import renderDate from './date';
import renderPrice from './price';
import renderColor from './color';
import renderText from './text';
import renderNumber from './number';
import renderOptions from './options';
import renderFormField from './form-field';

export {
  renderString,
  renderRichText,
  renderBoolean,
  renderBool,
  renderDate,
  renderPrice,
  renderColor,
  renderText,
  renderNumber,
  renderOptions,
  renderFormField,
};

export type ChangeHandler = (attributes: Attributes) => void;
export type FieldErrors = { [id: string]: any };
