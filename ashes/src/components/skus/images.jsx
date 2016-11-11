/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element, PropTypes } from 'react';
import { connect } from 'react-redux';

import { actions } from 'modules/skus/images';

// components
import Images from '../images/images';

// types
import type { Props as ImagesProps } from '../images/images';
import type { Sku } from 'modules/skus/details';

type Params = {
  skuCode: number,
};

type Props = ImagesProps & {
  params: Params,
  object: Sku,
};

class SkuImages extends Component {
  props: Props;

  componentDidMount(): void {
    const { params: { skuId }, object: { context } } = this.props;

    this.props.fetchAlbums(context.name, skuId);
  }

  render(): Element {
    const { params: { skuId }, object: { context }, ...rest } = this.props;

    return (
      <Images {...rest} entityId={skuId} context={context.name} />
    );
  }

}

const mapState = (state) => ({
  albums: _.get(state, ['skus', 'images', 'albums'], []),
  isLoading: _.get(state, ['asyncActions', 'skusFetchAlbums', 'inProgress'], true),
  addAlbumInProgress: _.get(state, ['asyncActions', 'skusAddAlbum', 'inProgress'], false),
  editAlbumInProgress: _.get(state, ['asyncActions', 'skusEditAlbum', 'inProgress'], false),
  uploadImagesInProgress: _.get(state, ['asyncActions', 'skusUploadImages', 'inProgress'], false),
});

export default connect(mapState, actions)(SkuImages);
