/* @flow */

// parent: `products/images` which just extends `object-page/object-images`

// styles
import styles from './images.css';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import WaitAnimation from '../common/wait-animation';
import { AddButton } from 'components/core/button';
import EditAlbum from './edit-album';
import Album from './album';

// types
import type { Album as TAlbum, NewAlbum, ImageInfo, ImageFile } from '../../modules/images';

export type Props = {
  entityId: number;
  context: string;
  albums: Array<TAlbum>;
  isLoading: boolean;
  failedImagesCount: number,
  clearErrors: () => void;
  uploadMedia: (context: string, albumId: number, files: Array<ImageFile>) => Promise<*>;
  uploadMediaByUrl: (context: string, albumId: number, url: string) => Promise<*>;
  editImage: (context: string, albumId: number, idx: number, info: ImageInfo) => Promise<*>;
  deleteImage: (context: string, albumId: number, idx: number) => Promise<*>;
  fetchAlbums: (context: string, entityId: number) => Promise<*>;
  addAlbum: (context: string, entityId: number, album: NewAlbum) => Promise<*>;
  editAlbum: (context: string, albumId: number, album: TAlbum) => Promise<*>;
  moveAlbum: (context: string, entityId: number, albumId: number, position: number) => Promise<*>;
  archiveAlbum: (context: string, albumId: number) => Promise<*>;
  asyncActionsState: {
    addAlbum?: AsyncState;
    editAlbum?: AsyncState;
    uploadMedia?: AsyncState;
    uploadMediaByUrl?: AsyncState;
    archiveAlbum?: AsyncState;
  };
};

type State = {
  newAlbumMode: boolean;
};

export default class Images extends Component {
  props: Props;

  state: State = {
    newAlbumMode: false,
  };

  @autobind
  handleAddAlbum() {
    this.setState({
      newAlbumMode: true,
    });
  }

  get bulkActions(): Array<Array<any>> {
    return [
      ['Preview in a new window', _.noop, '', ''],
      ['Move to a different album', _.noop, 'successfully moved', 'could not be moved'],
      ['Delete', _.noop, 'successfully deleted', 'could not be deleted'],
    ];
  }

  @autobind
  addNewAlbum(name: string): void {
    const { context, entityId } = this.props;

    this.props.addAlbum(context, entityId, { name, images: [] }).then(this.handleCancelEditAlbum);
  }

  @autobind
  handleCancelEditAlbum(): void {
    this.setState({ newAlbumMode: false });
  }

  get newAlbumDialog(): ?Element<*> {
    const album = { name: '', images: [] };
    const { addAlbum = {} } = this.props.asyncActionsState;

    return (
      <EditAlbum
        className={styles.modal}
        isVisible={this.state.newAlbumMode}
        album={album}
        inProgress={addAlbum.inProgress}
        onCancel={this.handleCancelEditAlbum}
        onSave={this.addNewAlbum}
        isNew={true}
      />
    );
  }

  render() {
    if (this.props.isLoading) {
      return <WaitAnimation />;
    }

    const { albums, context, entityId, failedImagesCount, asyncActionsState } = this.props;

    const inProgress = _.get(asyncActionsState, 'editAlbum.inProgress', false)
      || _.get(asyncActionsState, 'uploadMedia.inProgress', false)
      || _.get(asyncActionsState, 'uploadMediaByUrl.inProgress', false);

    return (
      <div className={styles.images}>
        {this.newAlbumDialog}
        <div className={styles.header}>
          <AddButton onClick={this.handleAddAlbum}>Album</AddButton>
        </div>
        {albums.map((album: TAlbum, i: number) => {
          return (
            <Album
              album={album}
              loading={inProgress}
              failedImagesCount={failedImagesCount}
              clearErrors={this.props.clearErrors}
              uploadFiles={(files: Array<ImageFile>) => this.props.uploadMedia(context, album.id, files)}
              uploadByUrl={(albumId, url) => this.props.uploadMediaByUrl(context, albumId, url)}
              editImage={(idx: number, form: ImageInfo) => this.props.editImage(context, album.id, idx, form)}
              deleteImage={(idx: number) => this.props.deleteImage(context, album.id, idx)}
              editAlbum={(album: TAlbum) => this.props.editAlbum(context, album.id, album)}
              moveAlbum={(position: number) => this.props.moveAlbum(context, entityId, album.id, position)}
              archiveAlbum={(id: number) => this.props.archiveAlbum(context, id)}
              position={i}
              albumsCount={albums.length}
              key={album.id}
              fetchAlbums={() => this.props.fetchAlbums(context, entityId)}
              editAlbumState={asyncActionsState.editAlbum}
              uploadMediaState={asyncActionsState.uploadMedia}
              uploadMediaByUrlState={asyncActionsState.uploadMediaByUrl}
              archiveAlbumState={asyncActionsState.archiveAlbum}
            />
          );
        })}
      </div>
    );
  }
}
