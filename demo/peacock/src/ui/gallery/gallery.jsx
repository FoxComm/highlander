// @flow

import React from 'react';
import ImageGallery from 'react-image-gallery';

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
      thumbnail: src,
      thumbnailClass: s.thumbnail,
    };
  });

  const thumbs = props.images.map(src => {
    return <img src={src} className={s.thumbnail} />;
  });

  return (
    <div className={s.gallery}>
      {/* <div className={s.thumbs}>
        {thumbs}
      </div> */}
      <ImageGallery
        items={items}
        slideInterval={2000}
        showThumbnails={true}
        thumbnailPosition="left"
        showPlayButton={false}
        showBullets={false}
        showFullscreenButton={false}
      />
    </div>
  );
};

export default Gallery;
