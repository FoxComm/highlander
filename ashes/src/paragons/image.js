// @flow

// export const uploadImage = (image: ImageFile) => () => createAsyncActions(
//     'imageUpload',
//     () => { return Api.post(`/images/s3/`, image) }
// );

// mocked api response
const delay = (ms) =>
  new Promise(resolve => setTimeout(resolve, ms));

export const uploadImage = (image: ImageFile) =>
  delay(500).then(() => {
  return "http://s5.pikabu.ru/images/big_size_comm/2015-10_2/144430079217215265.jpg"
  });
