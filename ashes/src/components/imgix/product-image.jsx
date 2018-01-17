// @flow

import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import ProductImageInner from '@foxcommerce/wings/lib/ui/imgix/product-image';
import * as PluginsActions from 'modules/plugins';


type Props = {
  lazyFetchSettings: (name: string) => Promise<*>,
  settings: {
    cdn_prefix: string,
    s3_prefix: string,
    s3_bucket: string,
  },
  src: string,
  className: ?string,
  width: ?number,
  height: ?number,
};


class ProductImage extends Component {
  props: Props;

  componentDidMount() {
    this.props.lazyFetchSettings('imgix');
  }

  render() {
    const { src, width, height, className, settings: { cdn_prefix, s3_bucket, s3_prefix } } = this.props;

    return (
      <ProductImageInner
        imgixProductsSource={cdn_prefix}
        s3BucketName={s3_bucket}
        s3BucketPrefix={s3_prefix}
        src={src}
        width={width}
        height={height}
        className={className}
      />
    );
  }
}

export default connect(
  state => ({
    settings: _.get(state.plugins.detailed, 'imgix.settings', {}),
  }),
  PluginsActions
)(ProductImage);
