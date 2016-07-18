/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element, PropTypes } from 'react';
import { connect } from 'react-redux';

import { actions } from '../../modules/products/images';

// components
import WaitAnimation from '../common/wait-animation';
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
  static props: Props;

  componentDidMount(): void {
    const { context, productId } = this.props.params;

    this.props.fetchAlbums(context, productId);
  }

  render(): Element {
    const { params: { productId, context }, isLoading } = this.props;
    if (isLoading) {
      return <WaitAnimation />;
    }

    return (
      <Images {...this.props} entityId={productId} context={context} />
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
