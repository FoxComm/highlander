/* @flow */

// libs
import React from 'react';

// components
import Upload from 'components/upload/upload';
import ImageCard from 'components/image-card/image-card';

// style
import s from './image.css';

export default function renderImage(errors: FieldErrors = {}, onChange: ChangeHandler = noop) {
  return function(name: string, value: any, options: AttrOptions, addFile, deleteFile ) {
    const actions = [
      { name: 'external-link', handler: () => window.open(value) },
      { name: 'edit', handler: () => console.log('open edit modal') },
      { name: 'trash', handler: () => deleteFile(value, name) }
    ];

    const empty = (value.length === 0);
    const children = empty ? null : (
      <div className={s.imageCard}>
        <ImageCard src={value} actions={actions} />
      </div>
    );

    return (
      <div>
        <label className="fc-object-form__field-label">{name}</label>
        <div className={s.uploadContainer}>
          <Upload
            empty={empty}
            onDrop={(image) => addFile(image, name)}
            children={children}
          />
        </div>
      </div>
    );
  }
}
