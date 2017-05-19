/* @flow */

// parent: `./images`

// styles
import styles from './images.css';

// libs
import { autobind } from 'core-decorators';
import React, { Component, Element, PropTypes } from 'react';
import { isEqual } from 'lodash';

// components
import ConfirmationDialog from 'components/modal/confirmation-dialog';
import Alert from 'components/alerts/alert';
import AlbumWrapper from './album-wrapper/album-wrapper';
import EditAlbum from './edit-album';
import Upload from 'components/upload/upload';
import SortableTiles from 'components/sortable/sortable-tiles';
import Image from './image';
import UploadByUrl from './upload-by-url';

// types
import type { Album as TAlbum, ImageFile, ImageInfo } from '../../modules/images';

export type Props = {
  album: TAlbum;
  loading: boolean;
  position: number;
  albumsCount: number;
  uploadFiles: (files: Array<ImageFile>) => Promise<*>;
  uploadByUrl: (idx: number, url: string) => Promise<*>;
  editImage: (idx: number, info: ImageInfo) => Promise<*>;
  deleteImage: (idx: number) => Promise<*>;
  editAlbum: (album: TAlbum) => Promise<*>;
  moveAlbum: (position: number) => Promise<*>;
  archiveAlbum: (id: number) => Promise<*>;
  fetchAlbums: () => Promise<*>;
  editAlbumState?: AsyncState;
  uploadMediaByUrlState?: AsyncState;
  archiveAlbumState?: AsyncState;
};

type State = {
  editMode: boolean;
  archiveMode: boolean;
  uploadUrlMode: boolean;
};

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
  idsToKey: { [key:any]: string };

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
    this.setState({ uploadUrlMode: false });
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

  @autobind
  handleMove(direction: number): Promise<*> {
    const position = this.props.position + direction;

    return this.props.moveAlbum(position);
  }

  get editAlbumDialog(): ?Element<*> {
    const { album, editAlbumState = {} } = this.props;

    return (
      <EditAlbum
        className={styles.modal}
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
        className={styles.modal}
        isVisible={this.state.uploadUrlMode}
        inProgress={uploadMediaByUrlState.inProgress}
        error={uploadMediaByUrlState.err}
        onCancel={this.handleCancelUrlUpload}
        onSave={this.handleConfirmUrlUpload}
      />
    );
  }

  get archiveAlbumDialog(): ?Element<*> {
    const { album, archiveAlbumState } = this.props;

    const body = (
      <div>
        <Alert type="warning">
          Archiving this album will remove <strong>{album.images.length} images</strong> from the product.
          <strong> This action cannot be undone</strong>
        </Alert>
        <span>
          Are you sure you want to archive <strong>{album.name}</strong> album?
        </span>
      </div>
    );

    return (
      <ConfirmationDialog
        className={styles.modal}
        isVisible={this.state.archiveMode}
        header='Archive Album'
        body={body}
        cancel='Cancel'
        confirm='Yes, Archive'
        onCancel={this.handleCancelArchiveAlbum}
        confirmAction={this.handleConfirmArchiveAlbum}
        asyncState={archiveAlbumState}
        focusCancel
      />
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
    const { album, position, albumsCount, loading, editAlbumState } = this.props;

    const albumContent = (
      <Upload
        ref={c => this._uploadRef = c}
        className={styles.upload}
        onDrop={this.handleNewFiles}
        empty={album.images.length == 0}
      >
        <SortableTiles loading={loading} onSort={this.handleSortImages}>
          {album.images.map((image: ImageFile, idx: number) => {
            if (image.key && image.id) this.idsToKey[image.id] = image.key;
            const imagePid = image.key || this.idsToKey[image.id] || image.id;

            const func = (disabled) =>
              <Image
                image={image}
                imagePid={imagePid}
                editImage={(form: ImageInfo) => this.props.editImage(idx, form)}
                deleteImage={() => this.props.deleteImage(idx)}
                key={imagePid}
                disabled={disabled}
                editAlbumState={editAlbumState}
              />;

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
          position={position}
          albumsCount={albumsCount}
          contentClassName={styles.albumContent}
          onSort={this.handleMove}
          actions={this.getAlbumActions()}
          onAddFile={this.handleAddFiles}
          onAddUrl={this.handleAddUrl}
        >
          {albumContent}
        </AlbumWrapper>
      </div>
    );
  }

  @autobind
  renderTitle(title: string, count: number): Element<*> {
    return <span className={styles.albumTitleText}>{title}</span>;
  }
}
