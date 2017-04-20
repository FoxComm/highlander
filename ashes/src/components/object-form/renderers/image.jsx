/* @flow */

// libs
import React from 'react';

// components
import Upload from 'components/upload/upload';
import renderFormField from '../form-field';

export default function renderImage(errors: FieldErrors = {}, onChange: ChangeHandler = noop) {
  return function(name: string, value: any, options: AttrOptions, onDrop) {
    const imageLoader = <Upload empty={true} onDrop={onDrop}/>;

    return renderFormField(name, imageLoader, options);
  }
}
