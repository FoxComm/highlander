/* @flow */

// libs
import React, { Component, Element } from 'react';
import { noop, isEmpty, get } from 'lodash';
import { autobind } from 'core-decorators';

// components
import Upload from 'components/upload/upload';
import ImageCard from 'components/image-card/image-card';
import EditImage from 'components/images/edit-image';
import BodyPortal from 'components/body-portal/body-portal';

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
      <ImageRenderer name={name} value={value} onChange={onChange}/>
    );
  };
}

type State = {
  editMode: boolean
}

class ImageRenderer extends React.Component {

  state: State = {
    editMode: false
  };

  @autobind
  addFile(image: ImageFile): void {
    const { name, onChange } = this.props;
    uploadImage(image).then((obj) => {
      onChange(name, 'image', obj);
    });
  }

  @autobind
  deleteFile(): void {
    const { value, name, onChange } = this.props;
    deleteImage(value).then((obj) => {
      onChange(name, 'image', obj);
    });
  }

  @autobind
  handleModeChange() {
    this.setState({ editMode: !this.state.editMode });
  }

  @autobind
  handleSave(obj) {
    const newObject = {
      ...this.props.value,
      ...obj
    };

    this.setState({ editMode: !this.state.editMode },
      this.props.onChange(this.props.name, 'image', newObject));
  }

  get actions() {
    const { value } = this.props;
    return [
      { name: 'external-link', handler: () => window.open(value.src) },
      { name: 'edit', handler: this.handleModeChange },
      { name: 'trash', handler: this.deleteFile }
    ];
  }

  get editImageDialog() {
    return (
      <BodyPortal className={s.modal}>
        <EditImage
          image={this.props.value}
          isVisible={this.state.editMode}
          onCancel={this.handleModeChange}
          onSave={this.handleSave}
        />
      </BodyPortal>
    );
  }

  render() {
    const { value, name } = this.props;
    const empty = isEmpty(value);
    const title = get(value, 'title');
    const alt = get(value, 'alt');
    const children = empty ? null : (
      <div className={s.imageCard}>
        <ImageCard
          src={value.src}
          actions={this.actions}
          id={value.id}
          loading={empty}
          title={title}
          secondaryTitle={alt || `Uploaded ${value.uploadedAt}`}
        />
      </div>
    );

    return (
      <div>
        {this.editImageDialog}
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
