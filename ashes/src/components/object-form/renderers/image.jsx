/* @flow */

// libs
import React, { Component, Element } from 'react';
import { noop, isEmpty } from 'lodash';
import { autobind } from 'core-decorators';

// components
import Upload from 'components/upload/upload';
import Image from 'components/images/image';

// helpers
import { uploadImage } from '../../../paragons/image';

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

type Props = {
  name: string,
  value: Object,
  onChange: ChangeHandler,
}

class ImageRenderer extends Component {
  props: Props;

  @autobind
  addFile(image: Array<ImageFile>) {
    const { name, onChange } = this.props;
    onChange(name, 'image', { loading: true });

    const newImage = image.map((file: ImageFile) => ({
      title: file.file.name,
      alt: file.file.name,
      src: file.src,
      file: file.file,
      key: file.key,
      loading: true,
    }));

    uploadImage(newImage).then(res => {
      onChange(name, 'image', res[0]);
    });
  }

  @autobind
  deleteFile(): Promise<*>  {
    const { name, onChange } = this.props;
    return Promise.resolve(onChange(name, 'image', ''));
  }

  @autobind
  handleSave(obj) {
    const newObject = {
      ...this.props.value,
      ...obj
    };

    return Promise.resolve(this.props.onChange(this.props.name, 'image', newObject));
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
              />
            </div>
          </Upload>
        </div>
      </div>
    );
  }

}
