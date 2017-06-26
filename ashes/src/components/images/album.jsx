/* @flow */

// parent: `./images`

// libs
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import { isEmpty, isEqual, get } from 'lodash';

// components
import ConfirmationModal from 'components/core/confirmation-modal';
import Alert from 'components/core/alert';
import AlbumWrapper from './album-wrapper/album-wrapper';
import EditAlbumModal from './edit-album';
import Upload from 'components/upload/upload';
import SortableTiles from 'components/sortable/sortable-tiles';
import Image from './image';
import UploadByUrl from './upload-by-url';

// styles
import s from './images.css';

// types
import type { Album as TAlbum, ImageFile, ImageInfo } from '../../modules/images';

export type Props = {|
  album: TAlbum;
  loading: boolean;
  clearErrors: () => void;
  uploadFiles: (files: Array<ImageFile>) => Promise<*>;
  uploadByUrl: (idx: number, url: string) => Promise<*>;
  editImage: (idx: number, info: ImageInfo) => Promise<*>;
  deleteImage: (idx: number) => Promise<*>;
  editAlbum: (album: TAlbum) => Promise<*>;
  archiveAlbum: (id: number) => Promise<*>;
  fetchAlbums: () => Promise<*>;
  editAlbumState?: AsyncState;
  uploadMediaState?: AsyncState;
  uploadMediaByUrlState?: AsyncState;
  archiveAlbumState?: AsyncState;
|};

type State = {|
  editMode: boolean;
  archiveMode: boolean;
  uploadUrlMode: boolean;
|};

const INVALID_TYPE_RESPONSE = 'image format';

function getErrorMessage(asyncState?: Object, failedCount: number = 1) {
  const errMsg = get(asyncState, 'err.response.body.errors[0]') || get(asyncState, 'err.message', '');
  let message;

  if (isEmpty(errMsg)) {
    return null;
  }

  if (errMsg.indexOf(INVALID_TYPE_RESPONSE) > -1) {
    message = (
      <span>
        Oops! That’s not a valid file type. Valid file types are <b>jpg</b>, <b>jpeg</b>, <b>gif</b>, and <b>png</b>.
      </span>
    );
  } else if (errMsg === 'Payload Too Large') {
    message = 'Oops! Your file size is too big. Files can be no larger than 15 MB.';
  } else {
    const postfix = failedCount === 1 ? 'an image' : `${failedCount} images`;
    message = `Oops! Looks like we were unable to upload ${postfix}. Please try again.`;
  }

  return message;
}

export default class Album extends Component {
  props: Props;

  static defaultProps = {
    loading: false,
  };

  state: State = {
    editMode: false,
    archiveMode: false,
    uploadUrlMode: false,
  };

  _uploadRef: Upload;
  idsToKey: { [key: any]: string };

  constructor(...args: Array<any>) {
    super(...args);
    this.idsToKey = {};
  }

  @autobind
  handleNewFiles(images: Array<ImageFile>): void {
    const newImages = images.map((file: ImageFile) => ({
      title: file.file.name,
      alt: file.file.name,
      src: file.src,
      file: file.file,
      key: file.key,
      loading: true,
    }));

    this.props.uploadFiles(newImages);
  }

  @autobind
  handleAddFiles(): void {
    this._uploadRef.openUploadDialog();
  }

  @autobind
  handleAddUrl(): void {
    this.setState({ uploadUrlMode: true });
  }

  @autobind
  handleEditAlbum(): void {
    this.setState({ editMode: true });
  }

  @autobind
  handleCancelEditAlbum(): void {
    this.setState({ editMode: false });
  }

  @autobind
  handleConfirmEditAlbum(name: string): void {
    this.props.editAlbum({ ...this.props.album, name })
      .then(this.handleCancelEditAlbum);
  }

  @autobind
  handleCancelUrlUpload(): void {
    this.setState({ uploadUrlMode: false }, this.props.clearErrors);
  }

  @autobind
  handleConfirmUrlUpload(url: string): void {
    this.props.uploadByUrl(this.props.album.id, url)
      .then(this.handleCancelUrlUpload);
  }

  @autobind
  handleArchiveAlbum(): void {
    this.setState({ archiveMode: true });
  }

  @autobind
  handleCancelArchiveAlbum(): void {
    this.setState({ archiveMode: false });
  }

  @autobind
  handleConfirmArchiveAlbum(): void {
    this.props.archiveAlbum(this.props.album.id)
      .then(() => this.setState({ archiveMode: false }))
      .then(this.props.fetchAlbums);
  }

