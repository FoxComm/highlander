/* @flow */

import React, { Component } from 'react';
import { env } from 'lib/env';
import ProductImage from '@foxcomm/wings/lib/ui/imgix/product-image';

type Props = {
  src: string,
  width: number,
  height: number,
};

const {
  IMGIX_PRODUCTS_SOURCE,
  S3_BUCKET_NAME,
  S3_BUCKET_PREFIX,
} = env;

class Image extends Component {
  props: Props;

  render() {
    return (
      <ProductImage
        src={this.props.src}
        width={this.props.width}
        height={this.props.height}
        imgixProductsSource={IMGIX_PRODUCTS_SOURCE}
        s3BucketName={S3_BUCKET_NAME}
        s3BucketPrefix={S3_BUCKET_PREFIX}
      />
    );
  }
}

export default Image;
