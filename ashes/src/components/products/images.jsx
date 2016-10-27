/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element, PropTypes } from 'react';
import { connect } from 'react-redux';

// actions
import { actions } from 'modules/products/images';

// components
import Images from '../images/images';

// types
import type { Props as ImagesProps } from '../images/images';

type Params = {
  productId: number;
  context: string;
};

type Props = ImagesProps & {
  params: Params;
};

class ProductImages extends Component {
  props: Props;

  componentDidMount(): void {
    const { context, productId } = this.props.params;

    this.props.fetchAlbums(context, productId);
  }

  render(): Element {
    const { params: { productId, context }, ...rest } = this.props;

    return (
      <Images {...rest} entityId={productId} context={context} />
    );
  }

}

const mapState = (state) => ({
  albums: _.get(state, ['products', 'images', 'albums'], []),
  isLoading: _.get(state, ['asyncActions', 'productsFetchAlbums', 'inProgress'], true),
  addAlbumInProgress: _.get(state, ['asyncActions', 'productsAddAlbum', 'inProgress'], false),
  editAlbumInProgress: _.get(state, ['asyncActions', 'productsEditAlbum', 'inProgress'], false),
  uploadImagesInProgress: _.get(state, ['asyncActions', 'productsUploadImages', 'inProgress'], false),
});

export default connect(mapState, actions)(ProductImages);
