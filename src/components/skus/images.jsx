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
  context: string;
};

type Props = ImagesProps & {
  params: Params;
};

class SkuImages extends Component {
  static props: Props;

  componentDidMount(): void {
    const { context, skuCode } = this.props.params;

    this.props.fetchAlbums(context, skuCode);
  }

  render(): Element {
    const { params: { skuCode, context }, isLoading } = this.props;
    if (isLoading) {
      return <WaitAnimation />;
    }

    return (
      <Images {...this.props} entityId={skuCode} context={context} />
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
