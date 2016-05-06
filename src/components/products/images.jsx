/* @flow */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { connect } from 'react-redux';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import moment from 'moment';

import { actions } from '../../modules/products/images';

// components
import WaitAnimation from '../common/wait-animation';
import ConfirmationDialog from '../modal/confirmation-dialog';
import Alert from '../alerts/alert';
import { AddButton } from '../common/buttons';
import Accordion from './accordion/accordion';
import ImageCard from './image-card/image-card';
import Upload from './upload/upload';
import EditImage from './edit-image';
import ActionsDropdown from '../bulk-actions/actions-dropdown';
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';

import SortableTiles from './sortable/sortable-tiles';

// helpers
import { getProductAttributes, setProductAttribute } from '../../paragons/product';

// types
import type { Image, ImageInfo } from '../../modules/images';
import type { FullProduct } from '../../modules/products/details';

type Props = {
  product: FullProduct;
  fetch: (id: number|string) => void;
  editImage: (album: string, idx: number, info: any) => void;
  deleteImage: (album: string, idx: number) => void;
  editAlbum: (albumName: string, newName: string) => void;
  deleteAlbum: (album: string) => void;
  list: any;
  isLoading: boolean;
  isImageLoading: (idx: number) => boolean;
};

type State = {
  files: Array<any>;
  isEditImageDialogVisible: boolean;
  isDeleteImageDialogVisible: boolean;
  isEditAlbumVisible: boolean;
  isDeleteAlbumDialogVisible: boolean;
  selectedImage?: SelectedImage;
  selectedAlbum?: string;
}

type SelectedImage = {
  idx: number;
  image: Image;
}

class ProductImages extends Component<void, Props, State> {
  static props: Props;

  state: State = {
    files: [],
    isEditImageDialogVisible: false,
    isDeleteImageDialogVisible: false,
    isEditAlbumVisible: false,
    isDeleteAlbumDialogVisible: false,
    selectedImage: void 0,
    selectedAlbum: void 0,
  };

  componentDidMount() {
    const id = this.props.product.form.product.id;

    if (id) {
      this.props.fetch(id);
    }
  }

  @autobind
  handleEditImage(selectedImage: SelectedImage) {
    this.setState({
      selectedImage,
      isEditImageDialogVisible: true,
    });
  }

  @autobind
  handleCancelEditImage() {
    this.setState({
      selectedImage: void 0,
      isEditImageDialogVisible: false,
    });
  }

  @autobind
  handleRemoveImage(selectedImage: SelectedImage) {
    this.setState({
      selectedImage,
      isDeleteImageDialogVisible: true,
    });
  }

  @autobind
  handleCancelRemoveImage() {
    this.setState({
      selectedImage: void 0,
      isDeleteImageDialogVisible: false,
    });
  }

  @autobind
  handleEditAlbum(selectedAlbum: string) {
    this.setState({
      selectedAlbum,
      isEditAlbumVisible: true,
    });
  }

  @autobind
  handleCancelEditAlbum() {
    this.setState({
      selectedAlbum: void 0,
      isEditAlbumVisible: false,
    });
  }

  @autobind
  handleRemoveAlbum(selectedAlbum: string) {
    this.setState({
      selectedAlbum,
      isDeleteAlbumDialogVisible: true,
    });
  }

  @autobind
  handleCancelRemoveAlbum() {
    this.setState({
      selectedAlbum: void 0,
      isDeleteAlbumDialogVisible: false,
    });
  }

  get bulkActions(): Array<Array<any>> {
    return [
      ['Preview in a new window', _.noop, '', ''],
      ['Move to a different album', _.noop, 'successfully moved', 'could not be moved'],
      ['Delete', _.noop, 'successfully deleted', 'could not be deleted'],
    ];
  }

  get emptyContainer(): Element {
    return (
      <div className="fc-product-details__upload-empty">
        <i className="icon-upload" /> Drag & Drop to upload or click here
      </div>
    );
  }

  get deleteImageDialog(): ?Element {
    if (!this.state.selectedImage) {
      return;
    }

    return (
      <ConfirmationDialog
        isVisible={this.state.isDeleteImageDialogVisible}
        header='Delete Image'
        body={'Are you sure you want to delete this image?'}
        cancel='Cancel'
        confirm='Yes, Delete'
        cancelAction={this.handleCancelRemoveImage}
        confirmAction={() => {
          if (!this.state.selectedImage) {
            return;
          }
          const { image, idx } = this.state.selectedImage;

          this.setState(
            {
              isDeleteImageDialogVisible: false,
              // selectedImage: void 0,
             },
            () => this.props.deleteImage(image.album, idx)
          );
        }} />
    );
  }

  get editImageDialog(): ?Element {
    if (!this.state.selectedImage) {
      return;
    }

    return (
      <EditImage
        isVisible={this.state.isEditImageDialogVisible}
        title={_.get(this.state, ['selectedImage', 'image', 'title'], '')}
        alt={_.get(this.state, ['selectedImage', 'image', 'alt'], '')}
        onCancel={this.handleCancelEditImage}
        onSave={(form: ImageInfo) => {
          if (!this.state.selectedImage) {
            return;
          }

          const { image, idx } = this.state.selectedImage;

          this.setState(
            { isEditImageDialogVisible: false },
            () => this.props.editImage(image.album, idx, form)
          );
        }} />
    );
  }

