/* @flow */

// parent: `./album`

// styles
import styles from './images.css';

// libs
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import BodyPortal from 'components/body-portal/body-portal';
import ConfirmationModal from 'components/core/confirmation-modal';
import ImageCard from 'components/image-card/image-card';
import EditImage from './edit-image';

// types
import type { ImageFile, ImageInfo } from '../../modules/images';
import type { Action } from '../image-card/image-card';

export type Props = {
  image: ImageFile;
  editImage: (info: ImageInfo) => Promise<*>;
  deleteImage: () => Promise<*>;
  imagePid: string|number;
  editAlbumState?: AsyncState;
  disabled?: boolean;
};

type State = {
  editMode: boolean;
  deleteMode: boolean;
};

export default class Image extends Component<void, Props, State> {
  props: Props;

  state: State = {
    editMode: false,
    deleteMode: false,
    disabled: false,
  };

  componentDidMount(): void {
    document.addEventListener('keydown', this.handleKeyDown);
  }

  componentWillUnmount() {
    document.removeEventListener('keydown', this.handleKeyDown);
  }

  @autobind
  handleKeyDown({ key }: KeyboardEvent) {
    if (key === 'Escape') {
      this.handleCancelEditImage();
    }
  }

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
    this.props.editImage(form)
      .then(() => this.setState({ editMode: false }));
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
    // We don't need to set `deleteMode: false`, because component will be removed
  }

  @autobind
  handleRemove(): void {
    this.setState({ editMode: false, deleteMode: true });
  }

  get deleteImageDialog(): ?Element<*> {
    const { editAlbumState } = this.props;

    if (!this.state.deleteMode) {
      return;
    }

    return (
      <BodyPortal className={styles.modal}>
        <ConfirmationModal
          isVisible={true}
          title='Delete Media'
          label='Are you sure you want to delete this asset?'
          confirmLabel='Yes, Delete'
          onCancel={this.handleCancelDeleteImage}
          onConfirm={this.handleConfirmDeleteImage}
          asyncState={editAlbumState}
          focus
        />
      </BodyPortal>
    );
  }

  get editImageDialog(): ?Element<*> {
    const { editAlbumState = {} } = this.props;

    if (!this.state.editMode) {
      return null;
    }

    return (
      <BodyPortal className={styles.modal}>
        <EditImage
          isVisible={true}
          image={this.props.image}
          onCancel={this.handleCancelEditImage}
          onSave={this.handleConfirmEditImage}
          onRemove={this.handleRemove}
          inProgress={editAlbumState.inProgress}
          error={editAlbumState.err}
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
    const { image, imagePid, disabled } = this.props;

    return (
      <div>
        {this.editImageDialog}
        {this.deleteImageDialog}
        <ImageCard
          id={image.id}
          src={image.src}
          actions={this.getImageActions()}
          onImageClick={this.handleEditImage}
          loading={image.loading}
          failed={image.failed}
          key={imagePid}
          disabled={disabled}
        />
      </div>
    );
  }
}
