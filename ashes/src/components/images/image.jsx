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
  disabled?: boolean;
};

type State = {
  editMode: boolean;
  deleteMode: boolean;
  saveInProgress: boolean;
};

export default class Image extends Component<void, Props, State> {
  props: Props;

  state: State = {
    editMode: false,
    deleteMode: false,
    saveInProgress: false,
    disabled: false,
  };

  @autobind
  handleEditImage(): void {
    const { disabled } = this.props;

    if (!disabled) {
      this.setState({ editMode: true });
    }
  }

  @autobind
  handleCancelEditImage(): void {
    this.setState({ editMode: false });
  }

  @autobind
  handleConfirmEditImage(form: ImageInfo): void {
    this.setState({ saveInProgress: true });
    this.props.editImage(form).then(() => this.setState({ editMode: false, saveInProgress: false }));
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

  @autobind
  handleRemove(): void {
    this.setState({ editMode: false, deleteMode: true });
  }

  get deleteImageDialog(): ?Element<*> {
    if (!this.state.deleteMode) {
      return;
    }

    return (
      <BodyPortal className={styles.modal}>
        <ConfirmationDialog
          isVisible={true}
          header='Delete Media'
          body='Are you sure you want to delete this asset?'
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
          onRemove={this.handleRemove}
          inProgress={this.state.saveInProgress}
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
    const { disabled } = this.props;

    return (
      <div>
        {this.editImageDialog}
        {this.deleteImageDialog}
        <ImageCard
          id={image.id}
          src={image.src}
          title={image.title}
          secondaryTitle={`Uploaded ${image.createdAt || moment().format('MM/DD/YYYY HH: mm')}`}
          actions={this.getImageActions()}
          onImageClick={this.handleEditImage}
          loading={image.loading}
          key={imagePid}
        />
      </div>
    );
  }
}
