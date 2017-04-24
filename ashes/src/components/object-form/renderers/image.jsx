/* @flow */

// libs
import React from 'react';

// components
import Upload from 'components/upload/upload';
import renderFormField from '../form-field';
import ImageCard from 'components/image-card/image-card';

export default function renderImage(errors: FieldErrors = {}, onChange: ChangeHandler = noop) {
  return function(name: string, value: any, options: AttrOptions, onDrop) {
    const empty = (value.length === 0);
    const children = (value.length > 0) ? <ImageCard src={value} /> : null;
    const imageLoader = (
      <Upload
        empty={empty}
        onDrop={(image) => onDrop(image, name)}
        children={children}
      />
    );

    return renderFormField(name, imageLoader, options);
  }
}
