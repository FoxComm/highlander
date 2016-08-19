/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element, PropTypes } from 'react';
import { connect } from 'react-redux';

import { actions } from '../../modules/skus/images';

// components
import WaitAnimation from '../common/wait-animation';
import Images from '../images/images';

// types
import type { Props as ImagesProps } from '../images/images';

type Params = {
  skuCode: number;
};

type Props = ImagesProps & {
  params: Params;
};

class SkuImages extends Component {
  props: Props;

  componentDidMount(): void {
    const { skuCode } = this.props.params;
    const context = this.props.sku.context.name;

    this.props.fetchAlbums(context, skuCode);
  }

  render(): Element {
    const { params: { skuCode }, ...rest } = this.props;
    const context = this.props.sku.context.name;

    return (
      <Images {...rest} entityId={skuCode} context={context} />
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