  @autobind
  handleSortImages(order: Array<number>): Promise<*> {
    const album = { ...this.props.album };

    const nextImages = [];

    order.forEach((pos: number) => {
      nextImages.push(album.images[pos]);
    });

    // If nothing changed, dont edit
    if (isEqual(album.images, nextImages)) {
      return Promise.resolve(false);
    }

    album.images = nextImages;

    return this.props.editAlbum(album);
  }

  get errorMsg(): ?Element<*> {
    const { album, uploadMediaState } = this.props;

    if (!album.failedImagesCount || !get(uploadMediaState, 'err')) {
      return null;
    }

    const errorMessage = getErrorMessage(this.props.uploadMediaState, this.props.album.failedImagesCount);

    return (
      <Alert className={s.alert} type="error">{errorMessage}</Alert>
    );
  }

  get editAlbumDialog(): ?Element<*> {
    const { album, editAlbumState = {} } = this.props;

    return (
      <EditAlbumModal
        className={s.modal}
        isVisible={this.state.editMode}
        album={album}
        inProgress={editAlbumState.inProgress}
        error={editAlbumState.err}
        onCancel={this.handleCancelEditAlbum}
        onSave={this.handleConfirmEditAlbum}
      />
    );
  }

  get uploadByUrlDialog(): ?Element<*> {
    const { uploadMediaByUrlState = {} } = this.props;

    return (
      <UploadByUrl
        isVisible={this.state.uploadUrlMode}
        inProgress={uploadMediaByUrlState.inProgress}
        error={getErrorMessage(uploadMediaByUrlState)}
        onCancel={this.handleCancelUrlUpload}
        onSave={this.handleConfirmUrlUpload}
      />
    );
  }

  get archiveAlbumDialog(): ?Element<*> {
    const { album, archiveAlbumState } = this.props;

    return (
      <ConfirmationModal
        className={s.modal}
        isVisible={this.state.archiveMode}
        title='Archive Album'
        confirmLabel='Yes, Archive'
        onCancel={this.handleCancelArchiveAlbum}
        onConfirm={this.handleConfirmArchiveAlbum}
        asyncState={archiveAlbumState}
        focusCancel
      >
        <Alert type={Alert.WARNING}>
          Archiving this album will remove <strong>{album.images.length} images</strong> from the product.
          <strong>This action cannot be undone</strong>.
        </Alert>
        <div className={s.confirmRemoveText}>
          Are you sure you want to archive <strong>{album.name} images</strong> album?
        </div>
      </ConfirmationModal>
    );
  }

  @autobind
  getAlbumActions(): Array<any> {
    return [
      { name: 'edit', handler: this.handleEditAlbum },
      { name: 'trash', handler: this.handleArchiveAlbum },
    ];
  }

  render() {
    const { album, loading, editAlbumState } = this.props;

    const albumContent = (
      <Upload
        ref={c => this._uploadRef = c}
        className={s.upload}
        onDrop={this.handleNewFiles}
        empty={album.images.length === 0}
      >
        <SortableTiles loading={loading} onSort={this.handleSortImages}>
          {album.images.map((image: ImageFile, idx: number) => {
            if (image.key && image.id) this.idsToKey[image.id] = image.key;
            const imagePid = image.key || this.idsToKey[image.id] || image.id;

            const func = (disabled) => (
              <Image
                image={image}
                imagePid={imagePid}
                editImage={(form: ImageInfo) => this.props.editImage(idx, form)}
                deleteImage={() => this.props.deleteImage(idx)}
                key={imagePid}
                disabled={disabled}
                editAlbumState={editAlbumState}
              />
            );

            func.key = imagePid;

            return func;
          })}
        </SortableTiles>
      </Upload>
    );

    return (
      <div>
        {this.editAlbumDialog}
        {this.uploadByUrlDialog}
        {this.archiveAlbumDialog}
        <AlbumWrapper
          title={album.name}
          titleWrapper={(title: string) => this.renderTitle(title, album.images.length)}
          contentClassName={s.albumContent}
          actions={this.getAlbumActions()}
          onAddFile={this.handleAddFiles}
          onAddUrl={this.handleAddUrl}
          alert={this.errorMsg}
        >
          {albumContent}
        </AlbumWrapper>
      </div>
    );
  }

  @autobind
  renderTitle(title: string, count: number): Element<*> {
    return <span className={s.albumTitleText}>{title}</span>;
  }
}
