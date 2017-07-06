import React, { Component } from 'react';
import PropTypes from 'prop-types';

class ComponentsListItem extends Component {
  _content;
  mounted: bool;

  componentDidMount() {
    this.mounted = true;

    this.recalculateHeight();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  componentDidUpdate() {
    this.recalculateHeight();
  }

  recalculateHeight() {
    if (!this.props.collapsible) {
      this._content.style.opacity = 1;
      this._content.style.overflow = 'visible';

      return;
    }

    let maxHeight = 0;
    let opacity = 0;

    if (this.props.open) {
      maxHeight = this._content.scrollHeight;
      opacity = 1;
    }

    this._content.style.maxHeight = `${maxHeight}px`;
    this._content.style.opacity = opacity;
  }

  render() {
    return (
      <div ref={c => (this._content = c)} style={{ transition: 'all .4s', opacity: 0, overflow: 'hidden', }}>
        {React.cloneElement(this.props.content)}
      </div>
    );
  }
}

ComponentsListItem.propTypes = {
  collapsible: PropTypes.bool.isRequired,
  open: PropTypes.bool.isRequired,
  content: PropTypes.node.isRequired,
};

export default ComponentsListItem;
