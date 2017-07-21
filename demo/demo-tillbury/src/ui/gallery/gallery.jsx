// @flow

import React from 'react';
import ImageGallery from 'react-image-gallery';
import { renderLeftNav } from './left';
import { renderRightNav } from './right';

import s from './gallery.css';

type Props = {
  images: Array<string>,
  leftNav: Function,
  rightNav: Function,
};

const Gallery = (props: Props) => {
  // @TODO: add imgix integration
  const items = props.images.map((src) => {
    return {
      original: src,
      originalClass: s.image,
    };
  });

  const rightNav = props.rightNav ? props.rightNav : renderRightNav;
  const leftNav = props.leftNav ? props.leftNav : renderLeftNav;

  return (
    <ImageGallery
      {...props}
      items={items}
      slideInterval={2000}
      showThumbnails={false}
      showPlayButton={false}
      showBullets={false}
      showFullscreenButton={false}
      renderRightNav={rightNav}
      renderLeftNav={leftNav}
    />
  );
};

export default Gallery;
