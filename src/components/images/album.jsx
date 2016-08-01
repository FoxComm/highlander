/* @flow */

// styles
import styles from './images.css';

// libs
import { autobind, debounce } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import ConfirmationDialog from '../modal/confirmation-dialog';
import Alert from '../alerts/alert';
import Accordion from './accordion/accordion';
import EditAlbum from './edit-album';
import Upload from '../upload/upload';
import SortableTiles from '../sortable/sortable-tiles';
import Image from './image';

// types
import type { TAlbum, ImageFile, ImageInfo } from '../../modules/images';

export type Props = {
  album: TAlbum,
  loading: boolean,
  upload: (files: Array<ImageFile>) => Promise,
  editImage: (idx: number, info: ImageInfo) => Promise,
  deleteImage: (idx: number) => Promise,
  addAlbum: (album: TAlbum) => Promise,
  editAlbum: (album: TAlbum) => Promise,
  archiveAlbum: () => Promise,
  fetchAlbums: () => Promise,
};

type State = {
  editMode: boolean,
  archiveMode: boolean,
};

export default class Album extends Component {
  static props: Props;

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
  handleSort(order: Array<number>) {
    const album = { ...this.props.album };

    const newOrder = [];

    order.forEach((pos: number) => {
      newOrder.push(album.images[pos]);
    });

    album.images = newOrder;

    this.props.editAlbum(album);
  }

  get editAlbumDialog(): ?Element {
    const { album, loading } = this.props;

    return (
      <EditAlbum className={styles.modal}
                 isVisible={this.state.editMode}
                 album={album}
                 loading={loading}
                 onCancel={this.handleCancelEditAlbum}
                 onSave={this.handleConfirmEditAlbum}
      />
    );
  }

  get archiveAlbumDialog(): ?Element {
    const album = this.props.album;

    const body = (
      <div>
        <Alert type="warning">
          Archiving this album will delete <strong>{album.images.length} images</strong> from the product.
        </Alert>
        <span>
          Are you sure you want to archive <strong>{album.name}</strong> album?
        </span>
      </div>
    );

    return (
      <ConfirmationDialog className={styles.modal}
                          isVisible={this.state.archiveMode}
                          header='Archive Album'
                          body={body}
                          cancel='Cancel'
                          confirm='Yes, Archive'
                          cancelAction={this.handleCancelArchiveAlbum}
                          confirmAction={this.handleConfirmArchiveAlbum}
      />
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

  render(): Element {
    const { album, loading } = this.props;

    const accordionContent = (
      <Upload
        ref={c => this._uploadRef = c}
        className={styles.upload}
        onDrop={this.handleNewFiles}
        empty={album.images.length == 0}
      >
        <SortableTiles itemWidth={298}
                       itemHeight={372}
                       gutter={10}
                       gutterY={40}
                       loading={loading}
                       onSort={this.handleSort}
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
        <Accordion title={album.name}
                   titleWrapper={(title: string) => this.renderTitle(title, album.images.length)}
                   placeholder="Album Name"
                   open={true}
                   loading={loading}
                   onEditComplete={this.handleConfirmEditAlbum}
                   onEditCancel={this.handleCancelEditAlbum}
                   contentClassName={styles.accordionContent}
                   actions={this.getAlbumActions()}
                   key={album.name}
        >
          {accordionContent}
        </Accordion>
      </div>
    );
  }

  @autobind
  renderTitle(title: string, count: number): Element {
    return (
      <span>
        <span className={styles.accordionTitleText}>{title}</span>
        <span className={styles.accordionTitleCount}>{count}</span>
      </span>
    );
  }
}
