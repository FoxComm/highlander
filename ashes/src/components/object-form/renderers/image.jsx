/* @flow */

// libs
import React, { Component, Element } from 'react';
import { noop, isEmpty } from 'lodash';
import { autobind } from 'core-decorators';

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
    return (
      <ImageRenderer name={name} value={value} onChange={onChange}/>
    )
  };
}

class ImageRenderer extends React.Component {

  @autobind
  addFile(image: ImageFile): void {
    const { name, onChange } = this.props;
    uploadImage(image).then((obj) => {
      onChange(name, 'image', obj);
    });
  };

  @autobind
  deleteFile(): void {
    const { value, name, onChange } = this.props;
    deleteImage(value).then((obj) => {
      onChange(name, 'image', obj);
    });
  };

  get actions() {
    const { value } = this.props;
    return [
      { name: 'external-link', handler: () => window.open(value.src) },
      { name: 'edit', handler: () => {} },
      { name: 'trash', handler: this.deleteFile }
    ]
  };

  render() {
    const { value, name } = this.props;
    const empty = isEmpty(value);
    const children = empty ? null : (
      <div className={s.imageCard}>
        <ImageCard
          src={value.src}
          actions={this.actions}
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
            onDrop={(image) => this.addFile(image, name)}
          >
            {children}
          </Upload>
        </div>
      </div>
    );
  }

}
