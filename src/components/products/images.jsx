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
// components
import WaitAnimation from '../common/wait-animation';
import { FormField } from '../forms';
import ContentBox from '../content-box/content-box';
import { AddButton } from '../common/buttons';
import Accordion from './accordion/accordion';
import ImageCard from './image-card/image-card';
import Upload from './upload/upload';
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
  product: FullProduct,
  onUpdateProduct: (product: FullProduct) => void,
  onSetProperty: (field: string, type: string, value: any) => void,
};

type State = {
  files: Array<any>;
}

const acts = (image) => {
  return [
    { name: 'external-link', handler: _.noop },
    { name: 'edit', handler: _.noop },
    { name: 'trash', _handler: _.noop },
  ];
};

class ProductImages extends Component<void, Props, State> {
  static propTypes = {
    product: PropTypes.object.isRequired,
    onSetProperty: PropTypes.func.isRequired,
    onUpdateProduct: PropTypes.func.isRequired,
    fetch: PropTypes.func.isRequired,
    list: PropTypes.array.isRequired,
    isLoading: PropTypes.bool.isRequired,
  };

  state: State = {
    files: []
  };

  componentDidMount() {
    this.props.fetch(this.props.product.form.product.id);
  }

  get contentBox(): Element {
    const imageControls = _.map(this.images, (val, idx) => {
      return (
        <div className="fc-product-details__image" key={idx}>
          <FormField
            className="fc-product-details__field"
            key={`product-image-page-field-${idx}`}>
            <input
              className="fc-product-details__field-value"
              type="text"
              value={val}
              onChange={(e) => this.handleUpdateImage(idx, e)} />
          </FormField>
          <i className="icon-close" onClick={() => this.handleRemoveImage(idx)} />
        </div>
      );
    });

    return (
      <ContentBox title="Image URLs">
        {imageControls}
        <div className="fc-product-details__add-custom-property">
          New Image
          <a className="fc-product-details__add-custom-property-icon"
             onClick={this.handleAddImage}>
            <i className="icon-add" />
          </a>
        </div>
      </ContentBox>
    );
  }

  get images(): Array<?string> {
    const attributes = getProductAttributes(this.props.product);
    return _.get(attributes, 'images.value', [null]);
  }

  @autobind
  handleAddImage() {
    const newImages = [...this.images, null];
    this.updateImages(newImages);
  }

  @autobind
  handleRemoveImage(idx: number) {
    const images = [
      ...this.images.slice(0, idx),
      ...this.images.slice(idx + 1),
    ];

    this.props.onSetProperty('images', 'images', images);
  }

  @autobind
  handleUpdateImage(idx: number, event: Object) {
    const newImages = [
      ...this.images.slice(0, idx),
      event.target.value,
      ...this.images.slice(idx + 1),
    ];

    this.updateImages(newImages);
  }

  updateImages(images: Array<?string>) {
    const product = setProductAttribute(this.props.product, 'images', 'images', images);
    this.props.onUpdateProduct(product);
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

    const byAlbum = _.groupBy(this.props.list, 'album');

    return (
      <div>
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
        {_.values(_.mapValues(byAlbum, this.renderAlbum))}
      </div>
    );
  }

  @autobind
  renderAlbum(images, albumName): Element {
    return (
      <div className="fc-grid fc-grid-no-gutter">
        <div className="fc-col-md-1-1">
          <Accordion title={this.renderTitle(albumName, images.length)}>
            {images.map((image, i) => {
              return <ImageCard src={`${image.src}`} title={image.title} secondaryTitle={image.du}
                                actions={acts(image.src)} key={i} />;
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
  isLoading: _.get(state, ['asyncActions', 'images', 'inProgress'], true),
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
