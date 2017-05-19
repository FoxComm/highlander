// @flow

// lib
import Api from 'lib/api';

// types
import type { ImageFile } from '../modules/images';

export const uploadImages = (files: Array<ImageFile>) => {

  const formData = new FormData();

  files.forEach((file: ImageFile) => {
    formData.append('upload-file', file.file);
  });

  return Api.post(`/images/default`, formData);
};
