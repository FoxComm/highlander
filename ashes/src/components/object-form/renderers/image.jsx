/* @flow */

// libs
import React from 'react';
import { noop, isEmpty } from 'lodash';

// components
import Upload from 'components/upload/upload';
import ImageCard from 'components/image-card/image-card';

// helpers
import { uploadImage, deleteImage } from '../../../paragons/image';

// style
import s from './image.css';

// types
import type { FieldErrors, ChangeHandler } from './index';

export default function renderImage(errors: FieldErrors = {}, onChange: ChangeHandler = noop) {
  return function( name: string, value: any, options: AttrOptions ) {

    const addFile = (image: ImageFile): void => {
      uploadImage(image).then((obj) => {
        onChange(name, 'image', obj);
      });
    };

    const deleteFile = (value): void => {
      deleteImage(value).then((obj) => {
        onChange(name, 'image', obj);
      });
    };

    const actions = [
      { name: 'external-link', handler: () => window.open(value.src) },
      { name: 'edit', handler: () => {} },
      { name: 'trash', handler: () => deleteFile(value) }
    ];

    const empty = isEmpty(value);
    const children = empty ? null : (
      <div className={s.imageCard}>
        <ImageCard
          src={value.src}
          actions={actions}
          id={value.id}
          loading={empty}
          secondaryTitle={`Uploaded ${value.uploadedAt}`}
        />
      </div>
    );

      return (
        <div>
          <label className="fc-object-form__field-label">{name}</label>
          <div className={s.uploadContainer}>
            <Upload
              empty={empty}
              onDrop={(image) => addFile(image, name)}
            >
              {children}
            </Upload>
          </div>
        </div>
      );
  };
}
