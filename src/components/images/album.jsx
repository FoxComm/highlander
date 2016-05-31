/* @flow */

// styles
import styles from './images.css';

// libs
import classNames from 'classnames';
import { autobind, debounce } from 'core-decorators';
import React, { Component, Element } from 'react';
import moment from 'moment';

// components
import BodyPortal from '../body-portal/body-portal';
import ConfirmationDialog from '../modal/confirmation-dialog';
import Alert from '../alerts/alert';
import Accordion from './accordion/accordion';
import Upload from '../upload/upload';
import SortableTiles from '../sortable/sortable-tiles';
import ImageCard from '../image-card/image-card';
import Image from './image';

// types
import type { TAlbum, ImageFile, ImageInfo } from '../../modules/images';

export type Props = {
  album: TAlbum;
  addMode: boolean;
  loading: boolean;
  upload: (files: Array<ImageFile>) => Promise;
  editImage: (idx: number, info: ImageInfo) => Promise;
  deleteImage: (idx: number) => Promise;
  addAlbum: (album: TAlbum) => Promise;
  editAlbum: (album: TAlbum) => Promise;
  deleteAlbum: () => Promise;
};

type State = {
  files: Array<ImageFile>;
  addMode: boolean;
  editMode: boolean;
  deleteMode: boolean;
}

export default class Album extends Component {
  static props: Props;

  static defaultProps = {
    addMode: false,
    loading: false,
  };

  state: State = {
    files: [],
    addMode: this.props.addMode,
    editMode: false,
    deleteMode: false,
  };

  componentWillReceiveProps(nextProps: Props): void {
    if (this.props.uploadImagesInProgress && !nextProps.uploadImagesInProgress) {
      this.setState({
        files: [],
      });
    }
  }

  @autobind
  handleNewFiles(images: Array<ImageFile>): void {
    const newImages = images.map((file: ImageFile) => ({
      title: file.file.name,
      alt: file.file.name,
      src: file.src,
      file: file.file,
      loading: true,
    }));

    this.props.upload(newImages);
  }

  @autobind
  handleAddImages() {
    this.setState({ addMode: !this.state.addMode });
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
    if (!name.length) {
      return this.handleCancelEditAlbum();
    }

    if (this.state.newAlbumMode) {
      this.props.addAlbum({ name, images: [] }).then(this.handleCancelEditAlbum);
    } else {
      const album = { ...this.props.album, name };

      this.props.editAlbum(album).then(this.handleCancelEditAlbum);
    }
  }

  @autobind
  handleDeleteAlbum(): void {
    this.setState({ deleteMode: true });
  }

  @autobind
  handleCancelDeleteAlbum(): void {
    this.setState({ deleteMode: false });
  }

  @autobind
  handleConfirmDeleteAlbum(): void {
    this.props.deleteAlbum(this.props.album.id);

    this.setState({ deleteMode: false });
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

  get deleteAlbumDialog(): ?Element {
    if (!this.state.deleteMode) {
      return;
    }

    const album = this.props.album;

    const body = (
      <div>
        <Alert type="warning">
          Deleting this album will delete <strong>{album.images.length} images</strong> from the product.
        </Alert>
        <span>
          Are you sure you want to delete <strong>{album.name}</strong> album?
        </span>
      </div>
    );

    return (
      <BodyPortal className={styles.modal}>
        <ConfirmationDialog isVisible={true}
                            header='Delete Album'
                            body={body}
                            cancel='Cancel'
                            confirm='Yes, Delete'
                            cancelAction={this.handleCancelDeleteAlbum}
                            confirmAction={this.handleConfirmDeleteAlbum}
        />
      </BodyPortal>
    );
  }

  @autobind
  getAlbumActions(): Array<any> {
    return [
      { name: 'add', handler: () => this.handleAddImages() },
      { name: 'edit', handler: () => this.handleEditAlbum() },
      { name: 'trash', handler: () => this.handleDeleteAlbum() },
    ];
  }

  render(): Element {
    const { album, loading } = this.props;
    const { addMode, editMode } = this.state;

    let accordionContent;

    if (addMode) {
      const files = album ? [...album.images, ...this.state.files] : this.state.files;

      accordionContent = (
        <Upload className={styles.upload} onDrop={this.handleNewFiles}>
          {files.map((image: ImageFile, idx: number) => {
            return (
              <ImageCard src={image.src}
                         key={`${image.src}-${idx}`}
                         title={image.title}
                         loading={image.loading}
                         secondaryTitle={`Uploaded ${moment().format('MM/DD/YYYY HH: mm')}`}
              />
            );
          })}
        </Upload>
      );
    } else {
      accordionContent = (
        <SortableTiles itemWidth={298}
                       itemHeight={372}
                       gutter={10}
                       gutterY={40}
                       loading={loading}
                       onSort={this.handleSort}
        >
          {album.images.map((image: ImageFile, idx: number) => {
            return (
              <Image image={image}
                     idx={idx}
                     editImage={(form: ImageInfo) => this.props.editImage(idx, form)}
                     deleteImage={() => this.props.deleteImage(idx)}
                     key={`${image.src}` /** replace with id*/}
              />
            );
          })}
        </SortableTiles>
      );
    }

    return (
      <div>
        {this.deleteAlbumDialog}
        <Accordion className={classNames({ [styles.addImages] : addMode })}
                   title={album.name}
                   titleWrapper={(title: string) => this.renderTitle(title, album.images.length)}
                   placeholder="Album Name"
                   open={addMode || album.images.length}
                   loading={loading}
                   editMode={editMode}
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
