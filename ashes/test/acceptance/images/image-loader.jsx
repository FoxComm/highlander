
import React from 'react';
import ReactDOM from 'react-dom';

describe('ImageLoader', function() {
  const ImageLoader = requireComponent('image/image.jsx');

  // @todo fixme
  // it('should animate on first load', function *() {
  //   const { container } = yield renderIntoDocument(
  //     <ImageLoader src="http://localhost/resources/gnu.svg?timeout=200" />
  //   );
  //   yield wait(150);

  //   expect(container.querySelector('.fc-wait-animation')).not.to.be.null;
  //   container.unmount();
  // });

  it('should not animate when already loaded and receive new src (replace temp image with new)', function *() {
    const { container } = yield renderIntoDocument(
      <ImageLoader src="http://localhost/resources/gnu.svg" />
    );
    expect(container.querySelector('.fc-wait-animation')).to.be.null;

    ReactDOM.render(<ImageLoader src="http://localhost/resources/circle.svg" />, container);
    expect(container.querySelector('.fc-wait-animation')).to.be.null;

    container.unmount();
  });
});
