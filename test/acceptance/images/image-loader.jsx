
import React from 'react';

describe('ImageLoader', function() {
  const ImageLoader = requireComponent('image/image.jsx');
  global.Image = global.window.Image;


  it.only('should animate on first load', function *() {
    const { container } = yield renderIntoDocument(
      <ImageLoader src="http://localhost/pic/name.svg" />
    );

    yield wait(200);

    expect(container.querySelector('.fc-wait-animation')).not.to.be.null;
    container.unmount();
  });
});
