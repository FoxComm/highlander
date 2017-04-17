/* @flow */

import renderFormField from './form-field';

export default function renderElement(name: string, value: any, options: AttrOptions) {
  return renderFormField(name, value, options);
}
