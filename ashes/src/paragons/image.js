// @flow

import type { ImageFile } from '../modules/images';

// export const uploadImage = (image: ImageFile) => () => createAsyncActions(
//     'imageUpload',
//     () => { return Api.post(`/images/s3/`, image) }
// );

// mocked api response
const delay = (ms) =>
  new Promise(resolve => setTimeout(resolve, ms));

export const uploadImage = (image: ImageFile) =>
  delay(1500).then(() => {
    return {
      uploadedAt: new Date().toString(),
      id: 14,
      src: 'https://s-media-cache-ak0.pinimg.com/originals/a3/f2/37/a3f237a95a6fa4a314a9ccdd667b6056.jpg',
      title: 'title',
      alt: 'alt'
    }
  });

export const deleteImage = (image: ImageFile) =>
  delay(1500).then(() => {
    return {};
  });
