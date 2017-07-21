// @flow

import React from 'react';
import ImageGallery from 'react-image-gallery';
import { renderLeftNav } from './left';
import { renderRightNav } from './right';

import s from './gallery.css';

type Props = {
  images: Array<string>
}

const Gallery = (props: Props) => {
  // @TODO: add imgix integration
  const items = props.images.map((src) => {
    return {
      original: src,
      originalClass: s.image,
    };
  });

  return (
    <ImageGallery
      {...props}
      items={items}
      slideInterval={2000}
      showThumbnails={false}
      showPlayButton={false}
      showBullets={false}
      showFullscreenButton={false}
      renderRightNav={renderRightNav}
      renderLeftNav={renderLeftNav}
    />
  );
};

export default Gallery;