  get deleteAlbumDialog(): ?Element {
    const { selectedAlbum } = this.state;

    if (!selectedAlbum) {
      return;
    }

    const imageCount = this.props.list[selectedAlbum].length;
    const body = (
      <div>
        <Alert type="warning">
          Deleting this album will delete <strong>{imageCount} images</strong> from the product.
        </Alert>
        <span>
          Are you sure you want to delete <strong>{selectedAlbum}</strong> album?
        </span>
      </div>
    );

    return (
      <ConfirmationDialog
        isVisible={this.state.isDeleteAlbumDialogVisible}
        header='Delete Album'
        body={body}
        cancel='Cancel'
        confirm='Yes, Delete'
        cancelAction={this.handleCancelRemoveAlbum}
        confirmAction={() => {
          if (!selectedAlbum) {
            return;
          }

          this.setState(
            {
              isDeleteAlbumDialogVisible: false,
              selectedAlbum: void 0,
            },
            () => this.props.deleteAlbum(selectedAlbum)
          );
        }} />
    );
  }

  @autobind
  getImageActions(selectedImage: SelectedImage): Array<any> {
    const actionsHandler = (handler: Function) => {
      return (e: MouseEvent) => {
        e.stopPropagation();
        handler();
      };
    };

    return [
      { name: 'external-link', handler: actionsHandler(() => window.open(selectedImage.image.src)) },
      { name: 'edit', handler: actionsHandler(() => this.handleEditImage(selectedImage)) },
      { name: 'trash', handler: actionsHandler(() => this.handleRemoveImage(selectedImage)) },
    ];
  }

  @autobind
  getAlbumActions(selectedAlbum: string): Array<any> {
    return [
      { name: 'edit', handler: () => this.handleEditAlbum(selectedAlbum) },
      { name: 'trash', handler: () => this.handleRemoveAlbum(selectedAlbum) },
    ];
  }

  @autobind
  onAddFile(res: any): void {
    const newFile = {
      id: guid(),
      name: res.file.name,
      size: res.file.size,
      altText: '',
      caption: '',
      file: res.file,
      url: res.imageUrl
    };

    this.setState({ files: [...this.state.files, newFile] });
  }

  render(): Element {
    if (this.props.isLoading) {
      return <WaitAnimation />;
    }

    return (
      <div>
        {this.editImageDialog}
        {this.deleteImageDialog}
        {this.deleteAlbumDialog}
        <div className="fc-table__header">
          <ActionsDropdown actions={this.bulkActions}
                           disabled={false}
                           allChecked={false}
                           toggledIds={[]}
                           total={0} />
          <AddButton>Album</AddButton>
        </div>
        {/**
         <div className="fc-grid fc-grid-no-gutter">
         <div className="fc-col-md-1-1">
         {this.contentBox}
         </div>
         </div>
         */}
        <div className="fc-grid fc-grid-no-gutter">
          <div className="fc-col-md-1-1">
            <Upload onDrop={this.onAddFile}>
              {this.state.files.length ?
                this.state.files.map((file, i) => {
                  return <ImageCard src={file.url} key={i}
                                    title={file.name}
                                    secondaryTitle={`Uploaded ${moment().format('MM/DD/YYYY HH: mm')}`} />;
                }) : this.emptyContainer}
            </Upload>
          </div>
        </div>
        {_.values(_.mapValues(this.props.list, this.renderAlbum))}
      </div>
    );
  }

  @autobind
  renderAlbum(images: Array<Image>, albumName: string): Element {
    return (
      <div className="fc-grid fc-grid-no-gutter" key={albumName}>
        <div className="fc-col-md-1-1">
          <Accordion
            title={albumName}
            titleWrapper={(title: string) => this.renderTitle(title, images.length)}
            placeholder="New album"
            editMode={this.state.isEditAlbumVisible && this.state.selectedAlbum == albumName}
            onEditComplete={(newTitle: string) => {
              this.setState({
                isEditAlbumVisible: false,
                selectedAlbum: void 0,
              }, this.props.editAlbum(albumName, newTitle));
            }}
            onEditCancel={() => this.setState({isEditAlbumVisible: false, selectedAlbum: void 0})}
            actions={this.getAlbumActions(albumName)}
          >
            <SortableTiles itemWidth={298} itemHeight={372} gutter={10}>
              {images.map((image: Image, idx: number) => {
                return (
                  <ImageCard
                    src={`${image.src}`}
                    title={image.title}
                    secondaryTitle={image.du}
                    actions={this.getImageActions({image, idx})}
                    loading={image.inProgress}
                    key={image.id}
                  />
                );
              })}
            </SortableTiles>
          </Accordion>
        </div>
      </div>
    );
  }

  @autobind
  renderTitle(title, count): Element {
    return (
      <span>
        <span className="fc-product-details__images-title-text">{title}</span>
        <span className="fc-product-details__images-title-count">{count}</span>
      </span>
    );
  }
}

const mapState = (state) => ({
  list: _.get(state, ['products', 'images', 'list'], []),
  albums: _.get(state, ['products', 'images', 'albums'], []),
  isLoading: _.get(state, ['asyncActions', 'productsFetchImages', 'inProgress'], true),
});

export default connect(mapState, actions)(ProductImages);

function guid(): string {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }

  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
    s4() + '-' + s4() + s4() + s4();
}
