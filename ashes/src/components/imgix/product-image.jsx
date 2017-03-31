// @flow

import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import ProductImageInner from '@foxcomm/wings/lib/ui/imgix/product-image';
import * as PluginsActions from 'modules/plugins';


type Props = {
  lazyFetchSettings: (name: string) => Promise<*>,
  settings: {
    cdn_prefix: string,
    s3_prefix: string,
    s3_bucket: string,
  },
  src: string,
};


class ProductImage extends Component {
  props: Props;

  componentDidMount() {
    this.props.lazyFetchSettings('imgix');
  }

  render() {
    console.log(this.props.settings);
    return (<ProductImageInner
      imgixProductsSource={this.props.settings.cdn_prefix}
      s3BucketName={this.props.settings.s3_bucket}
      s3BucketPrefix={this.props.settings.s3_prefix}
      src={this.props.src}
    />);
  }
}

export default connect(
  state => ({
    settings: _.get(state.plugins.detailed, 'imgix.settings', {}),
  }),
  PluginsActions
)(ProductImage);
