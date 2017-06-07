/* @flow */

// libs
import { autobind, debounce } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import ConfirmationModal from 'components/core/confirmation-modal';
import Alert from 'components/core/alert';
import AlbumWrapper from './album-wrapper/album-wrapper';
import EditAlbumModal from './edit-album';
import Upload from '../upload/upload';
import SortableTiles from '../sortable/sortable-tiles';
import Image from './image';

// types
import type { Album as TAlbum, ImageFile, ImageInfo } from '../../modules/images';

// styles
import s from './images.css';

export type Props = {
  album: TAlbum;
  loading: boolean;
  position: number;
  albumsCount: number;
  upload: (files: Array<ImageFile>) => Promise<*>;
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

    this.props.upload(newImages);
  }

  @autobind
  handleAddImages() {
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
  @debounce(300)
  handleSortImages(order: Array<number>): void {
    const album = { ...this.props.album };

    const newOrder = [];

    order.forEach((pos: number) => {
      newOrder.push(album.images[pos]);
    });

    album.images = newOrder;

    this.props.editAlbum(album);
  }

  @autobind
  handleMove(direction: number): void {
    const position = this.props.position + direction;
    this.props.moveAlbum(position);
  }

  get editAlbumDialog(): ?Element<*> {
    const { album, loading } = this.props;

    return (
      <EditAlbumModal
        className={s.modal}
        isVisible={this.state.editMode}
        album={album}
        loading={loading}
        onCancel={this.handleCancelEditAlbum}
        onSave={this.handleConfirmEditAlbum}
      />
    );
  }

  get archiveAlbumDialog(): ?Element<*> {
    const album = this.props.album;

    return (
      <ConfirmationModal
        className={s.modal}
        isVisible={this.state.archiveMode}
        title='Archive Album'
        confirmLabel='Yes, Archive'
        onCancel={this.handleCancelArchiveAlbum}
        onConfirm={this.handleConfirmArchiveAlbum}
      >
        <Alert type={Alert.WARNING}>
          Archiving this album will remove <strong>{album.images.length} images</strong> from the product.
          <strong> This action cannot be undone</strong>
        </Alert>
        <p>Are you sure you want to archive <strong>{album.name}</strong> album?</p>
      </ConfirmationModal>
    );
  }

  @autobind
  getAlbumActions(): Array<any> {
    return [
      { name: 'add', handler: this.handleAddImages },
      { name: 'edit', handler: this.handleEditAlbum },
      { name: 'trash', handler: this.handleArchiveAlbum },
    ];
  }

  render() {
    const { album, position, albumsCount, loading } = this.props;

    const albumContent = (
      <Upload
        ref={c => this._uploadRef = c}
        className={s.upload}
        onDrop={this.handleNewFiles}
        empty={album.images.length == 0}
      >
        <SortableTiles
          itemWidth={298}
          itemHeight={372}
          gutter={10}
          gutterY={40}
          loading={loading}
          onSort={this.handleSortImages}
        >
          {album.images.map((image: ImageFile, idx: number) => {
            if (image.key && image.id) this.idsToKey[image.id] = image.key;
            const imagePid = image.key || this.idsToKey[image.id] || image.id;
            return (
              <Image
                image={image}
                imagePid={imagePid}
                editImage={(form: ImageInfo) => this.props.editImage(idx, form)}
                deleteImage={() => this.props.deleteImage(idx)}
                key={imagePid}
              />
            );
          })}
        </SortableTiles>
      </Upload>
    );

    return (
      <div>
        {this.editAlbumDialog}
        {this.archiveAlbumDialog}
        <AlbumWrapper
          title={album.name}
          titleWrapper={(title: string) => this.renderTitle(title, album.images.length)}
          position={position}
          albumsCount={albumsCount}
          contentClassName={s.albumContent}
          onSort={this.handleMove}
          actions={this.getAlbumActions()}
        >
          {albumContent}
        </AlbumWrapper>
      </div>
    );
  }

  @autobind
  renderTitle(title: string, count: number): Element<*> {
    return (
      <span>
        <span className={s.albumTitleText}>{title}</span>
        <span className={s.albumTitleCount}>{count}</span>
      </span>
    );
  }
}
