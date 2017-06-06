import React from 'react';
import ReactDOM from 'react-dom';

describe('RichTextEditor', function() {
  const RichTextEditor = requireComponent('rich-text-editor/rich-text-editor.jsx');

  it('should update self content if passed value have changed', function*() {
    const container = createContainer();
    ReactDOM.render(<RichTextEditor value="value1" />, container);
    expect(container.querySelector('.public-DraftEditor-content').textContent).to.equal('value1');

    // render with new value in same container
    ReactDOM.render(<RichTextEditor value="value2" />, container);
    expect(container.querySelector('.public-DraftEditor-content').textContent).to.equal('value2');

    container.unmount();
  });
});
