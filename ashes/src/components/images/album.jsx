/* @flow */

// styles
import styles from './images.css';

// libs
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import ContentBox from 'components/content-box/content-box';
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
  editImage: (idx: number, info: ImageInfo) => Promise<*>;
  deleteImage: (idx: number) => Promise<*>;
  editAlbum: (album: TAlbum) => Promise<*>;
  moveAlbum: (position: number) => Promise<*>;
  archiveAlbum: (id: number) => Promise<*>;
  fetchAlbums: () => Promise<*>;
};

type State = {
  editMode: boolean;
  archiveMode: boolean;
  uploadMode: boolean;
};

export default class Album extends Component {
  props: Props;

  static defaultProps = {
    loading: false,
  };

  state: State = {
    editMode: false,
    archiveMode: false,
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
    this._uploadRef.openUploadDialog();
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
  handleCancelUpload(): void {
    this.setState({ uploadMode: false });
  }

  handleConfirmUpload(url): void {
    this.props.uploadByUrl({ ...this.props.album, url })
      .then(this.handleCancelEditAlbum);
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
      .then(this.props.fetchAlbums);

    this.setState({ archiveMode: false });
  }

  @autobind
  handleSortImages(order: Array<number>): Promise<*> {
    const album = { ...this.props.album };

    const newOrder = [];

    order.forEach((pos: number) => {
      newOrder.push(album.images[pos]);
    });

    album.images = newOrder;

    return this.props.editAlbum(album);
  }

  @autobind
  handleMove(direction: number): Promise<*> {
    const position = this.props.position + direction;

    return this.props.moveAlbum(position);
  }

  get editAlbumDialog(): ?Element<*> {
    const { album, loading } = this.props;

    return (
      <EditAlbum
        className={styles.modal}
        isVisible={this.state.editMode}
        album={album}
        loading={loading}
        onCancel={this.handleCancelEditAlbum}
        onSave={this.handleConfirmEditAlbum}
      />
    );
  }

  get editAlbumDialog(): ?Element<*> {
    const { album, loading } = this.props;

    return (
      <UploadByUrl
        className={styles.modal}
        isVisible={this.state.uploadMode}
        loading={loading}
        onCancel={this.handleCancelUpload}
        onSave={this.handleConfirmUpload}
      />
    );
  }

  get archiveAlbumDialog(): ?Element<*> {
    const album = this.props.album;

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
        focus
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
    const { album, position, albumsCount, loading } = this.props;

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
