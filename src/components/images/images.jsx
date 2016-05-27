/* @flow */

// styles

import styles from './images.css';

// libs
import _ from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import WaitAnimation from '../common/wait-animation';
import { AddButton } from '../common/buttons';
import Accordion from './accordion/accordion';
import Upload from '../upload/upload';
import Album from './album';
import ActionsDropdown from '../bulk-actions/actions-dropdown';

// types
import type { TAlbum, ImageInfo, ImageFile } from '../../modules/images';

export type Props = {
  entityId: number;
  context: string;
  albums: Array<TAlbum>;
  isLoading: boolean;
  addAlbumInProgress: boolean;
  editAlbumInProgress: boolean;
  uploadImagesInProgress: boolean;
  isImageLoading: (idx:number) => boolean;

  uploadImages: (context: string, albumId: number, files: Array<ImageFile>) => Promise;
  editImage: (context: string, albumId: number, idx: number, info: ImageInfo) => Promise;
  deleteImage: (context: string, albumId: number, idx: number) => Promise;
  fetchAlbums: (context: string, entityId: number) => Promise;
  addAlbum: (context: string, entityId: number, album: TAlbum) => Promise;
  editAlbum: (context: string, albumId: number, album: TAlbum) => Promise;
  deleteAlbum: (context: string, albumId: number) => Promise;
};

type State = {
  files: Array<ImageFile>;
  newAlbumMode: boolean;
  newAlbum: ?number;
}

class Images extends Component {
  static props: Props;

  state: State = {
    files: [],
    newAlbumMode: false,
    newAlbum: void 0,
  };

  componentWillReceiveProps(nextProps: Props): void {
    if (this.props.uploadImagesInProgress && !nextProps.uploadImagesInProgress) {
      this.setState({
        files: [],
      });
    }

    if (this.props.albums.length < nextProps.albums.length) {
      this.setState({ newAlbum: _.get(nextProps.albums, [0, 'id']) });
    } else {
      this.setState({ newAlbum: void 0 });
    }
  }

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
  handleCompleteEditAlbum(name: string): void {
    const { context, entityId } = this.props;

    this.props.addAlbum(context, entityId, { name, images: [] }).then(this.handleCancelEditAlbum);
  }

  @autobind
  handleCancelEditAlbum(): void {
    this.setState({ newAlbumMode: false });
  }

  get dropzone() {
    const { newAlbumMode } = this.state;
    const { addAlbumInProgress } = this.props;

    if (!newAlbumMode && !addAlbumInProgress) {
      return;
    }

    return (
      <Accordion className={styles.addImages}
                 placeholder="New album"
                 editMode={newAlbumMode}
                 loading={addAlbumInProgress}
                 onEditComplete={this.handleCompleteEditAlbum}
                 onEditCancel={this.handleCancelEditAlbum}
      >
        <Upload className={classNames(styles.upload, styles.disabled)} />
      </Accordion>
    );
  }

  render(): Element {
    if (this.props.isLoading) {
      return <WaitAnimation />;
    }

    const { albums, editAlbumInProgress, context, entityId } = this.props;
    const { newAlbum } = this.state;

    return (
      <div>
        <div className={styles.header}>
          <ActionsDropdown actions={this.bulkActions}
                           disabled={false}
                           allChecked={false}
                           toggledIds={[]}
                           total={0}
          />
          <AddButton onClick={this.handleAddAlbum}>Album</AddButton>
        </div>
        {this.dropzone}
        {albums.map((album: TAlbum) => {
          return (
            <Album album={album}
                   addMode={newAlbum === album.id}
                   loading={editAlbumInProgress}
                   upload={(files: Array<ImageFile>) => this.props.uploadImages(context, album.id, files)}
                   editImage={(idx: number, form: ImageInfo) => this.props.editImage(context, album.id, idx, form)}
                   deleteImage={(idx: number) => this.props.deleteImage(context, album.id, idx)}
                   addAlbum={(album: TAlbum) => this.props.addAlbum(context, entityId, album)}
                   editAlbum={(album: TAlbum) => this.props.editAlbum(context, album.id, album)}
                   deleteAlbum={(id: number) => this.props.deleteAlbum(context, id)}
                   key={album.id}
            />
          );
        })}
      </div>
    );
  }
}

export default Images;
