// @flow

import type { ImageFile } from '../modules/images'

// export const uploadImage = (image: ImageFile) => () => createAsyncActions(
//     'imageUpload',
//     () => { return Api.post(`/images/s3/`, image) }
// );

// mocked api response
const delay = (ms) =>
  new Promise(resolve => setTimeout(resolve, ms));

export const uploadImage = (image: ImageFile) =>
  delay(500).then(() => {
    return 'https://s-media-cache-ak0.pinimg.com/originals/a3/f2/37/a3f237a95a6fa4a314a9ccdd667b6056.jpg';
  });
