/* @flow */

// libs
import React, { Component, Element } from 'react';
import { noop, isEmpty, get } from 'lodash';
import { autobind } from 'core-decorators';

// components
import Upload from 'components/upload/upload';
import Image from 'components/images/image';

// helpers
import { uploadImage, deleteImage } from '../../../paragons/image';

// style
import s from './image.css';

// types
import type { FieldErrors, ChangeHandler } from './index';
import type { ImageFile } from '../../../modules/images';

export default function renderImage(errors: FieldErrors = {}, onChange: ChangeHandler = noop) {
  return function( name: string, value: any, options: AttrOptions ) {
    return (
      <ImageRenderer name={name} value={value} onChange={onChange} />
    );
  };
}

class ImageRenderer extends Component {

  @autobind
  addFile(image: ImageFile): Promise<*> {
    const { name, onChange } = this.props;
    onChange(name, 'image', { loading: true });

    return uploadImage(image).then((obj) => {
      const img = { ...obj, loading: false };
      onChange(name, 'image', img);
    });
  }

  @autobind
  deleteFile(): Promise<*> {
    const { value, name, onChange } = this.props;
    onChange(name, 'image', { loading: true });

    return deleteImage(value).then((obj) => {
      onChange(name, 'image', obj);
    });
  }

  @autobind
  handleSave(obj) {
    const newObject = {
      ...this.props.value,
      ...obj
    };
    this.props.onChange(this.props.name, 'image', newObject);
  }

  render() {
    const { value, name } = this.props;
    const empty = isEmpty(value);

    return (
      <div>
        <label className="fc-object-form__field-label">{name}</label>
        <div className={s.uploadContainer}>
          <Upload
            empty={empty}
            onDrop={(image) => this.addFile(image, name)}>
            <div className={s.imageCard}>
              <Image
                name={name}
                image={value}
                imagePid={value.id}
                deleteImage={this.deleteFile}
                editImage={this.handleSave}
                imageComponent='img'
              />
            </div>
          </Upload>
        </div>
      </div>
    );
  }

}
