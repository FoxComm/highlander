/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { connect } from 'react-redux';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import moment from 'moment';

import { actions } from '../../modules/products/images';
import { Image, ImageInfo } from '../../modules/images';

// components
import WaitAnimation from '../common/wait-animation';
import ConfirmationDialog from '../modal/confirmation-dialog';
import { AddButton } from '../common/buttons';
import Accordion from './accordion/accordion';
import ImageCard from './image-card/image-card';
import Upload from './upload/upload';
import EditImage from './edit-image';
import ActionsDropdown from '../bulk-actions/actions-dropdown';
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';

// helpers
import { getProductAttributes, setProductAttribute } from '../../paragons/product';

// types
import type {
  FullProduct,
} from '../../modules/products/details';

type Props = {
  product: FullProduct;
  fetch: (id: number|string) => void;
  editImage: (album: string, idx: number, info: any) => void;
  deleteImage: (album: string, idx: number) => void;
  list: any;
  isLoading: boolean;
  isImageLoading: (idx: number) => boolean;
};

type State = {
  files: Array<any>;
  isEditDialogVisible: boolean;
  isDeleteDialogVisible: boolean;
  selectedImage?: SelectedImage;
}

type SelectedImage = {
  idx: number;
  image: Image;
}

class ProductImages extends Component<void, Props, State> {
  static props: Props;

  state: State = {
    files: [],
    isEditDialogVisible: false,
    isDeleteDialogVisible: false,
    selectedImage: void 0,
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
      isEditDialogVisible: true,
    });
  }

  @autobind
  handleCancelEditImage() {
    this.setState({
      selectedImage: void 0,
      isEditDialogVisible: false,
    });
  }

  @autobind
  handleRemoveImage(selectedImage: SelectedImage) {
    this.setState({
      selectedImage,
      isDeleteDialogVisible: true,
    });
  }

  @autobind
  handleCancelRemoveImage() {
    this.setState({
      selectedImage: void 0,
      isDeleteDialogVisible: false,
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

  get deleteDialog() {
    return (
      <ConfirmationDialog
        isVisible={this.state.isDeleteDialogVisible}
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
            { isDeleteDialogVisible: false },
            () => this.props.deleteImage(image.album, idx)
          );
        }} />
    );
  }

  get editDialog(): ?Element {
    return (
      <EditImage
        isVisible={this.state.isEditDialogVisible}
        title={_.get(this.state, ['selectedImage', 'image', 'title'], '')}
        alt={_.get(this.state, ['selectedImage', 'image', 'alt'], '')}
        onCancel={this.handleCancelEditImage}
        onSave={(form: ImageInfo) => {
          if (!this.state.selectedImage) {
            return;
          }

          const { image, idx } = this.state.selectedImage;

          this.setState(
            { isEditDialogVisible: false },
            () => this.props.editImage(image.album, idx, form)
          );
        }} />
    );
  }

  @autobind
  getImageActions(selectedImage: SelectedImage): Array<any> {
    return [
      { name: 'external-link', handler: () => window.open(selectedImage.image.src)},
      { name: 'edit', handler: () => this.handleEditImage(selectedImage) },
      { name: 'trash', handler: () => this.handleRemoveImage(selectedImage) },
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
        {this.editDialog}
        {this.deleteDialog}
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
          <Accordion title={this.renderTitle(albumName, images.length)}>
            {images.map((image: Image, idx: number) => {
              return <ImageCard src={`${image.src}`}
                                title={image.title}
                                secondaryTitle={image.du}
                                actions={this.getImageActions({image, idx})}
                                loading={image.inProgress}
                                key={image.id} />;
            })}
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
