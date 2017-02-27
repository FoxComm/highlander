/* @flow */

// styles
import styles from './images.css';

// libs
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import moment from 'moment';

// components
import BodyPortal from '../body-portal/body-portal';
import ConfirmationDialog from '../modal/confirmation-dialog';
import ImageCard from '../image-card/image-card';
import EditImage from './edit-image';

// types
import type { ImageFile, ImageInfo } from '../../modules/images';
import type { Action } from '../image-card/image-card';

export type Props = {
  image: ImageFile;
  editImage: (info: ImageInfo) => Promise<*>;
  deleteImage: () => Promise<*>;
  imagePid: string|number;
};

type State = {
  editMode: boolean;
  deleteMode: boolean;
}

export default class Image extends Component<void, Props, State> {
  props: Props;

  state: State = {
    editMode: false,
    deleteMode: false,
  };

  @autobind
  handleEditImage(): void {
    this.setState({ editMode: true });
  }

  @autobind
  handleCancelEditImage(): void {
    this.setState({ editMode: false });
  }

  @autobind
  handleConfirmEditImage(form: ImageInfo): void {
    this.props.editImage(form);

    this.setState({ editMode: false });
  }

  @autobind
  handleDeleteImage(): void {
    this.setState({ deleteMode: true });
  }

  @autobind
  handleCancelDeleteImage(): void {
    this.setState({ deleteMode: false });
  }

  @autobind
  handleConfirmDeleteImage(): void {
    this.props.deleteImage();

    this.setState({ deleteMode: false });
  }

  get deleteImageDialog(): ?Element<*> {
    if (!this.state.deleteMode) {
      return;
    }

    return (
      <BodyPortal className={styles.modal}>
        <ConfirmationDialog
          isVisible={true}
          header='Delete Image'
          body={'Are you sure you want to delete this image?'}
          cancel='Cancel'
          confirm='Yes, Delete'
          onCancel={this.handleCancelDeleteImage}
          confirmAction={this.handleConfirmDeleteImage}
        />
      </BodyPortal>
    );
  }

  get editImageDialog(): ?Element<*> {
    return (
      <BodyPortal className={styles.modal}>
        <EditImage
          isVisible={this.state.editMode}
          image={this.props.image}
          onCancel={this.handleCancelEditImage}
          onSave={this.handleConfirmEditImage}
        />
      </BodyPortal>
    );
  }

  @autobind
  getImageActions(): Array<Action> {
    const actionsHandler = (handler: () => void) => {
      return (e: MouseEvent) => {
        e.stopPropagation();
        handler();
      };
    };

    return [
      { name: 'external-link', handler: actionsHandler(() => window.open(this.props.image.src)) },
      { name: 'edit', handler: actionsHandler(() => this.handleEditImage()) },
      { name: 'trash', handler: actionsHandler(() => this.handleDeleteImage()) },
    ];
  }

  render() {
    const { image, imagePid } = this.props;

    return (
      <div>
        {this.editImageDialog}
        {this.deleteImageDialog}
        <ImageCard
          id={image.id}
          src={image.src}
          title={image.title}
          secondaryTitle={`Uploaded ${image.uploadedAt || moment().format('MM/DD/YYYY HH: mm')}`}
          actions={this.getImageActions()}
          loading={image.loading}
          key={`${imagePid}`}
        />
      </div>
    );
  }
}
